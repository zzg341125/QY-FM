export interface Station {
  id: string;
  name: string;
  streamUrl: string;
  iconUrl: string;
  tags: string;
  country: string;
  state: string;
  favorite: boolean;
}

export interface AppSettings {
  autoUpdate: boolean;
  autoResume: boolean;
  resumeWindowMinutes: number;
  editMode: boolean;
  editStreamUrl: boolean;
  themeId: string;
  dynamicColor: boolean;
  transparentUi: boolean;
  clickStationToPlay: boolean;
  autoPlayOnLaunch: boolean;
  playbackMemory: boolean;
  fastStart: boolean;
  largeBuffer: boolean;
  pauseOnUnplug: boolean;
  lastStationId: string;
  webdavUrl: string;
  webdavUser: string;
  webdavPassword: string;
  webdavFile: string;
}

export interface StationBackup {
  schemaVersion: number;
  app: string;
  appVersion: string;
  exportedAt: string;
  stationOrder: string[];
  stations: Station[];
  settings?: PortableSettings;
}

export interface PortableSettings {
  autoUpdate: boolean;
  autoResume: boolean;
  resumeWindowMinutes: number;
  editMode: boolean;
  editStreamUrl: boolean;
  themeId: string;
  dynamicColor: boolean;
  transparentUi: boolean;
  clickStationToPlay: boolean;
  autoPlayOnLaunch: boolean;
  playbackMemory: boolean;
  fastStart: boolean;
  largeBuffer: boolean;
  pauseOnUnplug: boolean;
  lastStationId: string;
  webdavUrl: string;
  webdavUser: string;
  webdavFile: string;
}

export interface BackupResult {
  stations: Station[];
  settings: AppSettings;
}

export function createDefaultSettings(): AppSettings {
  return {
    autoUpdate: true,
    autoResume: true,
    resumeWindowMinutes: 120,
    editMode: true,
    editStreamUrl: true,
    themeId: 'lunar_light',
    dynamicColor: true,
    transparentUi: false,
    clickStationToPlay: true,
    autoPlayOnLaunch: true,
    playbackMemory: true,
    fastStart: true,
    largeBuffer: false,
    pauseOnUnplug: true,
    lastStationId: '',
    webdavUrl: '',
    webdavUser: '',
    webdavPassword: '',
    webdavFile: 'qyfm-backup.json'
  };
}

export function copyStation(station: Station): Station {
  return {
    id: station.id,
    name: station.name,
    streamUrl: station.streamUrl,
    iconUrl: station.iconUrl,
    tags: station.tags,
    country: station.country,
    state: station.state,
    favorite: station.favorite
  };
}

export function copySettings(settings: AppSettings): AppSettings {
  return {
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
    webdavPassword: settings.webdavPassword,
    webdavFile: settings.webdavFile
  };
}
