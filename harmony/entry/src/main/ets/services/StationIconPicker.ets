import { fileIo } from '@kit.CoreFileKit';
import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { encodeImageData } from '../utils/Base64Tools';

const MAX_ICON_BYTES: number = 2 * 1024 * 1024;

export class StationIconPicker {
  async pick(): Promise<string> {
    const options = new photoAccessHelper.PhotoSelectOptions();
    options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
    options.maxSelectNumber = 1;
    const picker = new photoAccessHelper.PhotoViewPicker();
    const result = await picker.select(options);
    if (result.photoUris.length === 0) {
      return '';
    }
    const uri = result.photoUris[0];
    const file = fileIo.openSync(uri, fileIo.OpenMode.READ_ONLY);
    try {
      const stat = fileIo.statSync(uri);
      if (stat.size <= 0 || stat.size > MAX_ICON_BYTES) {
        throw new Error('台标图片需小于 2 MB');
      }
      const buffer = new ArrayBuffer(stat.size);
      const readLength = fileIo.readSync(file.fd, buffer);
      if (readLength !== stat.size) {
        throw new Error('台标图片读取不完整');
      }
      const lowerUri = uri.toLowerCase();
      let mimeType = 'image/jpeg';
      if (lowerUri.endsWith('.png')) {
        mimeType = 'image/png';
      } else if (lowerUri.endsWith('.webp')) {
        mimeType = 'image/webp';
      } else if (lowerUri.endsWith('.gif')) {
        mimeType = 'image/gif';
      }
      return encodeImageData(buffer, mimeType);
    } catch (error) {
      const businessError = error as BusinessError;
      throw new Error(`选择台标失败：${businessError.message ?? '未知错误'}`);
    } finally {
      fileIo.closeSync(file);
    }
  }
}
