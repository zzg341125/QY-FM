import { audio } from '@kit.AudioKit';
import { BusinessError } from '@kit.BasicServicesKit';

/**
 * HarmonyOS 会在蓝牙断开约 1 分钟后结束空闲的蓝牙长时任务。
 * 等待重连时保持一条极低开销的静音音频流，使音频长时任务持续有效，
 * 从而能接收 120 分钟内的同设备重连事件。静音流不会请求可见媒体控制。
 */
export class SilentAudioKeepAlive {
  private renderer: audio.AudioRenderer | undefined = undefined;

  async start(): Promise<void> {
    if (this.renderer !== undefined) {
      return;
    }
    const options: audio.AudioRendererOptions = {
      streamInfo: {
        samplingRate: audio.AudioSamplingRate.SAMPLE_RATE_8000,
        channels: audio.AudioChannel.CHANNEL_1,
        sampleFormat: audio.AudioSampleFormat.SAMPLE_FORMAT_S16LE,
        encodingType: audio.AudioEncodingType.ENCODING_TYPE_RAW
      },
      rendererInfo: {
        usage: audio.StreamUsage.STREAM_USAGE_MUSIC,
        rendererFlags: 0
      }
    };
    try {
      this.renderer = await audio.createAudioRenderer(options);
      this.renderer.on('writeData', (buffer: ArrayBuffer): audio.AudioDataCallbackResult => {
        new Uint8Array(buffer).fill(0);
        return audio.AudioDataCallbackResult.VALID;
      });
      await this.renderer.start();
      console.info('蓝牙重连等待静音保持器已启动');
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`静音保持器启动失败: ${businessError.code} ${businessError.message}`);
      this.renderer = undefined;
    }
  }

  async stop(): Promise<void> {
    const renderer = this.renderer;
    this.renderer = undefined;
    if (renderer === undefined) {
      return;
    }
    try {
      if (renderer.state === audio.AudioState.STATE_RUNNING || renderer.state === audio.AudioState.STATE_PAUSED) {
        await renderer.stop();
      }
      await renderer.release();
      console.info('蓝牙重连等待静音保持器已释放');
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`静音保持器释放失败: ${businessError.code} ${businessError.message}`);
    }
  }
}
