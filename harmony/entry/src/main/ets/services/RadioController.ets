import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { media } from '@kit.MediaKit';
import { avSession } from '@kit.AVSessionKit';
import { audio } from '@kit.AudioKit';
import { AppSettings, createDefaultSettings, Station } from '../models/Station';
import { BackgroundTaskService } from './BackgroundTaskService';
import { BluetoothResumeCallbacks, BluetoothResumeManager } from './BluetoothResumeManager';
import { SilentAudioKeepAlive } from './SilentAudioKeepAlive';

export interface RadioSnapshot {
  currentStation: Station | undefined;
  state: string;
  isPlaying: boolean;
  waitingForBluetooth: boolean;
  statusText: string;
  diagnostics: string;
  sleepTimerMinutes: number;
}

export type RadioStateListener = (snapshot: RadioSnapshot) => void;

const SESSION_TAG: string = 'QYFM_NATIVE_SESSION';

export class RadioController {
  private static instance: RadioController | undefined = undefined;

  static getInstance(): RadioController {
    if (RadioController.instance === undefined) {
      RadioController.instance = new RadioController();
    }
    return RadioController.instance;
  }

  private context: common.UIAbilityContext | undefined = undefined;
  private player: media.AVPlayer | undefined = undefined;
  private session: avSession.AVSession | undefined = undefined;
  private currentStation: Station | undefined = undefined;
  private settings: AppSettings = createDefaultSettings();
  private listeners: RadioStateListener[] = [];
  private waitingForBluetooth: boolean = false;
  private statusText: string = '选择电台后开始播放';
  private diagnostics: string = '蓝牙监听尚未启动';
  private isPlaying: boolean = false;
  private sleepTimerMinutes: number = 0;
  private sleepTimer: number = -1;
  private backgroundTasks: BackgroundTaskService = new BackgroundTaskService();
  private silentKeepAlive: SilentAudioKeepAlive = new SilentAudioKeepAlive();
  private bluetooth: BluetoothResumeManager;

  private constructor() {
    const callbacks: BluetoothResumeCallbacks = {
      onBluetoothLost: (deviceId: string): void => {
        this.handleBluetoothLost(deviceId);
      },
      onBluetoothRestored: (deviceId: string): void => {
        this.handleBluetoothRestored(deviceId);
      },
      onWindowExpired: (deviceId: string): void => {
        this.handleResumeWindowExpired(deviceId);
      },
      onDiagnostics: (message: string): void => {
        this.diagnostics = message;
        this.notify();
      }
    };
    this.bluetooth = new BluetoothResumeManager(callbacks);
  }

  async init(context: common.UIAbilityContext, settings: AppSettings): Promise<void> {
    this.context = context;
    this.settings = settings;
    if (settings.autoResume && this.context !== undefined) {
      this.bluetooth.start();
    }
    this.notify();
  }

  subscribe(listener: RadioStateListener): void {
    if (!this.listeners.includes(listener)) {
      this.listeners.push(listener);
    }
    listener(this.snapshot());
  }

  unsubscribe(listener: RadioStateListener): void {
    const index = this.listeners.indexOf(listener);
    if (index >= 0) {
      this.listeners.splice(index, 1);
    }
  }

  async applySettings(settings: AppSettings): Promise<void> {
    this.settings = settings;
    if (settings.autoResume) {
      this.bluetooth.start();
    } else {
      this.bluetooth.stop();
      this.bluetooth.cancelPending();
      await this.silentKeepAlive.stop();
      if (this.waitingForBluetooth && this.context !== undefined) {
        await this.backgroundTasks.stop(this.context);
      }
      this.waitingForBluetooth = false;
    }
    this.notify();
  }

  snapshot(): RadioSnapshot {
    return {
      currentStation: this.currentStation,
      state: this.isPlaying ? 'playing' : (this.waitingForBluetooth ? 'waiting' : 'stopped'),
      isPlaying: this.isPlaying,
      waitingForBluetooth: this.waitingForBluetooth,
      statusText: this.statusText,
      diagnostics: this.diagnostics,
      sleepTimerMinutes: this.sleepTimerMinutes
    };
  }

