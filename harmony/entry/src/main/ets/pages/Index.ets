import { abilityAccessCtrl, common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import {
  AppSettings,
  copySettings,
  createDefaultSettings,
  Station
} from '../models/Station';
import { StationRepository } from '../data/StationRepository';
import { RadioController, RadioSnapshot, RadioStateListener } from '../services/RadioController';
import { WebDavBackupService, WebDavConfig } from '../services/WebDavBackupService';
import { AppUpdateService, NativeUpdateInfo } from '../services/AppUpdateService';
import { StationIconPicker } from '../services/StationIconPicker';

@Entry
@Component
struct Index {
  @State stations: Station[] = [];
  @State currentStation: Station | undefined = undefined;
  @State isPlaying: boolean = false;
  @State waitingForBluetooth: boolean = false;
  @State playbackStatus: string = '正在载入电台';
  @State diagnostics: string = '蓝牙监听尚未启动';
  @State sleepTimerMinutes: number = 0;
  @State editMode: boolean = false;
  @State showEditor: boolean = false;
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
  private abilityContext: common.UIAbilityContext | undefined = undefined;

  private readonly snapshotListener: RadioStateListener = (snapshot: RadioSnapshot): void => {
    this.currentStation = snapshot.currentStation;
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
      if (this.stations.length > 0) {
        this.currentStation = this.stations[0];
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
    await this.radio.playStation(station);
  }

  private async togglePlayback(): Promise<void> {
    await this.radio.togglePlayback();
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
    if (!this.editMode) {
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

  private async moveStation(index: number, delta: number): Promise<void> {
    if (!this.editMode) {
      return;
    }
    const target = index + delta;
    if (target < 0 || target >= this.stations.length) {
      return;
    }
    const next = this.stations.slice();
    const removed = next.splice(index, 1);
    next.splice(target, 0, removed[0]);
    this.stations = next;
    await this.repository.saveStations(this.stations);
    this.playbackStatus = '电台顺序已保存';
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

  @Builder
  StationDeleteAction(id: string) {
    Column() {
      if (this.editMode) {
        Button('删除')
          .width(76)
          .height(52)
          .fontColor(Color.White)
          .backgroundColor('#D84A4A')
          .onClick(() => {
            this.deleteStation(id);
          })
      }
    }
    .width(this.editMode ? 88 : 0)
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  StationRow(station: Station, index: number) {
    Row({ space: 12 }) {
      if (station.iconUrl.length > 0) {
        Image(station.iconUrl)
          .width(52)
          .height(52)
          .borderRadius(14)
          .objectFit(ImageFit.Cover)
          .alt($r('app.media.icon'))
      } else {
        Stack() {
          Text('FM')
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor('#EAF7FF')
        }
        .width(52)
        .height(52)
        .borderRadius(14)
        .backgroundColor('#176B88')
      }

      Column({ space: 5 }) {
        Text(station.name)
          .fontSize(17)
          .fontWeight(this.currentStation?.id === station.id ? FontWeight.Bold : FontWeight.Medium)
          .fontColor('#F5FAFD')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(station.tags.length > 0 ? station.tags : `${station.country} ${station.state}`)
          .fontSize(12)
          .fontColor('#91A7B5')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)

      if (this.editMode) {
        Column({ space: 2 }) {
          Button('↑')
            .width(34)
            .height(28)
            .fontSize(15)
            .backgroundColor('#244455')
            .onClick(() => {
              this.moveStation(index, -1);
            })
          Button('↓')
            .width(34)
            .height(28)
            .fontSize(15)
            .backgroundColor('#244455')
            .onClick(() => {
              this.moveStation(index, 1);
            })
        }
        Button('编辑')
          .height(34)
          .fontSize(12)
          .backgroundColor('#244455')
          .onClick(() => {
            this.openStationEditor(station);
          })
      }

      Text(this.currentStation?.id === station.id && this.isPlaying ? 'Ⅱ' : '▶')
        .width(40)
        .height(40)
        .textAlign(TextAlign.Center)
        .fontSize(16)
        .fontColor('#03131B')
        .backgroundColor('#62D5C7')
        .borderRadius(20)
        .onClick(() => {
          if (this.currentStation?.id === station.id) {
            this.togglePlayback();
          } else {
            this.playStation(station);
          }
        })
    }
    .width('100%')
    .height(82)
    .padding({ left: 12, right: 10 })
    .borderRadius(18)
    .backgroundColor(this.currentStation?.id === station.id ? '#123E4E' : '#0E2733')
  }

  @Builder
  PlaybackPage() {
    Column() {
      Row() {
        Column({ space: 4 }) {
          Text('QY·FM')
            .fontSize(29)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F5FAFD')
          Text('原生鸿蒙 · 曲阳收音机')
            .fontSize(12)
            .fontColor('#83AABA')
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        Button(this.editMode ? '完成' : '编辑电台')
          .height(36)
          .fontSize(13)
          .backgroundColor(this.editMode ? '#62D5C7' : '#244455')
          .fontColor(this.editMode ? '#03131B' : '#F5FAFD')
          .onClick(() => {
            this.editMode = !this.editMode;
          })
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 18, bottom: 12 })

      Column({ space: 8 }) {
        Text(this.currentStation?.name ?? '请选择电台')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor('#F5FAFD')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(this.playbackStatus)
          .fontSize(13)
          .fontColor(this.waitingForBluetooth ? '#FFD27D' : '#9CC1D0')
          .maxLines(2)
        Row({ space: 10 }) {
          Button(this.isPlaying ? '暂停' : (this.waitingForBluetooth ? '等待蓝牙' : '播放'))
            .layoutWeight(1)
            .height(44)
            .backgroundColor('#62D5C7')
            .fontColor('#03131B')
            .enabled(!this.waitingForBluetooth)
            .onClick(() => {
              this.togglePlayback();
            })
          Button('停止')
            .width(84)
            .height(44)
            .backgroundColor('#244455')
            .onClick(() => {
              this.radio.stop();
            })
        }
        .width('100%')
      }
      .width('100%')
      .padding(18)
      .borderRadius(22)
      .backgroundColor('#0E2733')
      .margin({ left: 16, right: 16, bottom: 10 })

      Row({ space: 8 }) {
        Text('睡眠定时')
          .fontSize(12)
          .fontColor('#83AABA')
        ForEach([0, 15, 30, 60], (minutes: number) => {
          Button(minutes === 0 ? '关' : `${minutes}分`)
            .height(30)
            .fontSize(12)
            .backgroundColor(this.sleepTimerMinutes === minutes ? '#62D5C7' : '#173744')
            .fontColor(this.sleepTimerMinutes === minutes ? '#03131B' : '#D9E6EC')
            .onClick(() => {
              this.radio.setSleepTimer(minutes);
            })
        }, (minutes: number) => minutes.toString())
      }
      .width('100%')
      .padding({ left: 20, right: 20, bottom: 8 })

      Row() {
        Text('我的电台')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#EAF3F7')
        Blank()
        Button('+ 新增')
          .height(32)
          .fontSize(12)
          .backgroundColor('#173744')
          .onClick(() => {
            this.openNewStation();
          })
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 4, bottom: 8 })

      List({ space: 8 }) {
        ForEach(this.stations, (station: Station, index: number) => {
          ListItem() {
            this.StationRow(station, index)
          }
          .swipeAction({
            end: {
              builder: () => {
                this.StationDeleteAction(station.id)
              }
            }
          })
        }, (station: Station) => station.id)
      }
      .width('100%')
      .layoutWeight(1)
      .padding({ left: 12, right: 12 })
      .cachedCount(8)
      .edgeEffect(EdgeEffect.Spring)
      .scrollBar(BarState.Off)
      .nestedScroll({
        scrollForward: NestedScrollMode.PARENT_FIRST,
        scrollBackward: NestedScrollMode.SELF_FIRST
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#071A23')
  }

  @Builder
  SettingsPage() {
    Scroll() {
      Column({ space: 16 }) {
        Column({ space: 4 }) {
          Text('设置')
            .fontSize(28)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F5FAFD')
            .width('100%')
          Text('在线升级、蓝牙续播和 WebDAV 备份')
            .fontSize(12)
            .fontColor('#83AABA')
            .width('100%')
        }
        .padding({ top: 20, bottom: 2 })

        Column({ space: 14 }) {
          Row() {
            Column({ space: 3 }) {
              Text('自动检查新版本')
                .fontSize(16)
                .fontColor('#F5FAFD')
              Text('启动时检查，升级仍由系统安装器确认')
                .fontSize(11)
                .fontColor('#83AABA')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            Toggle({ type: ToggleType.Switch, isOn: this.settings.autoUpdate })
              .onChange((value: boolean) => {
                const settings = copySettings(this.settings);
                settings.autoUpdate = value;
                this.settings = settings;
                this.persistSettings();
              })
          }
          Button('手动检查更新')
            .width('100%')
            .height(42)
            .backgroundColor('#244455')
            .onClick(() => {
              this.checkForUpdates(true);
            })
        }
        .width('100%')
        .padding(16)
        .borderRadius(18)
        .backgroundColor('#0E2733')

        Column({ space: 14 }) {
          Row() {
            Column({ space: 3 }) {
              Text('蓝牙重连自动续播')
                .fontSize(16)
                .fontColor('#F5FAFD')
              Text('同一设备断开后 120 分钟内重连，锁屏时同样生效')
                .fontSize(11)
                .fontColor('#83AABA')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            Toggle({ type: ToggleType.Switch, isOn: this.settings.autoResume })
              .onChange((value: boolean) => {
                const settings = copySettings(this.settings);
                settings.autoResume = value;
                settings.resumeWindowMinutes = 120;
                this.settings = settings;
                this.persistSettings();
              })
          }
          Text(this.diagnostics)
            .width('100%')
            .fontSize(11)
            .fontColor(this.waitingForBluetooth ? '#FFD27D' : '#83AABA')
            .maxLines(3)
        }
        .width('100%')
        .padding(16)
        .borderRadius(18)
        .backgroundColor('#0E2733')

        Column({ space: 12 }) {
          Text('WebDAV 备份')
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F5FAFD')
            .width('100%')
          Row({ space: 10 }) {
            Text('服务器')
              .width(68)
              .fontSize(13)
              .fontColor('#BDD2DC')
            TextInput({ text: this.settings.webdavUrl, placeholder: 'https://dav.example.com/radio/' })
              .layoutWeight(1)
              .height(42)
              .backgroundColor('#102F3C')
              .onChange((value: string) => {
                this.updateWebDavUrl(value);
              })
          }
          Row({ space: 10 }) {
            Text('用户名')
              .width(68)
              .fontSize(13)
              .fontColor('#BDD2DC')
            TextInput({ text: this.settings.webdavUser })
              .layoutWeight(1)
              .height(42)
              .backgroundColor('#102F3C')
              .onChange((value: string) => {
                this.updateWebDavUser(value);
              })
          }
          Row({ space: 10 }) {
            Text('密码')
              .width(68)
              .fontSize(13)
              .fontColor('#BDD2DC')
            TextInput({ text: this.settings.webdavPassword })
              .type(InputType.Password)
              .layoutWeight(1)
              .height(42)
              .backgroundColor('#102F3C')
              .onChange((value: string) => {
                this.updateWebDavPassword(value);
              })
          }
          Row({ space: 10 }) {
            Text('文件名')
              .width(68)
              .fontSize(13)
              .fontColor('#BDD2DC')
            TextInput({ text: this.settings.webdavFile })
              .layoutWeight(1)
              .height(42)
              .backgroundColor('#102F3C')
              .onChange((value: string) => {
                this.updateWebDavFile(value);
              })
          }
          Row({ space: 8 }) {
            Button('保存')
              .layoutWeight(1)
              .height(40)
              .backgroundColor('#244455')
              .onClick(() => {
                this.persistSettings();
              })
            Button('测试')
              .layoutWeight(1)
              .height(40)
              .backgroundColor('#244455')
              .enabled(!this.busy)
              .onClick(() => {
                this.testWebDav();
              })
          }
          Row({ space: 8 }) {
            Button('立即备份')
              .layoutWeight(1)
              .height(44)
              .backgroundColor('#62D5C7')
              .fontColor('#03131B')
              .enabled(!this.busy)
              .onClick(() => {
                this.backupToWebDav();
              })
            Button('恢复备份')
              .layoutWeight(1)
              .height(44)
              .backgroundColor('#173744')
              .enabled(!this.busy)
              .onClick(() => {
                this.restoreFromWebDav();
              })
          }
          if (this.settingsStatus.length > 0) {
            Text(this.settingsStatus)
              .width('100%')
              .fontSize(12)
              .fontColor('#9CC1D0')
          }
        }
        .width('100%')
        .padding(16)
        .borderRadius(18)
        .backgroundColor('#0E2733')

        Text('Android APK 独立保留；HarmonyOS 原生版使用 harmony-v* 版本标签。')
          .width('100%')
          .fontSize(11)
          .fontColor('#627D89')
          .textAlign(TextAlign.Center)
          .padding({ bottom: 24 })
      }
      .padding({ left: 16, right: 16 })
    }
    .width('100%')
    .height('100%')
    .edgeEffect(EdgeEffect.Spring)
    .scrollBar(BarState.Off)
    .backgroundColor('#071A23')
  }

  @Builder
  StationEditor() {
    Column({ space: 12 }) {
      Row() {
        Text(this.editorIsNew ? '新增电台' : '编辑电台')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#F5FAFD')
          .layoutWeight(1)
        Button('关闭')
          .height(32)
          .backgroundColor('#244455')
          .onClick(() => {
            this.showEditor = false;
          })
      }
      TextInput({ text: this.editorName, placeholder: '电台名称' })
        .height(44)
        .backgroundColor('#102F3C')
        .onChange((value: string) => {
          this.editorName = value;
        })
      TextInput({ text: this.editorUrl, placeholder: '播放地址（m3u8 / mp3 / aac）' })
        .height(44)
        .backgroundColor('#102F3C')
        .onChange((value: string) => {
          this.editorUrl = value;
        })
      TextInput({ text: this.editorTags, placeholder: '标签，例如：新闻、音乐、交通' })
        .height(44)
        .backgroundColor('#102F3C')
        .onChange((value: string) => {
          this.editorTags = value;
        })
      Row({ space: 12 }) {
        if (this.editorIcon.length > 0) {
          Image(this.editorIcon)
            .width(54)
            .height(54)
            .borderRadius(14)
            .objectFit(ImageFit.Cover)
        } else {
          Stack() {
            Text('FM')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor('#EAF7FF')
          }
          .width(54)
          .height(54)
          .borderRadius(14)
          .backgroundColor('#176B88')
        }
        Button('从图库选择台标')
          .layoutWeight(1)
          .height(40)
          .backgroundColor('#244455')
          .onClick(() => {
            this.pickStationIcon();
          })
        Button('清除')
          .height(40)
          .backgroundColor('#173744')
          .onClick(() => {
            this.editorIcon = '';
          })
      }
      Row({ space: 10 }) {
        Button('取消')
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#244455')
          .onClick(() => {
            this.showEditor = false;
          })
        Button('保存电台')
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#62D5C7')
          .fontColor('#03131B')
          .onClick(() => {
            this.saveStation();
          })
      }
    }
    .width('92%')
    .padding(18)
    .borderRadius(22)
    .backgroundColor('#0E2733')
    .shadow({ radius: 28, color: '#AA000000', offsetY: 8 })
  }

  @Builder
  UpdateDialog() {
    if (this.updateInfo !== undefined) {
      Column({ space: 13 }) {
        Text('发现原生鸿蒙新版本')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#F5FAFD')
          .width('100%')
        Text(this.updateInfo.title)
          .fontSize(15)
          .fontColor('#62D5C7')
          .width('100%')
        Text(this.updateInfo.notes.length > 0 ? this.updateInfo.notes : '请在发布页下载最新 HAP。')
          .fontSize(12)
          .fontColor('#9CC1D0')
          .maxLines(6)
          .width('100%')
        Row({ space: 10 }) {
          Button('稍后')
            .layoutWeight(1)
            .height(42)
            .backgroundColor('#244455')
            .onClick(() => {
              this.updateInfo = undefined;
            })
          Button('前往更新')
            .layoutWeight(1)
            .height(42)
            .backgroundColor('#62D5C7')
            .fontColor('#03131B')
            .onClick(() => {
              this.openReleasePage();
            })
        }
      }
      .width('88%')
      .padding(18)
      .borderRadius(22)
      .backgroundColor('#0E2733')
      .shadow({ radius: 28, color: '#AA000000', offsetY: 8 })
    }
  }

  build() {
    Stack() {
      Tabs({ barPosition: BarPosition.End }) {
        TabContent() {
          this.PlaybackPage()
        }
        .tabBar('电台')

        TabContent() {
          this.SettingsPage()
        }
        .tabBar('设置')
      }
      .barHeight(58)
      .barBackgroundColor('#0A202A')
      .width('100%')
      .height('100%')

      if (this.showEditor) {
        Column() {
          this.StationEditor()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#99000000')
      }

      if (this.updateInfo !== undefined) {
        Column() {
          this.UpdateDialog()
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
        .backgroundColor('#99000000')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#071A23')
  }
}
