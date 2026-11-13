import { common } from '@kit.AbilityKit';
import { http } from '@kit.NetworkKit';
import { updateManager } from '@kit.AppGalleryKit';
import { BusinessError } from '@kit.BasicServicesKit';

const GITHUB_RELEASES_URL: string =
  'https://api.github.com/repos/zzg341125/QY-FM/releases?per_page=30';
const CURRENT_VERSION: string = '1.0.0';

export interface NativeUpdateInfo {
  version: string;
  title: string;
  notes: string;
  pageUrl: string;
}

interface GitHubRelease {
  tag_name: string;
  name: string;
  body: string;
  html_url: string;
  prerelease: boolean;
  draft: boolean;
}

class GitHubHeaders {
  'Accept': string = 'application/vnd.github+json';
  'User-Agent': string = 'QYFM-HarmonyOS';
}

function parseVersion(value: string): number[] {
  return value.replace(/^harmony-v/i, '').replace(/^v/i, '').split('.').map((part: string) => {
    const match = part.match(/^\d+/);
    return match === null ? 0 : Number(match[0]);
  });
}

function isNewer(remote: string, current: string): boolean {
  const remoteParts = parseVersion(remote);
  const currentParts = parseVersion(current);
  const count = Math.max(remoteParts.length, currentParts.length);
  for (let index = 0; index < count; index++) {
    const remotePart = remoteParts[index] ?? 0;
    const currentPart = currentParts[index] ?? 0;
    if (remotePart > currentPart) {
      return true;
    }
    if (remotePart < currentPart) {
      return false;
    }
  }
  return false;
}

export class AppUpdateService {
  async checkNativeRelease(): Promise<NativeUpdateInfo | undefined> {
    const request = http.createHttp();
    try {
      const response = await request.request(GITHUB_RELEASES_URL, {
        method: http.RequestMethod.GET,
        header: new GitHubHeaders(),
        connectTimeout: 15000,
        readTimeout: 30000,
        expectDataType: http.HttpDataType.STRING,
        usingCache: false
      });
      if (response.responseCode !== 200 || typeof response.result !== 'string') {
        return undefined;
      }
      const releases = JSON.parse(response.result) as GitHubRelease[];
      for (const release of releases) {
        if (release.draft || release.prerelease || !release.tag_name.toLowerCase().startsWith('harmony-v')) {
          continue;
        }
        if (isNewer(release.tag_name, CURRENT_VERSION)) {
          return {
            version: release.tag_name,
            title: release.name.length > 0 ? release.name : release.tag_name,
            notes: release.body,
            pageUrl: release.html_url
          };
        }
      }
      return undefined;
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`检查 GitHub 版本失败: ${businessError.message}`);
      return undefined;
    } finally {
      request.destroy();
    }
  }

  async checkAppGallery(context: common.UIAbilityContext): Promise<boolean> {
    try {
      const result = await updateManager.checkAppUpdate(context);
      if (result.updateAvailable === updateManager.UpdateAvailableCode.LATER_VERSION_EXIST) {
        await updateManager.showUpdateDialog(context);
        return true;
      }
    } catch (error) {
      const businessError = error as BusinessError;
      console.info(`AppGallery 更新检查不可用: ${businessError.code}`);
    }
    return false;
  }

  async openReleasePage(context: common.UIAbilityContext, url: string): Promise<void> {
    await context.openLink(url);
  }
}
