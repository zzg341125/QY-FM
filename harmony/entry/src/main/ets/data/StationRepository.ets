import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import {
  AppSettings,
  BackupResult,
  copySettings,
  copyStation,
  createDefaultSettings,
  Station,
  StationBackup,
  PortableSettings
} from '../models/Station';
import { PRESET_STATIONS } from './PresetStations';

const PREFERENCES_NAME: string = 'qyfm_store';
const STATIONS_KEY: string = 'stations';
const SETTINGS_KEY: string = 'settings';
const BACKUP_SCHEMA_VERSION: number = 2;

export class StationRepository {
  private store: preferences.Preferences | undefined = undefined;

  async init(context: common.Context): Promise<void> {
    this.store = await preferences.getPreferences(context, PREFERENCES_NAME);
  }

  private requireStore(): preferences.Preferences {
    if (this.store === undefined) {
      throw new Error('StationRepository 尚未初始化');
    }
    return this.store;
  }

  async loadStations(): Promise<Station[]> {
    const store = this.requireStore();
    const raw = await store.get(STATIONS_KEY, '');
    if (typeof raw !== 'string' || raw.length === 0) {
      const defaults = PRESET_STATIONS.map((station: Station) => copyStation(station));
      await this.saveStations(defaults);
      return defaults;
    }
    try {
      const parsed = JSON.parse(raw) as Station[];
      if (!Array.isArray(parsed)) {
        return PRESET_STATIONS.map((station: Station) => copyStation(station));
      }
      const ordered: Station[] = [];
      const seen = new Set<string>();
      parsed.forEach((station: Station) => {
        if (station.id.length === 0 || seen.has(station.id)) {
          return;
        }
        seen.add(station.id);
        ordered.push(copyStation(station));
      });
      return ordered.length > 0 ? ordered : PRESET_STATIONS.map((station: Station) => copyStation(station));
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`读取电台列表失败: ${businessError.message}`);
      return PRESET_STATIONS.map((station: Station) => copyStation(station));
    }
  }

  async saveStations(stations: Station[]): Promise<void> {
    const store = this.requireStore();
    await store.put(STATIONS_KEY, JSON.stringify(stations));
    await store.flush();
  }

  async loadSettings(): Promise<AppSettings> {
    const store = this.requireStore();
    const raw = await store.get(SETTINGS_KEY, '');
    if (typeof raw !== 'string' || raw.length === 0) {
      return createDefaultSettings();
    }
    try {
      const parsed = JSON.parse(raw) as AppSettings;
      const defaults = createDefaultSettings();
      return {
        autoUpdate: parsed.autoUpdate ?? defaults.autoUpdate,
        autoResume: parsed.autoResume ?? defaults.autoResume,
        resumeWindowMinutes: parsed.resumeWindowMinutes ?? defaults.resumeWindowMinutes,
        editMode: parsed.editMode ?? defaults.editMode,
        editStreamUrl: parsed.editStreamUrl ?? defaults.editStreamUrl,
        themeId: parsed.themeId ?? defaults.themeId,
        dynamicColor: parsed.dynamicColor ?? defaults.dynamicColor,
        transparentUi: parsed.transparentUi ?? defaults.transparentUi,
        clickStationToPlay: parsed.clickStationToPlay ?? defaults.clickStationToPlay,
        autoPlayOnLaunch: parsed.autoPlayOnLaunch ?? defaults.autoPlayOnLaunch,
        playbackMemory: parsed.playbackMemory ?? defaults.playbackMemory,
        fastStart: parsed.fastStart ?? defaults.fastStart,
        largeBuffer: parsed.largeBuffer ?? defaults.largeBuffer,
        pauseOnUnplug: parsed.pauseOnUnplug ?? defaults.pauseOnUnplug,
        lastStationId: parsed.lastStationId ?? defaults.lastStationId,
        webdavUrl: parsed.webdavUrl ?? defaults.webdavUrl,
        webdavUser: parsed.webdavUser ?? defaults.webdavUser,
        webdavPassword: parsed.webdavPassword ?? defaults.webdavPassword,
        webdavFile: parsed.webdavFile ?? defaults.webdavFile
      };
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`读取设置失败: ${businessError.message}`);
      return createDefaultSettings();
    }
  }

  async saveSettings(settings: AppSettings): Promise<void> {
    const store = this.requireStore();
    await store.put(SETTINGS_KEY, JSON.stringify(copySettings(settings)));
    await store.flush();
  }

  exportBackup(stations: Station[], settings: AppSettings): string {
    const stationOrder: string[] = stations.map((station: Station) => station.id);
    const portableSettings: PortableSettings = {
      autoUpdate: settings.autoUpdate,
      autoResume: settings.autoResume,
      resumeWindowMinutes: settings.resumeWindowMinutes,
      editMode: settings.editMode,
      editStreamUrl: settings.editStreamUrl,
      themeId: settings.themeId,
      dynamicColor: settings.dynamicColor,
      transparentUi: settings.transparentUi,
      clickStationToPlay: settings.clickStationToPlay,
      autoPlayOnLaunch: settings.autoPlayOnLaunch,
      playbackMemory: settings.playbackMemory,
      fastStart: settings.fastStart,
      largeBuffer: settings.largeBuffer,
      pauseOnUnplug: settings.pauseOnUnplug,
      lastStationId: settings.lastStationId,
      webdavUrl: settings.webdavUrl,
      webdavUser: settings.webdavUser,
      webdavFile: settings.webdavFile
    };
    const backup: StationBackup = {
      schemaVersion: BACKUP_SCHEMA_VERSION,
      app: 'QYFM HarmonyOS',
      appVersion: '1.0.0',
      exportedAt: new Date().toISOString(),
      stationOrder: stationOrder,
      stations: stations.map((station: Station) => copyStation(station)),
      settings: portableSettings
    };
    return JSON.stringify(backup);
  }

  parseBackup(text: string): BackupResult {
    const backup = JSON.parse(text) as StationBackup;
    if (backup.schemaVersion !== BACKUP_SCHEMA_VERSION || !Array.isArray(backup.stations)) {
      throw new Error('不是受支持的 QYFM HarmonyOS 备份');
    }

    const unique = new Map<string, Station>();
    backup.stations.forEach((station: Station) => {
      if (station.id.length > 0 && station.name.length > 0 && station.streamUrl.length > 0) {
        unique.set(station.id, copyStation(station));
      }
    });

    const ordered: Station[] = [];
    const included = new Set<string>();
    backup.stationOrder.forEach((id: string) => {
      const station = unique.get(id);
      if (station !== undefined && !included.has(id)) {
        included.add(id);
        ordered.push(station);
      }
    });
    unique.forEach((station: Station, id: string) => {
      if (!included.has(id)) {
        ordered.push(station);
      }
    });

    const defaultSettings = createDefaultSettings();
    if (backup.settings !== undefined) {
      defaultSettings.autoUpdate = backup.settings.autoUpdate ?? defaultSettings.autoUpdate;
      defaultSettings.autoResume = backup.settings.autoResume ?? defaultSettings.autoResume;
      defaultSettings.resumeWindowMinutes =
        backup.settings.resumeWindowMinutes ?? defaultSettings.resumeWindowMinutes;
      defaultSettings.editMode = backup.settings.editMode ?? defaultSettings.editMode;
      defaultSettings.editStreamUrl = backup.settings.editStreamUrl ?? defaultSettings.editStreamUrl;
      defaultSettings.themeId = backup.settings.themeId ?? defaultSettings.themeId;
      defaultSettings.dynamicColor = backup.settings.dynamicColor ?? defaultSettings.dynamicColor;
      defaultSettings.transparentUi = backup.settings.transparentUi ?? defaultSettings.transparentUi;
      defaultSettings.clickStationToPlay =
        backup.settings.clickStationToPlay ?? defaultSettings.clickStationToPlay;
      defaultSettings.autoPlayOnLaunch =
        backup.settings.autoPlayOnLaunch ?? defaultSettings.autoPlayOnLaunch;
      defaultSettings.playbackMemory = backup.settings.playbackMemory ?? defaultSettings.playbackMemory;
      defaultSettings.fastStart = backup.settings.fastStart ?? defaultSettings.fastStart;
      defaultSettings.largeBuffer = backup.settings.largeBuffer ?? defaultSettings.largeBuffer;
      defaultSettings.pauseOnUnplug = backup.settings.pauseOnUnplug ?? defaultSettings.pauseOnUnplug;
      defaultSettings.lastStationId = backup.settings.lastStationId ?? defaultSettings.lastStationId;
      defaultSettings.webdavUrl = backup.settings.webdavUrl ?? defaultSettings.webdavUrl;
      defaultSettings.webdavUser = backup.settings.webdavUser ?? defaultSettings.webdavUser;
      defaultSettings.webdavFile = backup.settings.webdavFile ?? defaultSettings.webdavFile;
    }
    return {
      stations: ordered,
      settings: defaultSettings
    };
  }
}
