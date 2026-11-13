import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { encodeBasicCredentials } from '../utils/Base64Tools';

const USER_AGENT: string = 'QYFM-HarmonyOS/1.0.0';

export interface WebDavConfig {
  baseUrl: string;
  user: string;
  password: string;
  fileName: string;
}

class WebDavHeaders {
  'Authorization'?: string;
  'Content-Type': string;
  'User-Agent': string;
  'Overwrite': string;

  constructor(contentType: string, user: string, password: string) {
    this['Content-Type'] = contentType;
    this['User-Agent'] = USER_AGENT;
    this['Overwrite'] = 'T';
    if (user.length > 0) {
      this['Authorization'] = `Basic ${encodeBasicCredentials(user, password)}`;
    }
  }
}

function normalizeFileName(fileName: string): string {
  const trimmed = fileName.trim().replaceAll('\\', '/');
  if (trimmed.length === 0 || trimmed.includes('..') || trimmed.startsWith('/')) {
    throw new Error('WebDAV 文件名不能为空，不能以 / 开头，也不能包含 ..');
  }
  return trimmed;
}

export function buildBackupUrl(config: WebDavConfig): string {
  const base = config.baseUrl.trim().replaceAll('\r', '').replaceAll('\n', '');
  if (base.length === 0) {
    throw new Error('请先填写 WebDAV 地址');
  }
  if (base.toLowerCase().endsWith('.json')) {
    return base;
  }
  const fileName = normalizeFileName(config.fileName);
  return `${base.replace(/\/+$/, '')}/${fileName}`;
}

function buildHeaders(config: WebDavConfig, contentType: string): WebDavHeaders {
  return new WebDavHeaders(contentType, config.user, config.password);
}

function describeHttpFailure(code: number, prefix: string): Error {
  let hint = '';
  if (code === 401 || code === 403) {
    hint = '（账号或密码错误，或账号没有读写权限）';
  } else if (code === 404) {
    hint = '（备份文件或目录不存在）';
  } else if (code === 409) {
    hint = '（上级目录不存在或路径冲突）';
  } else if (code === 405) {
    hint = '（目标不是可写的 WebDAV 地址）';
  }
  return new Error(`${prefix}：HTTP ${code}${hint}`);
}

export class WebDavBackupService {
  async testConnection(config: WebDavConfig): Promise<void> {
    const request = http.createHttp();
    try {
      const response = await request.request(config.baseUrl.trim(), {
        method: http.RequestMethod.OPTIONS,
        header: buildHeaders(config, 'application/json'),
        connectTimeout: 20000,
        readTimeout: 30000,
        expectDataType: http.HttpDataType.STRING
      });
      if (response.responseCode < 200 || response.responseCode >= 400) {
        throw describeHttpFailure(response.responseCode, 'WebDAV 连接失败');
      }
    } finally {
      request.destroy();
    }
  }

  async uploadBackup(config: WebDavConfig, json: string): Promise<void> {
    const request = http.createHttp();
    try {
      const response = await request.request(buildBackupUrl(config), {
        method: http.RequestMethod.PUT,
        header: buildHeaders(config, 'application/json; charset=utf-8'),
        extraData: json,
        connectTimeout: 20000,
        readTimeout: 120000,
        maxLimit: 50 * 1024 * 1024,
        expectDataType: http.HttpDataType.STRING
      });
      if (response.responseCode !== 200 && response.responseCode !== 201 && response.responseCode !== 204) {
        throw describeHttpFailure(response.responseCode, '云端备份失败');
      }
    } catch (error) {
      const businessError = error as BusinessError;
      throw new Error(`WebDAV 上传失败：${businessError.message ?? '未知错误'}`);
    } finally {
      request.destroy();
    }
  }

  async downloadBackup(config: WebDavConfig): Promise<string> {
    const request = http.createHttp();
    try {
      const response = await request.request(buildBackupUrl(config), {
        method: http.RequestMethod.GET,
        header: buildHeaders(config, 'application/json'),
        connectTimeout: 20000,
        readTimeout: 120000,
        maxLimit: 50 * 1024 * 1024,
        expectDataType: http.HttpDataType.STRING
      });
      if (response.responseCode !== 200) {
        throw describeHttpFailure(response.responseCode, '云端恢复失败');
      }
      if (typeof response.result !== 'string') {
        throw new Error('WebDAV 返回的备份内容不是文本');
      }
      return response.result;
    } catch (error) {
      const businessError = error as BusinessError;
      throw new Error(`WebDAV 下载失败：${businessError.message ?? '未知错误'}`);
    } finally {
      request.destroy();
    }
  }
}
