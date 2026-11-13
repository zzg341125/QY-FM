import { Station } from '../models/Station';

export interface M3uEntry {
  name: string;
  url: string;
  iconUrl: string;
}

export class M3uPlaylistService {
  parse(content: string): M3uEntry[] {
    const result: M3uEntry[] = [];
    const seenUrls = new Set<string>();
    let pendingName = '';
    let pendingLogo = '';
    const lines = content.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');
    lines.forEach((raw: string) => {
      const line = raw.trim().replaceAll('\uFEFF', '');
      if (line.startsWith('#EXTINF')) {
        const comma = line.lastIndexOf(',');
        pendingName = comma >= 0 ? line.substring(comma + 1).trim() : '';
        const logoMatch = line.match(/tvg-logo\s*=\s*"([^"]*)"/i);
        pendingLogo = logoMatch === null ? '' : logoMatch[1].trim();
        return;
      }
      if (line.length === 0 || line.startsWith('#')) {
        return;
      }
      if (!line.toLowerCase().startsWith('http://') && !line.toLowerCase().startsWith('https://')) {
        return;
      }
      if (seenUrls.has(line)) {
        return;
      }
      seenUrls.add(line);
      let fallbackName = line.substring(line.lastIndexOf('/') + 1);
      const queryIndex = fallbackName.indexOf('?');
      if (queryIndex >= 0) {
        fallbackName = fallbackName.substring(0, queryIndex);
      }
      result.push({
        name: pendingName.length > 0 ? pendingName : fallbackName,
        url: line,
        iconUrl: pendingLogo
      });
      pendingName = '';
      pendingLogo = '';
    });
    return result;
  }

  build(stations: Station[]): string {
    const lines: string[] = ['#EXTM3U'];
    stations.forEach((station: Station) => {
      const logo = station.iconUrl.length > 0 ? ` tvg-logo="${station.iconUrl}"` : '';
      lines.push(`#EXTINF:-1${logo},${station.name}`);
      lines.push(station.streamUrl);
    });
    return `${lines.join('\n')}\n`;
  }
}
