import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { Station } from '../models/Station';
import { PRESET_STATIONS } from '../data/PresetStations';

interface RadioBrowserStation {
  stationuuid: string;
  name: string;
  url: string;
  url_resolved?: string;
  favicon?: string;
  country?: string;
  state?: string;
  tags?: string;
}

export class StationSearchService {
  searchLocal(query: string): Station[] {
    const keyword = query.trim().toLowerCase();
    if (keyword.length === 0) {
      return [];
    }
    return PRESET_STATIONS.filter((station: Station) => {
      const text = `${station.name} ${station.tags} ${station.country} ${station.state}`.toLowerCase();
      return text.includes(keyword);
    }).slice(0, 20);
  }

  async search(query: string): Promise<Station[]> {
    const keyword = query.trim();
    if (keyword.length === 0) {
      return [];
    }
    const local = this.searchLocal(keyword);
    const request = http.createHttp();
    try {
      const encoded = encodeURIComponent(keyword);
      const url =
        `https://de1.api.radio-browser.info/json/stations/search?name=${encoded}` +
        '&hidebroken=true&order=votes&reverse=true&limit=40';
      const response = await request.request(url, {
        method: http.RequestMethod.GET,
        connectTimeout: 15000,
        readTimeout: 30000,
        expectDataType: http.HttpDataType.STRING,
        usingCache: false
      });
      if (response.responseCode !== 200 || typeof response.result !== 'string') {
        return local;
      }
      const rows = JSON.parse(response.result) as RadioBrowserStation[];
      const remote = rows.map((row: RadioBrowserStation): Station => {
        return {
          id: `radio-${row.stationuuid}`,
          name: row.name.trim(),
          streamUrl: (row.url_resolved ?? row.url).trim(),
          iconUrl: row.favicon ?? '',
          tags: row.tags ?? '',
          country: row.country ?? '',
          state: row.state ?? '',
          favorite: false
        };
      }).filter((station: Station) => station.name.length > 0 && station.streamUrl.length > 0);
      const merged = local.concat(remote);
      const unique = new Map<string, Station>();
      merged.forEach((station: Station) => unique.set(station.id, station));
      return Array.from(unique.values()).slice(0, 60);
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`在线电台搜索失败: ${businessError.message}`);
      return local;
    } finally {
      request.destroy();
    }
  }
}
