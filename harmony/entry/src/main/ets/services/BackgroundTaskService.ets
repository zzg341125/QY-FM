import { backgroundTaskManager } from '@kit.BackgroundTasksKit';
import { bundleManager, common, WantAgent, wantAgent } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';

export class BackgroundTaskService {
  private continuousTaskId: number = -1;

  async start(context: common.UIAbilityContext): Promise<void> {
    if (this.continuousTaskId >= 0) {
      return;
    }
    const bundleInfo = bundleManager.getBundleInfoForSelfSync(
      bundleManager.BundleFlag.GET_BUNDLE_INFO_WITH_APPLICATION
    );
    const wantAgentInfo: wantAgent.WantAgentInfo = {
      wants: [
        {
          bundleName: bundleInfo.name,
          abilityName: 'EntryAbility'
        }
      ],
      actionType: wantAgent.OperationType.START_ABILITY,
      requestCode: 4101,
      actionFlags: [wantAgent.WantAgentFlags.UPDATE_PRESENT_FLAG]
    };
    const wantAgentObj: WantAgent = await wantAgent.getWantAgent(wantAgentInfo);
    const request = new backgroundTaskManager.ContinuousTaskRequest();
    request.backgroundTaskModes = [
      backgroundTaskManager.BackgroundTaskMode.MODE_AUDIO_PLAYBACK
    ];
    request.backgroundTaskSubmodes = [
      backgroundTaskManager.BackgroundTaskSubmode.SUBMODE_NORMAL_NOTIFICATION
    ];
    request.wantAgent = wantAgentObj;
    request.combinedTaskNotification = true;
    try {
      const result = await backgroundTaskManager.startBackgroundRunning(context, request);
      this.continuousTaskId = result.continuousTaskId ?? -1;
      console.info(`后台播放任务已启动: ${this.continuousTaskId}`);
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`后台播放任务启动失败: ${businessError.code} ${businessError.message}`);
    }
  }

  async stop(context: common.UIAbilityContext): Promise<void> {
    if (this.continuousTaskId < 0) {
      return;
    }
    const taskId = this.continuousTaskId;
    this.continuousTaskId = -1;
    try {
      backgroundTaskManager.stopBackgroundRunning(context, taskId);
    } catch (error) {
      const businessError = error as BusinessError;
      console.error(`后台播放任务停止失败: ${businessError.code} ${businessError.message}`);
    }
  }
}