  async playStation(station: Station): Promise<void> {
    const context = this.context;
    if (context === undefined) {
      this.statusText = '应用尚未完成初始化';
      this.notify();
      return;
    }
    if (this.sleepTimer >= 0) {
      clearTimeout(this.sleepTimer);
      this.sleepTimer = -1;
      this.sleepTimerMinutes = 0;
    }
    this.waitingForBluetooth = false;
    this.bluetooth.cancelPending();
    await this.silentKeepAlive.stop();
    this.currentStation = station;
    this.statusText = `正在连接 ${station.name}`;
    this.isPlaying = false;
    this.notify();
    try {
      await this.ensureSession();
      await this.updateSessionMetadata(station);
      await this.replacePlayer(station.streamUrl);
      await this.backgroundTasks.start(context);
      if (this.settings.autoResume) {
        this.bluetooth.setPlaybackActive(true);
      }
    } catch (error) {
      const businessError = error as BusinessError;
      this.isPlaying = false;
      this.statusText = `播放失败：${businessError.message ?? '未知错误'}`;
      this.notify();
    }
  }

  async togglePlayback(): Promise<void> {
    if (this.currentStation === undefined) {
      this.statusText = '请先选择一个电台';
      this.notify();
      return;
    }
    if (this.isPlaying) {
      await this.pause('已暂停', true);
    } else {
      await this.resume('继续播放');
    }
  }

  async resume(reason: string): Promise<void> {
    if (this.currentStation === undefined) {
      return;
    }
    const player = this.player;
    if (player === undefined || (player.state !== 'paused' && player.state !== 'prepared')) {
      await this.playStation(this.currentStation);
      return;
    }
    try {
      this.waitingForBluetooth = false;
      await this.silentKeepAlive.stop();
      await player.play();
      this.statusText = reason;
      this.notify();
    } catch (error) {
      const businessError = error as BusinessError;
      this.statusText = `继续播放失败：${businessError.message ?? '未知错误'}`;
      this.notify();
    }
  }

