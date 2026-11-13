import { common } from '@kit.AbilityKit';
import { fileIo, picker } from '@kit.CoreFileKit';
import { BusinessError } from '@kit.BasicServicesKit';

export class LocalFileService {
  async pickText(context: common.UIAbilityContext, suffixFilter: string): Promise<string> {
    const options = new picker.DocumentSelectOptions();
    options.maxSelectNumber = 1;
    options.fileSuffixFilters = [suffixFilter];
    const documentPicker = new picker.DocumentViewPicker(context);
    const uris = await documentPicker.select(options);
    if (uris.length === 0) {
      return '';
    }
    try {
      const file = fileIo.openSync(uris[0], fileIo.OpenMode.READ_ONLY);
      try {
        return fileIo.readTextSync(uris[0]);
      } finally {
        fileIo.closeSync(file);
      }
    } catch (error) {
      const businessError = error as BusinessError;
      throw new Error(`读取文件失败：${businessError.message ?? '未知错误'}`);
    }
  }

  async saveText(context: common.UIAbilityContext, fileName: string, content: string): Promise<void> {
    const options = new picker.DocumentSaveOptions();
    options.newFileNames = [fileName];
    const documentPicker = new picker.DocumentViewPicker(context);
    const uris = await documentPicker.save(options);
    if (uris.length === 0) {
      return;
    }
    try {
      const file = fileIo.openSync(
        uris[0],
        fileIo.OpenMode.WRITE_ONLY | fileIo.OpenMode.CREATE | fileIo.OpenMode.TRUNC
      );
      try {
        fileIo.writeSync(file.fd, content);
      } finally {
        fileIo.closeSync(file);
      }
    } catch (error) {
      const businessError = error as BusinessError;
      throw new Error(`保存文件失败：${businessError.message ?? '未知错误'}`);
    }
  }
}
