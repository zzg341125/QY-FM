import { util } from '@kit.ArkTS';

const encoder = new util.TextEncoder();
const base64 = new util.Base64Helper();

export function encodeBasicCredentials(user: string, password: string): string {
  return base64.encodeToStringSync(encoder.encodeInto(`${user}:${password}`));
}

export function encodeImageData(data: ArrayBuffer, mimeType: string): string {
  return `data:${mimeType};base64,${base64.encodeToStringSync(new Uint8Array(data))}`;
}
