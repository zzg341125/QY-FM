import { a2dp, baseProfile, constant } from '@kit.ConnectivityKit';
import { audio } from '@kit.AudioKit';
import { BusinessError } from '@kit.BasicServicesKit';

export interface BluetoothResumeCallbacks {
  onBluetoothLost: (deviceId: string) => void;
  onBluetoothRestored: (deviceId: string) => void;
  onWindowExpired: (deviceId: string) => void;
  onDiagnostics: (message: string) => void;
}

interface PendingResume {
  deviceId: string;
  disconnectedAt: number;
  deadline: number;
}

export class BluetoothResumeManager {
  private callbacks: BluetoothResumeCallbacks;
  private profile: a2dp.A2dpSourceProfile | undefined = undefined;
  private routingManager: audio.AudioRoutingManager | undefined = undefined;
  private pending: PendingResume | undefined = undefined;
  private lastActiveDeviceId: string = '';
  private userPlaybackActive: boolean = false;
  private lostHandled: boolean = false;
  private started: boolean = false;
  private expiryTimer: number = -1;

  private readonly profileListener = (event: baseProfile.StateChangeParam): void => {
    this.handleProfileState(event);
  };

  private readonly routeListener = (devices: audio.AudioDeviceDescriptors): void => {
    if (!this.userPlaybackActive || this.lastActiveDeviceId.length === 0 || this.lostHandled) {
      return;
    }
    if (devices.length > 0 && devices[0].deviceType === audio.DeviceType.SPEAKER) {
      if (this.isStillConnected(this.lastActiveDeviceId)) {
        return;
      }
      this.markLost(this.lastActiveDeviceId, '音频输出已切回扬声器');
    }
  };

  constructor(callbacks: BluetoothResumeCallbacks) {
    this.callbacks = callbacks;
  }

  start(): void {
    if (this.started) {
      return;
    }
    try {
      this.profile = a2dp.createA2dpSrcProfile();
      this.profile.on('connectionStateChange', this.profileListener);
      const rendererInfo: audio.AudioRendererInfo = {
        usage: audio.StreamUsage.STREAM_USAGE_MUSIC,
        rendererFlags: 0
      };
      this.routingManager = audio.getAudioManager().getRoutingManager();
      this.routingManager.on('preferOutputDeviceChangeForRendererInfo', rendererInfo, this.routeListener);
      this.started = true;
      this.callbacks.onDiagnostics('原生 A2DP 与音频路由监听已启动');
    } catch (error) {
      const businessError = error as BusinessError;
      this.callbacks.onDiagnostics(`蓝牙监听启动失败: ${businessError.code}`);
    }
  }

  stop(): void {
    try {
      this.profile?.off('connectionStateChange', this.profileListener);
      this.routingManager?.off('preferOutputDeviceChangeForRendererInfo', this.routeListener);
      this.started = false;
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`蓝牙监听停止失败: ${businessError.message}`);
    }
  }

  setPlaybackActive(active: boolean): void {
    this.userPlaybackActive = active;
    if (!active) {
      this.pending = undefined;
      this.lostHandled = false;
      return;
    }
    try {
      const devices = this.profile?.getConnectedDevices() ?? [];
      if (devices.length > 0) {
        this.lastActiveDeviceId = devices[0];
        this.callbacks.onDiagnostics(`播放已绑定蓝牙设备 ${devices[0]}`);
      }
    } catch (error) {
      const businessError = error as BusinessError;
      this.callbacks.onDiagnostics(`读取蓝牙设备失败: ${businessError.code}`);
    }
  }

  cancelPending(): void {
    this.pending = undefined;
    this.lostHandled = false;
    this.clearExpiryTimer();
  }

  expirationMinutes(): number {
    if (this.pending === undefined) {
      return 0;
    }
    return Math.max(0, Math.ceil((this.pending.deadline - Date.now()) / 60000));
  }

  private handleProfileState(event: baseProfile.StateChangeParam): void {
    this.callbacks.onDiagnostics(`A2DP ${event.deviceId} 状态=${event.state}`);
    if (event.state === constant.ProfileConnectionState.STATE_DISCONNECTED) {
      if (this.userPlaybackActive && !this.lostHandled && event.deviceId === this.lastActiveDeviceId) {
        this.markLost(event.deviceId, 'A2DP 已断开');
      }
      return;
    }
    if (event.state !== constant.ProfileConnectionState.STATE_CONNECTED) {
      return;
    }
    if (this.pending === undefined && this.userPlaybackActive) {
      this.lastActiveDeviceId = event.deviceId;
    }
    if (this.pending === undefined || event.deviceId !== this.pending.deviceId) {
      return;
    }
    if (Date.now() > this.pending.deadline) {
      this.callbacks.onDiagnostics('同一蓝牙设备已超过 120 分钟续播窗口');
      this.pending = undefined;
      this.lostHandled = false;
      this.clearExpiryTimer();
      return;
    }
    this.lastActiveDeviceId = event.deviceId;
    this.pending = undefined;
    this.lostHandled = false;
    this.clearExpiryTimer();
    this.callbacks.onDiagnostics(`同一蓝牙设备 ${event.deviceId} 已重连，准备续播`);
    this.callbacks.onBluetoothRestored(event.deviceId);
  }

  private markLost(deviceId: string, reason: string): void {
    this.lostHandled = true;
    const now = Date.now();
    this.pending = {
      deviceId: deviceId,
      disconnectedAt: now,
      deadline: now + 120 * 60 * 1000
    };
    this.clearExpiryTimer();
    this.expiryTimer = setTimeout(() => {
      const pending = this.pending;
      this.pending = undefined;
      this.lostHandled = false;
      if (pending !== undefined) {
        this.callbacks.onWindowExpired(pending.deviceId);
      }
    }, 120 * 60 * 1000);
    this.callbacks.onDiagnostics(`${reason}，已记录 120 分钟续播窗口`);
    this.callbacks.onBluetoothLost(deviceId);
  }

  private isStillConnected(deviceId: string): boolean {
    try {
      return (this.profile?.getConnectedDevices() ?? []).includes(deviceId);
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`检查蓝牙连接状态失败: ${businessError.message}`);
      return false;
    }
  }

  private clearExpiryTimer(): void {
    if (this.expiryTimer >= 0) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = -1;
    }
  }
}