  async pause(reason: string, manual: boolean): Promise<void> {
    if (manual) {
      this.bluetooth.cancelPending();
      this.bluetooth.setPlaybackActive(false);
      await this.silentKeepAlive.stop();
      if (this.context !== undefined) {
        await this.backgroundTasks.stop(this.context);
      }
    }
    try {
      if (this.player !== undefined && this.player.state === 'playing') {
        await this.player.pause();
      }
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`暂停失败: ${businessError.message}`);
    }
    this.isPlaying = false;
    this.waitingForBluetooth = !manual && this.settings.autoResume;
    this.statusText = reason;
    this.notify();
  }

  async stop(): Promise<void> {
    this.bluetooth.cancelPending();
    this.bluetooth.setPlaybackActive(false);
    await this.silentKeepAlive.stop();
    if (this.sleepTimer >= 0) {
      clearTimeout(this.sleepTimer);
      this.sleepTimer = -1;
      this.sleepTimerMinutes = 0;
    }
    if (this.player !== undefined) {
      await this.player.release();
      this.player = undefined;
    }
    if (this.context !== undefined) {
      await this.backgroundTasks.stop(this.context);
    }
    this.isPlaying = false;
    this.waitingForBluetooth = false;
    this.statusText = '已停止';
    this.notify();
  }

  setSleepTimer(minutes: number): void {
    if (this.sleepTimer >= 0) {
      clearTimeout(this.sleepTimer);
      this.sleepTimer = -1;
    }
    this.sleepTimerMinutes = minutes;
    if (minutes > 0) {
      this.sleepTimer = setTimeout(() => {
        this.sleepTimer = -1;
        this.sleepTimerMinutes = 0;
        this.pause('睡眠定时已结束', true);
      }, minutes * 60 * 1000);
      this.statusText = `${minutes} 分钟后停止播放`;
    } else {
      this.statusText = '睡眠定时已关闭';
    }
    this.notify();
  }

  private async replacePlayer(url: string): Promise<void> {
    const oldPlayer = this.player;
    this.player = undefined;
    if (oldPlayer !== undefined) {
      try {
        await oldPlayer.release();
      } catch (error) {
        const businessError = error as BusinessError;
        console.error(`旧播放器释放失败: ${businessError.message}`);
      }
    }

    const player = await media.createAVPlayer();
    this.player = player;
    player.on('stateChange', (state: string) => {
      if (this.player !== player) {
        return;
      }
      this.handlePlayerState(player, state).catch((error: BusinessError) => {
        this.statusText = `播放状态处理失败：${error.message}`;
        this.notify();
      });
    });
    player.on('error', (error: BusinessError) => {
      if (this.player !== player) {
        return;
      }
      this.isPlaying = false;
      this.statusText = `播放器错误：${error.message}`;
      this.notify();
    });
    player.url = url;
  }

  private async handlePlayerState(player: media.AVPlayer, state: string): Promise<void> {
    if (state === 'initialized') {
      player.audioRendererInfo = {
        usage: audio.StreamUsage.STREAM_USAGE_MUSIC,
        rendererFlags: 0
      };
      await player.prepare();
    } else if (state === 'prepared') {
      await player.play();
    } else if (state === 'playing') {
      this.isPlaying = true;
      this.waitingForBluetooth = false;
      this.statusText = this.currentStation?.name ?? '正在播放';
      await this.updateSessionPlaybackState(true);
      this.notify();
    } else if (state === 'paused') {
      this.isPlaying = false;
      if (!this.waitingForBluetooth) {
        this.statusText = '已暂停';
      }
      await this.updateSessionPlaybackState(false);
      this.notify();
    } else if (state === 'completed' || state === 'stopped') {
      this.isPlaying = false;
      this.statusText = '播放已停止';
      await this.updateSessionPlaybackState(false);
      this.notify();
    }
  }

  private async ensureSession(): Promise<void> {
    if (this.session !== undefined || this.context === undefined) {
      return;
    }
    const session = await avSession.createAVSession(this.context, SESSION_TAG, 'audio');
    this.session = session;
    session.setBackgroundPlayMode(avSession.BackgroundPlayMode.ENABLE_BACKGROUND_PLAY);
    session.on('play', () => {
      this.resume('从媒体控制继续播放');
    });
    session.on('pause', () => {
      this.pause('从媒体控制暂停', true);
    });
    session.on('stop', () => {
      this.stop();
    });
    await session.activate();
  }

  private async updateSessionMetadata(station: Station): Promise<void> {
    if (this.session === undefined) {
      return;
    }
    await this.session.setAVMetadata({
      assetId: station.id,
      title: station.name,
      artist: station.tags,
      mediaImage: station.iconUrl
    });
  }

  private async updateSessionPlaybackState(isPlaying: boolean): Promise<void> {
    if (this.session === undefined) {
      return;
    }
    await this.session.setAVPlaybackState({
      state: isPlaying
        ? avSession.PlaybackState.PLAYBACK_STATE_PLAY
        : avSession.PlaybackState.PLAYBACK_STATE_PAUSE
    });
  }

  private async handleBluetoothLost(deviceId: string): Promise<void> {
    if (!this.settings.autoResume || this.currentStation === undefined || !this.isPlaying) {
      return;
    }
    await this.pause('蓝牙已断开，等待同一设备重连', false);
    await this.silentKeepAlive.start();
    this.waitingForBluetooth = true;
    this.statusText = '蓝牙已断开，120 分钟内重连将自动续播';
    this.notify();
  }

  private async handleBluetoothRestored(deviceId: string): Promise<void> {
    if (!this.waitingForBluetooth || this.currentStation === undefined) {
      return;
    }
    await this.resume(`蓝牙设备 ${deviceId} 已重连，自动续播`);
  }

  private async handleResumeWindowExpired(deviceId: string): Promise<void> {
    if (!this.waitingForBluetooth) {
      return;
    }
    this.waitingForBluetooth = false;
    await this.silentKeepAlive.stop();
    if (this.context !== undefined) {
      await this.backgroundTasks.stop(this.context);
    }
    this.statusText = `蓝牙设备 ${deviceId} 超过 120 分钟未重连`;
    this.notify();
  }

  private notify(): void {
    const snapshot = this.snapshot();
    this.listeners.forEach((listener: RadioStateListener) => {
      listener(snapshot);
    });
  }
}
