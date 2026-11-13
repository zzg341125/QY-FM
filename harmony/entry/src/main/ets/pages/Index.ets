import { abilityAccessCtrl, common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import {
  AppSettings,
  copyStation,
  copySettings,
  createDefaultSettings,
  Station
} from '../models/Station';
import { StationRepository } from '../data/StationRepository';
import { RadioController, RadioSnapshot, RadioStateListener } from '../services/RadioController';
import { WebDavBackupService, WebDavConfig } from '../services/WebDavBackupService';
import { AppUpdateService, NativeUpdateInfo } from '../services/AppUpdateService';
import { StationIconPicker } from '../services/StationIconPicker';
import { LocalFileService } from '../services/LocalFileService';
import { M3uPlaylistService, M3uEntry } from '../services/M3uPlaylistService';
import { StationSearchService } from '../services/StationSearchService';
import { findTheme, THEME_PRESETS, ThemePreset } from '../models/ThemePreset';

const COLOR_DANGER_BG: string = '#E9B7B3';
const COLOR_DANGER: string = '#B3261E';

@Entry
@Component
struct Index {
  @State stations: Station[] = [];
  @State currentStation: Station | undefined = undefined;
  @State isPlaying: boolean = false;
  @State waitingForBluetooth: boolean = false;
  @State playbackStatus: string = '选择电台后开始播放';
  @State diagnostics: string = '蓝牙监听尚未启动';
  @State sleepTimerMinutes: number = 0;
  @State showSleepPanel: boolean = false;
  @State showSettings: boolean = false;
  @State showEditor: boolean = false;
  @State showWebDav: boolean = false;
  @State showTheme: boolean = false;
  @State pendingDelete: Station | undefined = undefined;
  @State swipedStationId: string = '';
  @State swipingStationId: string = '';
  @State stationSwipeDistance: number = 0;
  @State searchQuery: string = '';
  @State searchResults: Station[] = [];
  @State searching: boolean = false;
  @State palette: ThemePreset = findTheme('lunar_light');
  @State editorIsNew: boolean = true;
  @State editorId: string = '';
  @State editorName: string = '';
  @State editorUrl: string = '';
  @State editorIcon: string = '';
  @State editorTags: string = '';
  @State settings: AppSettings = createDefaultSettings();
  @State settingsStatus: string = '';
  @State updateInfo: NativeUpdateInfo | undefined = undefined;
  @State busy: boolean = false;

  private repository: StationRepository = new StationRepository();
  private radio: RadioController = RadioController.getInstance();
  private webDav: WebDavBackupService = new WebDavBackupService();
  private updateService: AppUpdateService = new AppUpdateService();
  private iconPicker: StationIconPicker = new StationIconPicker();
  private localFiles: LocalFileService = new LocalFileService();
  private m3u: M3uPlaylistService = new M3uPlaylistService();
  private searchService: StationSearchService = new StationSearchService();
  private abilityContext: common.UIAbilityContext | undefined = undefined;

  private readonly snapshotListener: RadioStateListener = (snapshot: RadioSnapshot): void => {
    if (snapshot.currentStation !== undefined) {
      this.currentStation = snapshot.currentStation;
    }
    this.isPlaying = snapshot.isPlaying;
    this.waitingForBluetooth = snapshot.waitingForBluetooth;
    this.playbackStatus = snapshot.statusText;
    this.diagnostics = snapshot.diagnostics;
    this.sleepTimerMinutes = snapshot.sleepTimerMinutes;
  };

  aboutToAppear(): void {
    this.bootstrap();
  }

  aboutToDisappear(): void {
    this.radio.unsubscribe(this.snapshotListener);
  }

  private async bootstrap(): Promise<void> {
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    this.abilityContext = context;
    try {
      await this.repository.init(context);
      this.stations = await this.repository.loadStations();
      this.settings = await this.repository.loadSettings();
      await this.radio.init(context, this.settings);
      this.radio.subscribe(this.snapshotListener);
      await this.requestBluetoothPermission(context);
      this.palette = findTheme(this.settings.themeId);
      const remembered = this.stations.find((station: Station) => station.id === this.settings.lastStationId);
      if (remembered !== undefined) {
        this.currentStation = remembered;
      } else if (this.stations.length > 0) {
        this.currentStation = this.stations[0];
      }
      if (this.settings.autoPlayOnLaunch && this.currentStation !== undefined) {
        await this.playStation(this.currentStation);
      }
      if (this.settings.autoUpdate) {
        await this.checkForUpdates(false);
      }
    } catch (error) {
      const businessError = error as BusinessError;
      this.playbackStatus = `初始化失败：${businessError.message ?? '未知错误'}`;
    }
  }

  private async requestBluetoothPermission(context: common.UIAbilityContext): Promise<void> {
    try {
      const manager = abilityAccessCtrl.createAtManager();
      await manager.requestPermissionsFromUser(context, ['ohos.permission.ACCESS_BLUETOOTH']);
    } catch (error) {
      const businessError = error as BusinessError;
      this.diagnostics = `蓝牙权限申请返回：${businessError.code}`;
    }
  }

  private async playStation(station: Station): Promise<void> {
    this.currentStation = station;
    if (this.settings.playbackMemory) {
      const settings = copySettings(this.settings);
      settings.lastStationId = station.id;
      this.settings = settings;
      await this.repository.saveSettings(settings);
    }
    await this.radio.playStation(station);
  }

  private activateStation(station: Station): void {
    if (this.swipedStationId.length > 0) {
      this.swipedStationId = '';
      return;
    }
    if (this.settings.clickStationToPlay) {
      if (this.currentStation?.id === station.id) {
        this.togglePlayback();
      } else {
        this.playStation(station);
      }
    }
  }

  private async togglePlayback(): Promise<void> {
    if (this.currentStation === undefined) {
      this.playbackStatus = '请先选择一个电台';
      return;
    }
    if (!this.isPlaying) {
      await this.playStation(this.currentStation);
      return;
    }
    await this.radio.pause('已暂停', true);
  }

  private openNewStation(): void {
    this.editorIsNew = true;
    this.editorId = `station-${Date.now()}`;
    this.editorName = '';
    this.editorUrl = '';
    this.editorIcon = '';
    this.editorTags = '';
    this.showEditor = true;
  }

  private openStationEditor(station: Station): void {
    this.editorIsNew = false;
    this.editorId = station.id;
    this.editorName = station.name;
    this.editorUrl = station.streamUrl;
    this.editorIcon = station.iconUrl;
    this.editorTags = station.tags;
    this.showEditor = true;
  }

  private async saveStation(): Promise<void> {
    const name = this.editorName.trim();
    const url = this.editorUrl.trim();
    if (name.length === 0 || (!url.startsWith('http://') && !url.startsWith('https://'))) {
      this.playbackStatus = '电台名称不能为空，播放地址必须以 http:// 或 https:// 开头';
      return;
    }
    const existing = this.stations.find((station: Station) => station.id === this.editorId);
    const nextStation: Station = {
      id: this.editorId,
      name: name,
      streamUrl: url,
      iconUrl: this.editorIcon,
      tags: this.editorTags.trim(),
      country: existing?.country ?? '自定义',
      state: existing?.state ?? '',
      favorite: true
    };
    const nextStations = this.stations.map((station: Station) => {
      if (station.id === nextStation.id) {
        return nextStation;
      }
      return station;
    });
    if (!nextStations.some((station: Station) => station.id === nextStation.id)) {
      nextStations.push(nextStation);
    }
    this.stations = nextStations;
    this.currentStation = nextStation;
    await this.repository.saveStations(this.stations);
    this.showEditor = false;
    this.playbackStatus = '电台已保存';
  }

  private async deleteStation(id: string): Promise<void> {
    if (!this.settings.editMode) {
      return;
    }
    this.stations = this.stations.filter((station: Station) => station.id !== id);
    if (this.currentStation?.id === id) {
      await this.radio.stop();
      this.currentStation = this.stations.length > 0 ? this.stations[0] : undefined;
    }
    await this.repository.saveStations(this.stations);
    this.playbackStatus = '电台已删除';
  }

  private moveStationByIndex(from: number, to: number): void {
    if (!this.settings.editMode || from === to || from < 0 || to < 0 || from >= this.stations.length ||
      to >= this.stations.length) {
      return;
    }
    const next = this.stations.slice();
    const removed = next.splice(from, 1);
    next.splice(to, 0, removed[0]);
    this.stations = next;
    this.repository.saveStations(this.stations).then(() => {
      this.playbackStatus = '电台顺序已保存';
    });
  }

  private async pickStationIcon(): Promise<void> {
    try {
      const icon = await this.iconPicker.pick();
      if (icon.length > 0) {
        this.editorIcon = icon;
      }
    } catch (error) {
      const businessError = error as BusinessError;
      this.playbackStatus = businessError.message;
    }
  }

  private async persistSettings(): Promise<void> {
    const settings = copySettings(this.settings);
    this.settings = settings;
    await this.repository.saveSettings(settings);
    await this.radio.applySettings(settings);
    this.settingsStatus = '设置已保存';
  }

  private updateWebDavUrl(value: string): void {
    const settings = copySettings(this.settings);
    settings.webdavUrl = value;
    this.settings = settings;
  }

  private updateWebDavUser(value: string): void {
    const settings = copySettings(this.settings);
    settings.webdavUser = value;
    this.settings = settings;
  }

  private updateWebDavPassword(value: string): void {
    const settings = copySettings(this.settings);
    settings.webdavPassword = value;
    this.settings = settings;
  }

  private updateWebDavFile(value: string): void {
    const settings = copySettings(this.settings);
    settings.webdavFile = value;
    this.settings = settings;
  }

  private webDavConfig(): WebDavConfig {
    return {
      baseUrl: this.settings.webdavUrl,
      user: this.settings.webdavUser,
      password: this.settings.webdavPassword,
      fileName: this.settings.webdavFile
    };
  }

  private async testWebDav(): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.settingsStatus = '正在连接 WebDAV…';
    try {
      await this.webDav.testConnection(this.webDavConfig());
      this.settingsStatus = 'WebDAV 连接成功';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    } finally {
      this.busy = false;
    }
  }

  private async backupToWebDav(): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.settingsStatus = '正在上传电台、台标和顺序…';
    try {
      await this.repository.saveSettings(this.settings);
      const json = this.repository.exportBackup(this.stations, this.settings);
      await this.webDav.uploadBackup(this.webDavConfig(), json);
      this.settingsStatus = '备份完成：已包含电台、台标与顺序';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    } finally {
      this.busy = false;
    }
  }

  private async restoreFromWebDav(): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.settingsStatus = '正在从 WebDAV 恢复…';
    try {
      const json = await this.webDav.downloadBackup(this.webDavConfig());
      const result = this.repository.parseBackup(json);
      this.stations = result.stations;
      this.currentStation = this.stations.length > 0 ? this.stations[0] : undefined;
      result.settings.webdavPassword = this.settings.webdavPassword;
      this.settings = result.settings;
      await this.repository.saveStations(this.stations);
      await this.repository.saveSettings(this.settings);
      await this.radio.applySettings(this.settings);
      await this.radio.stop();
      this.settingsStatus = '恢复完成：电台、台标与顺序均已恢复';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    } finally {
      this.busy = false;
    }
  }

  private async checkForUpdates(manual: boolean): Promise<void> {
    if (manual) {
      this.settingsStatus = '正在检查新版本…';
    }
    const release = await this.updateService.checkNativeRelease();
    if (release !== undefined) {
      this.updateInfo = release;
      if (manual) {
        this.settingsStatus = `发现原生鸿蒙新版本 ${release.version}`;
      }
      return;
    }
    if (this.abilityContext !== undefined) {
      const systemUpdate = await this.updateService.checkAppGallery(this.abilityContext);
      if (systemUpdate) {
        this.settingsStatus = '发现应用市场新版本';
        return;
      }
    }
    if (manual) {
      this.settingsStatus = '当前已是最新版本';
    }
  }

  private async openReleasePage(): Promise<void> {
    if (this.abilityContext === undefined || this.updateInfo === undefined) {
      return;
    }
    await this.updateService.openReleasePage(this.abilityContext, this.updateInfo.pageUrl);
    this.updateInfo = undefined;
  }

  private updateEditMode(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.editMode = value;
    this.settings = settings;
    if (!value) {
      this.closeStationSwipe();
    }
    this.persistSettings();
  }

  private beginStationSwipe(stationId: string): void {
    if (!this.settings.editMode) {
      return;
    }
    if (this.swipedStationId.length > 0 && this.swipedStationId !== stationId) {
      this.swipedStationId = '';
    }
    this.swipingStationId = stationId;
    this.stationSwipeDistance = 0;
  }

  private updateStationSwipe(stationId: string, offset: number): void {
    if (!this.settings.editMode) {
      return;
    }
    this.swipingStationId = stationId;
    this.stationSwipeDistance = Math.max(-88, Math.min(0, offset));
    if (this.stationSwipeDistance <= -32) {
      this.swipedStationId = stationId;
    }
  }

  private finishStationSwipe(stationId: string, offset: number): void {
    if (!this.settings.editMode) {
      this.closeStationSwipe();
      return;
    }
    const finalOffset = Math.min(offset, this.stationSwipeDistance);
    this.swipedStationId = finalOffset <= -32 ? stationId : '';
    this.swipingStationId = '';
    this.stationSwipeDistance = 0;
  }

  private cancelStationSwipe(): void {
    this.swipingStationId = '';
    this.stationSwipeDistance = 0;
  }

  private closeStationSwipe(): void {
    this.swipedStationId = '';
    this.cancelStationSwipe();
  }

  private stationSwipeOffset(stationId: string): number {
    if (!this.settings.editMode) {
      return 0;
    }
    if (this.swipedStationId === stationId) {
      return -88;
    }
    if (this.swipingStationId === stationId) {
      return this.stationSwipeDistance;
    }
    return 0;
  }

  private updateAutoUpdate(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.autoUpdate = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateAutoResume(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.autoResume = value;
    settings.resumeWindowMinutes = 120;
    this.settings = settings;
    this.persistSettings();
  }

  private updateDynamicColor(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.dynamicColor = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateTransparentUi(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.transparentUi = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateClickStation(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.clickStationToPlay = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateAutoPlayOnLaunch(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.autoPlayOnLaunch = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updatePlaybackMemory(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.playbackMemory = value;
    if (!value) {
      settings.lastStationId = '';
    }
    this.settings = settings;
    this.persistSettings();
  }

  private updateFastStart(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.fastStart = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateLargeBuffer(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.largeBuffer = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updateEditStreamUrl(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.editStreamUrl = value;
    this.settings = settings;
    this.persistSettings();
  }

  private updatePauseOnUnplug(value: boolean): void {
    const settings = copySettings(this.settings);
    settings.pauseOnUnplug = value;
    this.settings = settings;
    this.persistSettings();
  }

  private async selectTheme(theme: ThemePreset): Promise<void> {
    const settings = copySettings(this.settings);
    settings.themeId = theme.id;
    this.settings = settings;
    this.palette = theme;
    this.showTheme = false;
    await this.persistSettings();
  }

  private async searchStations(): Promise<void> {
    if (this.searching || this.searchQuery.trim().length === 0) {
      return;
    }
    this.searching = true;
    this.settingsStatus = '正在搜索电台…';
    try {
      this.searchResults = await this.searchService.search(this.searchQuery);
      this.settingsStatus = this.searchResults.length > 0 ? `找到 ${this.searchResults.length} 个电台` : '没有找到电台';
    } finally {
      this.searching = false;
    }
  }

  private async addSearchStation(station: Station): Promise<void> {
    if (this.stations.some((item: Station) => item.id === station.id || item.streamUrl === station.streamUrl)) {
      this.settingsStatus = '该电台已在播放列表中';
      return;
    }
    this.stations = [station].concat(this.stations);
    await this.repository.saveStations(this.stations);
    this.settingsStatus = `已添加：${station.name}`;
  }

  private async importM3uFile(): Promise<void> {
    if (this.abilityContext === undefined) {
      return;
    }
    try {
      const text = await this.localFiles.pickText(this.abilityContext, 'M3U 播放列表|.m3u,.m3u8,.txt');
      if (text.length === 0) {
        return;
      }
      const entries = this.m3u.parse(text);
      let added = 0;
      entries.forEach((entry: M3uEntry) => {
        if (this.stations.some((station: Station) => station.streamUrl === entry.url)) {
          return;
        }
        const importedStation: Station = {
          id: `m3u-${Date.now()}-${added}`,
          name: entry.name,
          streamUrl: entry.url,
          iconUrl: entry.iconUrl,
          tags: 'M3U',
          country: 'M3U导入',
          state: 'M3U',
          favorite: false
        };
        this.stations = [importedStation].concat(this.stations);
        added++;
      });
      await this.repository.saveStations(this.stations);
      this.settingsStatus = added > 0 ? `已导入 ${added} 个电台` : '没有新的可导入电台';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    }
  }

  private async exportM3uFile(): Promise<void> {
    if (this.abilityContext === undefined) {
      return;
    }
    try {
      await this.localFiles.saveText(
        this.abilityContext,
        'QYFM_电台列表.m3u',
        this.m3u.build(this.stations)
      );
      this.settingsStatus = 'M3U 已导出';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    }
  }

  private async backupLocalFile(): Promise<void> {
    if (this.abilityContext === undefined) {
      return;
    }
    try {
      await this.localFiles.saveText(
        this.abilityContext,
        'QYFM_电台备份.json',
        this.repository.exportBackup(this.stations, this.settings)
      );
      this.settingsStatus = '本地备份已保存';
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    }
  }

  private async restoreLocalFile(): Promise<void> {
    if (this.abilityContext === undefined) {
      return;
    }
    try {
      const text = await this.localFiles.pickText(this.abilityContext, 'QYFM 备份|.json,.txt');
      if (text.length === 0) {
        return;
      }
      const result = this.repository.parseBackup(text);
      this.stations = result.stations;
      this.currentStation = this.stations.length > 0 ? this.stations[0] : undefined;
      await this.repository.saveStations(this.stations);
      await this.radio.stop();
      this.settingsStatus = `已恢复 ${this.stations.length} 个电台`;
    } catch (error) {
      const businessError = error as BusinessError;
      this.settingsStatus = businessError.message;
    }
  }

  private async openGithubPage(): Promise<void> {
    if (this.abilityContext !== undefined) {
      await this.abilityContext.openLink('https://github.com/zzg341125/QY-FM');
    }
  }

  private async refreshStationIcons(): Promise<void> {
    this.stations = this.stations.map((station: Station) => copyStation(station));
    await this.repository.saveStations(this.stations);
    this.settingsStatus = '电台图标已刷新';
  }

  private primaryColor(): string {
    return this.settings.dynamicColor ? this.palette.primary : '#56698C';
  }

  private settingValue(key: string): boolean {
    switch (key) {
      case 'dynamicColor':
        return this.settings.dynamicColor;
      case 'transparentUi':
        return this.settings.transparentUi;
      case 'clickStation':
        return this.settings.clickStationToPlay;
      case 'autoPlay':
        return this.settings.autoPlayOnLaunch;
      case 'playbackMemory':
        return this.settings.playbackMemory;
      case 'fastStart':
        return this.settings.fastStart;
      case 'largeBuffer':
        return this.settings.largeBuffer;
      case 'editMode':
        return this.settings.editMode;
      case 'editStreamUrl':
        return this.settings.editStreamUrl;
      case 'pauseOnUnplug':
        return this.settings.pauseOnUnplug;
      case 'autoResume':
        return this.settings.autoResume;
      case 'autoUpdate':
        return this.settings.autoUpdate;
      default:
        return false;
    }
  }

  private updateSettingSwitch(key: string, value: boolean): void {
    switch (key) {
      case 'dynamicColor':
        this.updateDynamicColor(value);
        break;
      case 'transparentUi':
        this.updateTransparentUi(value);
        break;
      case 'clickStation':
        this.updateClickStation(value);
        break;
      case 'autoPlay':
        this.updateAutoPlayOnLaunch(value);
        break;
      case 'playbackMemory':
        this.updatePlaybackMemory(value);
        break;
      case 'fastStart':
        this.updateFastStart(value);
        break;
      case 'largeBuffer':
        this.updateLargeBuffer(value);
        break;
      case 'editMode':
        this.updateEditMode(value);
        break;
      case 'editStreamUrl':
        this.updateEditStreamUrl(value);
        break;
      case 'pauseOnUnplug':
        this.updatePauseOnUnplug(value);
        break;
      case 'autoResume':
        this.updateAutoResume(value);
        break;
      case 'autoUpdate':
        this.updateAutoUpdate(value);
        break;
      default:
        break;
    }
  }

  private async runSettingAction(action: string): Promise<void> {
    switch (action) {
      case 'theme':
        this.showTheme = true;
        break;
      case 'refreshIcons':
        await this.refreshStationIcons();
        break;
      case 'exportM3u':
        await this.exportM3uFile();
        break;
      case 'importM3u':
        await this.importM3uFile();
        break;
      case 'backupLocal':
        await this.backupLocalFile();
        break;
      case 'restoreLocal':
        await this.restoreLocalFile();
        break;
      case 'webdav':
        this.showWebDav = true;
        break;
      case 'checkUpdate':
        await this.checkForUpdates(true);
        break;
      case 'github':
        await this.openGithubPage();
        break;
      case 'bluetoothPermission':
        if (this.abilityContext !== undefined) {
          await this.requestBluetoothPermission(this.abilityContext);
          this.settingsStatus = '蓝牙设备识别权限已请求';
        }
        break;
      default:
        break;
    }
  }

  @Builder
  StationDeleteAction(id: string) {
    Column() {
      if (this.settings.editMode) {
        Text('−')
          .width(44)
          .height(44)
          .textAlign(TextAlign.Center)
          .fontSize(30)
          .fontColor(COLOR_DANGER)
          .borderRadius(22)
          .border({ width: 1.2, color: COLOR_DANGER })
          .onClick(() => {
            const station = this.stations.find((item: Station) => item.id === id);
            if (station !== undefined) {
              this.pendingDelete = station;
            }
          })
      }
    }
    .width(this.settings.editMode ? 88 : 0)
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.End)
    .padding({ right: 24 })
    .backgroundColor(COLOR_DANGER_BG)
  }

  @Builder
  StationRow(station: Station, index: number) {
    Row({ space: 18 }) {
      if (station.iconUrl.length > 0) {
        Image(station.iconUrl)
          .width(56)
          .height(56)
          .borderRadius(28)
          .objectFit(ImageFit.Cover)
          .backgroundColor(this.palette.surfaceVariant)
      } else {
        Stack() {
          Text('FM')
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.palette.textSecondary)
        }
        .width(56)
        .height(56)
        .borderRadius(28)
        .backgroundColor(this.palette.surfaceVariant)
      }

      Text(station.name)
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
        .fontColor(this.palette.textPrimary)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .layoutWeight(1)

      Text(this.currentStation?.id === station.id && this.isPlaying ? '■' : '▶')
        .width(44)
        .height(44)
        .textAlign(TextAlign.Center)
        .fontSize(this.currentStation?.id === station.id ? 18 : 22)
        .fontColor(this.currentStation?.id === station.id ? this.primaryColor() : this.palette.textPrimary)
        .border({ width: 1.2, color: this.currentStation?.id === station.id ? this.primaryColor() : this.palette.textPrimary })
        .borderRadius(22)
        .hitTestBehavior(HitTestMode.Block)
        .onClick(() => {
          if (this.currentStation?.id === station.id) {
            this.togglePlayback();
          } else {
            this.playStation(station);
          }
        })
    }
    .width('100%')
    .height(100)
    .padding({ left: 16, right: 16 })
    .backgroundColor(this.palette.background)
  }

  @Builder
  StationListItemContent(station: Station, index: number) {
    Column() {
      this.StationRow(station, index)
      Divider()
        .strokeWidth(1)
        .color(this.palette.divider)
        .margin({ left: 16, right: 16 })
    }
  }

  @Builder
  SwipeableStationItem(station: Station, index: number) {
    Stack({ alignContent: Alignment.End }) {
      if (this.settings.editMode && this.swipedStationId === station.id) {
        this.StationDeleteAction(station.id)
      }
      Column() {
        this.StationListItemContent(station, index)
      }
      .width('100%')
      .translate({ x: this.stationSwipeOffset(station.id) })
    }
    .width('100%')
    .clip(true)
    .gesture(
      PanGesture({ direction: PanDirection.Horizontal, distance: 10 })
        .onActionStart(() => {
          this.beginStationSwipe(station.id);
        })
        .onActionUpdate((event: GestureEvent) => {
          this.updateStationSwipe(station.id, event.offsetX);
        })
        .onActionEnd((event: GestureEvent) => {
          this.finishStationSwipe(station.id, event.offsetX);
        })
        .onActionCancel(() => {
          this.cancelStationSwipe();
        })
    )
  }

  @Builder
  HomeActions() {
    Column() {
      Row({ space: 10 }) {
        Button('＋  添加新电台')
          .layoutWeight(3)
          .height(52)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.palette.textPrimary)
          .backgroundColor(this.palette.surfaceVariant)
          .border({ width: 1, color: this.palette.divider })
          .borderRadius(26)
          .onClick(() => {
            this.openNewStation();
          })
        Button('⚙  设置')
          .layoutWeight(2)
          .height(52)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.palette.textPrimary)
          .backgroundColor(this.palette.surfaceVariant)
          .border({ width: 1, color: this.palette.divider })
          .borderRadius(26)
          .onClick(() => {
            this.showSettings = true;
          })
      }
      .width('100%')
      .padding({ left: 14, right: 14, top: 14, bottom: 14 })
    }
    .width('100%')
  }

  @Builder
  SleepPanel() {
    Column({ space: 14 }) {
      Row() {
        Text('睡眠定时')
          .fontSize(19)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.primaryColor())
        Blank()
        Text('收起')
          .fontSize(13)
          .fontColor(this.palette.textSecondary)
          .onClick(() => {
            this.showSleepPanel = false;
          })
      }
      .width('100%')
      Row({ space: 8 }) {
        ForEach([0, 15, 30, 45, 60], (minutes: number) => {
          Button(minutes === 0 ? '关闭' : `${minutes} 分`)
            .layoutWeight(1)
            .height(44)
            .fontSize(14)
            .fontColor(this.sleepTimerMinutes === minutes ? Color.White : this.palette.textPrimary)
            .backgroundColor(this.sleepTimerMinutes === minutes ? this.primaryColor() : this.palette.surfaceVariant)
            .borderRadius(22)
            .onClick(() => {
              this.radio.setSleepTimer(minutes);
            })
        }, (minutes: number) => minutes.toString())
      }
      .width('100%')
    }
    .width('100%')
    .padding(14)
    .borderRadius(26)
    .backgroundColor(this.palette.nowPlaying)
    .margin({ left: 14, right: 14, bottom: 8 })
  }

  @Builder
  NowPlayingBar() {
    Row({ space: 14 }) {
      if (this.currentStation !== undefined && this.currentStation.iconUrl.length > 0) {
        Image(this.currentStation.iconUrl)
          .width(58)
          .height(58)
          .borderRadius(29)
          .objectFit(ImageFit.Cover)
          .backgroundColor(this.palette.surface)
      } else {
        Stack() {
          Text('FM')
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.primaryColor())
        }
        .width(58)
        .height(58)
        .borderRadius(29)
        .backgroundColor(this.palette.surface)
      }

      Column({ space: 2 }) {
        Text(this.currentStation?.name ?? '暂未选择电台')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.primaryColor())
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(this.waitingForBluetooth ? '等待同一蓝牙设备重连' : this.playbackStatus)
          .fontSize(13)
          .fontColor(this.primaryColor())
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)

      Text(this.isPlaying ? 'Ⅱ' : '▶')
        .width(56)
        .height(56)
        .textAlign(TextAlign.Center)
        .fontSize(24)
        .fontColor(Color.White)
        .backgroundColor(this.primaryColor())
        .borderRadius(28)
        .onClick(() => {
          this.togglePlayback();
        })
    }
    .width('100%')
    .height(84)
    .padding({ left: 12, right: 8 })
    .borderRadius(30)
    .backgroundColor(this.palette.nowPlaying)
    .opacity(this.settings.transparentUi ? 0.88 : 1)
    .margin({ left: 14, right: 14, bottom: 10 })
    .onClick(() => {
      this.togglePlayback();
    })
    .gesture(
      LongPressGesture()
        .onAction(() => {
          this.showSleepPanel = !this.showSleepPanel;
        })
    )
  }

  @Builder
  HomePage() {
    Column() {
      List() {
        ForEach(this.stations, (station: Station, index: number) => {
          ListItem() {
            this.SwipeableStationItem(station, index)
          }
          .onClick(() => {
            this.activateStation(station);
          })
          .parallelGesture(
            LongPressGesture()
              .onAction(() => {
                if (this.settings.editMode) {
                  this.closeStationSwipe();
                  this.openStationEditor(station);
                }
              })
          )
        }, (station: Station) => station.id)
          .onMove((from: number, to: number) => {
            this.moveStationByIndex(from, to);
          })

        ListItem() {
          this.HomeActions()
        }
      }
      .width('100%')
      .layoutWeight(1)
      .cachedCount(10)
      .edgeEffect(EdgeEffect.Spring)
      .scrollBar(BarState.Off)

      if (this.showSleepPanel) {
        this.SleepPanel()
      }
      this.NowPlayingBar()
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.palette.background)
  }

  @Builder
  SettingsSectionTitle(title: string) {
    Text(title)
      .width('100%')
      .fontSize(20)
      .fontWeight(FontWeight.Bold)
      .fontColor(this.palette.textSecondary)
      .padding({ left: 20, right: 20, top: 14, bottom: 4 })
  }

  @Builder
  SettingsDivider() {
    Divider()
      .strokeWidth(0.7)
      .color(this.palette.divider)
      .margin({ left: 20, right: 20, top: 8, bottom: 8 })
  }

  @Builder
  SettingsSwitchRow(icon: string, title: string, subtitle: string, key: string) {
    Row({ space: 16 }) {
      Text(icon)
        .width(36)
        .fontSize(21)
        .fontColor(this.palette.textPrimary)
      Column({ space: 4 }) {
        Text(title)
          .fontSize(19)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .onClick(() => {
            this.updateSettingSwitch(key, !this.settingValue(key));
          })
        Text(subtitle)
          .fontSize(15)
          .fontColor(this.palette.textSecondary)
          .lineHeight(21)
          .onClick(() => {
            this.updateSettingSwitch(key, !this.settingValue(key));
          })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Toggle({ type: ToggleType.Switch, isOn: this.settingValue(key) })
        .selectedColor(this.primaryColor())
        .onChange((value: boolean) => {
          this.updateSettingSwitch(key, value);
        })
    }
    .width('100%')
    .padding({ left: 20, right: 20, top: 14, bottom: 14 })
  }

  @Builder
  SettingsActionRow(icon: string, title: string, subtitle: string, action: string) {
    Row({ space: 16 }) {
      Text(icon)
        .width(36)
        .fontSize(21)
        .fontColor(this.palette.textPrimary)
      Column({ space: 4 }) {
        Text(title)
          .fontSize(19)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
        Text(subtitle)
          .fontSize(15)
          .fontColor(this.palette.textSecondary)
          .lineHeight(21)
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .padding({ left: 20, right: 20, top: 14, bottom: 14 })
    .onClick(() => {
      this.runSettingAction(action);
    })
  }

  @Builder
  ThemeSettingsRow() {
    Row({ space: 16 }) {
      Text('◈')
        .width(36)
        .fontSize(21)
        .fontColor(this.palette.textPrimary)
      Column({ space: 4 }) {
        Text('应用主题')
          .fontSize(19)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
        Text(`当前主题：${this.palette.title}`)
          .fontSize(15)
          .fontColor(this.palette.textSecondary)
          .lineHeight(21)
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .padding({ left: 20, right: 20, top: 14, bottom: 14 })
    .onClick(() => {
      this.runSettingAction('theme');
    })
  }

  @Builder
  SettingsPage() {
    Column() {
      Row({ space: 10 }) {
        Text('←')
          .width(40)
          .height(40)
          .textAlign(TextAlign.Center)
          .fontSize(28)
          .fontColor(this.palette.textPrimary)
          .onClick(() => {
            this.showSettings = false;
          })
        Text('设置')
          .fontSize(30)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
      }
      .width('100%')
      .padding({ left: 10, right: 20, top: 8, bottom: 4 })

      List() {
        ListItem() {
          Column({ space: 8 }) {
            Row({ space: 8 }) {
              Text('⌕')
                .fontSize(22)
                .fontColor(this.palette.textSecondary)
              TextInput({ text: this.searchQuery, placeholder: '搜索电台' })
                .layoutWeight(1)
                .height(44)
                .fontSize(15)
                .backgroundColor(this.palette.surfaceVariant)
                .borderRadius(22)
                .onChange((value: string) => {
                  this.searchQuery = value;
                })
                .onSubmit(() => {
                  this.searchStations();
                })
              Button('搜索')
                .height(40)
                .backgroundColor(this.primaryColor())
                .fontColor(Color.White)
                .borderRadius(20)
                .enabled(!this.searching)
                .onClick(() => {
                  this.searchStations();
                })
            }
            .width('100%')
            if (this.searchResults.length > 0) {
              Column({ space: 2 }) {
                ForEach(this.searchResults.slice(0, 8), (station: Station) => {
                  Row({ space: 8 }) {
                    Text(station.name)
                      .fontSize(15)
                      .fontColor(this.palette.textPrimary)
                      .maxLines(1)
                      .textOverflow({ overflow: TextOverflow.Ellipsis })
                      .layoutWeight(1)
                    Text('添加')
                      .fontSize(14)
                      .fontColor(this.primaryColor())
                      .onClick(() => {
                        this.addSearchStation(station);
                      })
                  }
                  .width('100%')
                  .height(40)
                }, (station: Station) => station.id)
              }
              .width('100%')
            }
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 8, bottom: 10 })
        }

        ListItem() {
          this.SettingsSectionTitle('常规')
        }
        ListItem() { this.ThemeSettingsRow() }
        ListItem() {
          this.SettingsSwitchRow('◉', '动态颜色', '用户界面元素使用当前主题强调色。', 'dynamicColor')
        }
        ListItem() {
          this.SettingsSwitchRow('◐', '用户界面透明效果', '播放控件采用半透明背景。', 'transparentUi')
        }
        ListItem() {
          this.SettingsSwitchRow('▶', '点击广播电台', '点击电台任意部位开始收听。', 'clickStation')
        }
        ListItem() {
          this.SettingsSwitchRow('◎', '启动时自动播放', '打开应用后自动播放上次收听的电台。', 'autoPlay')
        }
        ListItem() {
          this.SettingsSwitchRow('↺', '记忆播放列表与进度', '保存当前列表和最后收听的电台。', 'playbackMemory')
        }
        ListItem() {
          this.SettingsDivider()
        }

        ListItem() {
          this.SettingsSectionTitle('维护')
        }
        ListItem() { this.SettingsActionRow('▧', '更新电台图标', '重新加载所有电台的最新图标。', 'refreshIcons') }
        ListItem() { this.SettingsActionRow('↗', '导出 M3U', '保存为可被其它播放器导入的 M3U 列表。', 'exportM3u') }
        ListItem() { this.SettingsActionRow('↙', '导入 M3U', '从 M3U / M3U8 播放列表追加电台。', 'importM3u') }
        ListItem() { this.SettingsActionRow('⇩', '备份电台', '保存包含台标与顺序的本地 JSON 备份。', 'backupLocal') }
        ListItem() { this.SettingsActionRow('⇧', '恢复电台', '从本地备份恢复并保持原顺序。', 'restoreLocal') }
        ListItem() { this.SettingsActionRow('☁', '在线备份 (WebDAV)', '备份电台、台标、设置与上下顺序。', 'webdav') }

        ListItem() {
          this.SettingsDivider()
        }
        ListItem() {
          this.SettingsSectionTitle('高级')
        }
        ListItem() {
          this.SettingsSwitchRow('ϟ', '0.25s 极速起播优化', '使用快速起播缓冲，重启应用后生效。', 'fastStart')
        }
        ListItem() {
          this.SettingsSwitchRow('▤', '使用更大缓冲', '使用更大的直播缓冲，重启应用后生效。', 'largeBuffer')
        }
        ListItem() {
          this.SettingsSwitchRow('✎', '编辑电台', '开启后可长按编辑、拖动排序或左滑删除。', 'editMode')
        }
        ListItem() {
          this.SettingsSwitchRow('↗', '编辑流媒体链接', '允许在编辑电台时修改流媒体链接。', 'editStreamUrl')
        }
        ListItem() {
          this.SettingsActionRow('⌁', '蓝牙设备识别权限', this.diagnostics, 'bluetoothPermission')
        }
        ListItem() {
          this.SettingsSwitchRow('⇄', '拔出耳机自动暂停', '耳机或蓝牙断开时自动暂停，避免外放。', 'pauseOnUnplug')
        }
        ListItem() {
          this.SettingsSwitchRow('✓', '蓝牙重连自动续播', '同一设备 120 分钟内重连时自动续播。', 'autoResume')
        }
        ListItem() {
          this.SettingsDivider()
        }
        ListItem() {
          this.SettingsSectionTitle('关于')
        }
        ListItem() { this.SettingsActionRow('↻', '检查更新', '当前版本 1.0.0 · GitHub Releases', 'checkUpdate') }
        ListItem() {
          this.SettingsSwitchRow('⟳', '自动检查并下载更新', '启动后自动检查新版本，安装由系统确认。', 'autoUpdate')
        }
        ListItem() { this.SettingsActionRow('⌘', 'GitHub 开源项目', 'github.com/zzg341125/QY-FM', 'github') }
        if (this.settingsStatus.length > 0) {
          ListItem() {
            Text(this.settingsStatus)
              .width('100%')
              .fontSize(13)
              .fontColor(this.palette.textSecondary)
              .padding({ left: 20, right: 20, top: 8, bottom: 16 })
          }
        }
      }
      .width('100%')
      .layoutWeight(1)
      .edgeEffect(EdgeEffect.Spring)
      .scrollBar(BarState.Off)

      this.NowPlayingBar()
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.palette.background)
  }

  @Builder
  DialogInput(label: string, value: string) {
    Row({ space: 10 }) {
      Text(label)
        .width(68)
        .fontSize(14)
        .fontColor(this.palette.textSecondary)
      TextInput({ text: value })
        .layoutWeight(1)
        .height(44)
        .fontSize(14)
        .fontColor(this.palette.textPrimary)
        .backgroundColor(this.palette.surface)
        .borderRadius(12)
        .border({ width: 1, color: this.palette.divider })
    }
    .width('100%')
  }

  @Builder
  ThemeDialog() {
    Column({ space: 10 }) {
      Row() {
        Text('应用主题')
          .fontSize(21)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .layoutWeight(1)
        Text('×')
          .width(32)
          .height(32)
          .textAlign(TextAlign.Center)
          .fontSize(24)
          .fontColor(this.palette.textSecondary)
          .onClick(() => {
            this.showTheme = false;
          })
      }
      .width('100%')

      List({ space: 4 }) {
        ForEach(THEME_PRESETS, (theme: ThemePreset) => {
          ListItem() {
            Row({ space: 12 }) {
              Text('●')
                .fontSize(18)
                .fontColor(theme.primary)
              Column({ space: 2 }) {
                Text(theme.title)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(this.palette.textPrimary)
                Text(theme.subtitle)
                  .fontSize(12)
                  .fontColor(this.palette.textSecondary)
                  .maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
              }
              .layoutWeight(1)
              .alignItems(HorizontalAlign.Start)
              if (this.settings.themeId === theme.id) {
                Text('✓')
                  .fontSize(18)
                  .fontColor(this.primaryColor())
              }
            }
            .width('100%')
            .height(58)
            .padding({ left: 8, right: 8 })
            .onClick(() => {
              this.selectTheme(theme);
            })
          }
        }, (theme: ThemePreset) => theme.id)
      }
      .width('100%')
      .height(360)
      .scrollBar(BarState.Off)
    }
    .width('92%')
    .padding(18)
    .borderRadius(24)
    .backgroundColor(this.palette.background)
    .shadow({ radius: 26, color: '#33000000', offsetY: 8 })
  }

  @Builder
  DeleteDialog() {
    if (this.pendingDelete !== undefined) {
      Column({ space: 14 }) {
        Text('移除电台')
          .fontSize(21)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .width('100%')
        Text(`确定从播放列表移除“${this.pendingDelete.name}”吗？`)
          .fontSize(15)
          .fontColor(this.palette.textSecondary)
          .width('100%')
        Row({ space: 10 }) {
          Button('取消')
            .layoutWeight(1)
            .height(44)
            .backgroundColor(this.palette.surfaceVariant)
            .fontColor(this.palette.textSecondary)
            .borderRadius(22)
            .onClick(() => {
              this.pendingDelete = undefined;
            })
          Button('移除')
            .layoutWeight(1)
            .height(44)
            .backgroundColor('#B3261E')
            .fontColor(Color.White)
            .borderRadius(22)
            .onClick(() => {
              if (this.pendingDelete !== undefined) {
                const id = this.pendingDelete.id;
                this.pendingDelete = undefined;
                this.deleteStation(id);
              }
            })
        }
        .width('100%')
      }
      .width('88%')
      .padding(18)
      .borderRadius(24)
      .backgroundColor(this.palette.background)
      .shadow({ radius: 26, color: '#33000000', offsetY: 8 })
    }
  }

  @Builder
  WebDavDialog() {
    Column({ space: 12 }) {
      Row() {
        Text('在线备份 (WebDAV)')
          .fontSize(21)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .layoutWeight(1)
        Text('×')
          .width(32)
          .height(32)
          .textAlign(TextAlign.Center)
          .fontSize(24)
          .fontColor(this.palette.textSecondary)
          .onClick(() => {
            this.showWebDav = false;
          })
      }
      .width('100%')

      Row({ space: 10 }) {
        Text('服务器')
          .width(68)
          .fontSize(14)
          .fontColor(this.palette.textSecondary)
        TextInput({ text: this.settings.webdavUrl, placeholder: 'https://dav.example.com/radio/' })
          .layoutWeight(1)
          .height(44)
          .fontSize(14)
          .backgroundColor(this.palette.surface)
          .borderRadius(12)
          .border({ width: 1, color: this.palette.divider })
          .onChange((value: string) => {
            this.updateWebDavUrl(value);
          })
      }
      .width('100%')
      Row({ space: 10 }) {
        Text('用户名')
          .width(68)
          .fontSize(14)
          .fontColor(this.palette.textSecondary)
        TextInput({ text: this.settings.webdavUser })
          .layoutWeight(1)
          .height(44)
          .fontSize(14)
          .backgroundColor(this.palette.surface)
          .borderRadius(12)
          .border({ width: 1, color: this.palette.divider })
          .onChange((value: string) => {
            this.updateWebDavUser(value);
          })
      }
      .width('100%')
      Row({ space: 10 }) {
        Text('密码')
          .width(68)
          .fontSize(14)
          .fontColor(this.palette.textSecondary)
        TextInput({ text: this.settings.webdavPassword })
          .type(InputType.Password)
          .layoutWeight(1)
          .height(44)
          .fontSize(14)
          .backgroundColor(this.palette.surface)
          .borderRadius(12)
          .border({ width: 1, color: this.palette.divider })
          .onChange((value: string) => {
            this.updateWebDavPassword(value);
          })
      }
      .width('100%')
      Row({ space: 10 }) {
        Text('文件名')
          .width(68)
          .fontSize(14)
          .fontColor(this.palette.textSecondary)
        TextInput({ text: this.settings.webdavFile })
          .layoutWeight(1)
          .height(44)
          .fontSize(14)
          .backgroundColor(this.palette.surface)
          .borderRadius(12)
          .border({ width: 1, color: this.palette.divider })
          .onChange((value: string) => {
            this.updateWebDavFile(value);
          })
      }
      .width('100%')
      Row({ space: 8 }) {
        Button('保存')
          .layoutWeight(1)
          .height(44)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textPrimary)
          .borderRadius(22)
          .onClick(() => {
            this.persistSettings();
          })
        Button('测试连接')
          .layoutWeight(1)
          .height(44)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textPrimary)
          .borderRadius(22)
          .enabled(!this.busy)
          .onClick(() => {
            this.testWebDav();
          })
      }
      .width('100%')
      Row({ space: 8 }) {
        Button('上传备份')
          .layoutWeight(1)
          .height(46)
          .backgroundColor(this.primaryColor())
          .fontColor(Color.White)
          .borderRadius(23)
          .enabled(!this.busy)
          .onClick(() => {
            this.backupToWebDav();
          })
        Button('从云端恢复')
          .layoutWeight(1)
          .height(46)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textPrimary)
          .borderRadius(23)
          .enabled(!this.busy)
          .onClick(() => {
            this.restoreFromWebDav();
          })
      }
      .width('100%')
      if (this.settingsStatus.length > 0) {
        Text(this.settingsStatus)
          .width('100%')
          .fontSize(13)
          .fontColor(this.palette.textSecondary)
      }
      Text('备份包含电台、台标和顺序；WebDAV 密码不会写入备份。')
        .width('100%')
        .fontSize(12)
        .fontColor(this.palette.textSecondary)
    }
    .width('92%')
    .padding(18)
    .borderRadius(24)
    .backgroundColor(this.palette.background)
    .shadow({ radius: 26, color: '#33000000', offsetY: 8 })
  }

  @Builder
  StationEditor() {
    Column({ space: 12 }) {
      Row() {
        Text(this.editorIsNew ? '添加新电台' : '编辑电台')
          .fontSize(21)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .layoutWeight(1)
        Text('×')
          .width(32)
          .height(32)
          .textAlign(TextAlign.Center)
          .fontSize(24)
          .fontColor(this.palette.textSecondary)
          .onClick(() => {
            this.showEditor = false;
          })
      }
      .width('100%')

      TextInput({ text: this.editorName, placeholder: '电台名称' })
        .height(44)
        .fontSize(15)
        .backgroundColor(this.palette.surface)
        .borderRadius(12)
        .border({ width: 1, color: this.palette.divider })
        .onChange((value: string) => {
          this.editorName = value;
        })
      if (this.editorIsNew || this.settings.editStreamUrl) {
        TextInput({ text: this.editorUrl, placeholder: '播放地址（m3u8 / mp3 / aac）' })
          .height(44)
          .fontSize(15)
          .backgroundColor(this.palette.surface)
          .borderRadius(12)
          .border({ width: 1, color: this.palette.divider })
          .onChange((value: string) => {
            this.editorUrl = value;
          })
      }
      TextInput({ text: this.editorTags, placeholder: '标签，例如：新闻、音乐、交通' })
        .height(44)
        .fontSize(15)
        .backgroundColor(this.palette.surface)
        .borderRadius(12)
        .border({ width: 1, color: this.palette.divider })
        .onChange((value: string) => {
          this.editorTags = value;
        })
      Row({ space: 12 }) {
        if (this.editorIcon.length > 0) {
          Image(this.editorIcon)
            .width(56)
            .height(56)
            .borderRadius(28)
            .objectFit(ImageFit.Cover)
        } else {
          Stack() {
            Text('FM')
              .fontSize(15)
              .fontWeight(FontWeight.Bold)
              .fontColor(this.palette.textSecondary)
          }
          .width(56)
          .height(56)
          .borderRadius(28)
          .backgroundColor(this.palette.surfaceVariant)
        }
        Button('更换台标')
          .layoutWeight(1)
          .height(42)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textPrimary)
          .borderRadius(21)
          .onClick(() => {
            this.pickStationIcon();
          })
        Button('清除')
          .width(74)
          .height(42)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textSecondary)
          .borderRadius(21)
          .onClick(() => {
            this.editorIcon = '';
          })
      }
      .width('100%')
      Row({ space: 10 }) {
        Button('取消')
          .layoutWeight(1)
          .height(44)
          .backgroundColor(this.palette.surfaceVariant)
          .fontColor(this.palette.textSecondary)
          .borderRadius(22)
          .onClick(() => {
            this.showEditor = false;
          })
        Button('保存更改')
          .layoutWeight(1)
          .height(44)
          .backgroundColor(this.primaryColor())
          .fontColor(Color.White)
          .borderRadius(22)
          .onClick(() => {
            this.saveStation();
          })
      }
      .width('100%')
      if (!this.editorIsNew && this.settings.editMode) {
        Button('删除电台')
          .width('100%')
          .height(42)
          .fontSize(15)
          .fontColor(COLOR_DANGER)
          .backgroundColor(COLOR_DANGER_BG)
          .borderRadius(21)
          .onClick(() => {
            const station = this.stations.find((item: Station) => item.id === this.editorId);
            if (station !== undefined) {
              this.showEditor = false;
              this.pendingDelete = station;
            }
          })
      }
    }
    .width('92%')
    .padding(18)
    .borderRadius(24)
    .backgroundColor(this.palette.background)
    .shadow({ radius: 26, color: '#33000000', offsetY: 8 })
  }

  @Builder
  UpdateDialog() {
    if (this.updateInfo !== undefined) {
      Column({ space: 13 }) {
        Text('发现新版本')
          .fontSize(21)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.palette.textPrimary)
          .width('100%')
        Text(this.updateInfo.title)
          .fontSize(16)
          .fontColor(this.primaryColor())
          .width('100%')
        Text(this.updateInfo.notes.length > 0 ? this.updateInfo.notes : '请前往发布页下载最新 HAP。')
          .fontSize(14)
          .fontColor(this.palette.textSecondary)
          .maxLines(6)
          .width('100%')
        Row({ space: 10 }) {
          Button('稍后')
            .layoutWeight(1)
            .height(44)
            .backgroundColor(this.palette.surfaceVariant)
            .fontColor(this.palette.textSecondary)
            .borderRadius(22)
            .onClick(() => {
              this.updateInfo = undefined;
            })
          Button('前往更新')
            .layoutWeight(1)
            .height(44)
            .backgroundColor(this.primaryColor())
            .fontColor(Color.White)
            .borderRadius(22)
            .onClick(() => {
              this.openReleasePage();
            })
        }
      }
      .width('88%')
      .padding(18)
      .borderRadius(24)
      .backgroundColor(this.palette.background)
      .shadow({ radius: 26, color: '#33000000', offsetY: 8 })
    }
  }

  build() {
    Stack() {
      if (this.showSettings) {
        this.SettingsPage()
      } else {
        this.HomePage()
      }

      if (this.showWebDav) {
        Column() {
          this.WebDavDialog()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#66000000')
      }

      if (this.showTheme) {
        Column() {
          this.ThemeDialog()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#66000000')
      }

      if (this.pendingDelete !== undefined) {
        Column() {
          this.DeleteDialog()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#66000000')
      }

      if (this.showEditor) {
        Column() {
          this.StationEditor()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#66000000')
      }

      if (this.updateInfo !== undefined) {
        Column() {
          this.UpdateDialog()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#66000000')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.palette.background)
  }
}
