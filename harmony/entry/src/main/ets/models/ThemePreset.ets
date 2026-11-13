export class ThemePreset {
  id: string;
  title: string;
  subtitle: string;
  primary: string;
  background: string;
  surface: string;
  surfaceVariant: string;
  textPrimary: string;
  textSecondary: string;
  divider: string;
  nowPlaying: string;
  isDark: boolean;

  constructor(
    id: string,
    title: string,
    subtitle: string,
    primary: string,
    background: string,
    surface: string,
    surfaceVariant: string,
    textPrimary: string,
    textSecondary: string,
    divider: string,
    nowPlaying: string,
    isDark: boolean
  ) {
    this.id = id;
    this.title = title;
    this.subtitle = subtitle;
    this.primary = primary;
    this.background = background;
    this.surface = surface;
    this.surfaceVariant = surfaceVariant;
    this.textPrimary = textPrimary;
    this.textSecondary = textSecondary;
    this.divider = divider;
    this.nowPlaying = nowPlaying;
    this.isDark = isDark;
  }
}

export const THEME_PRESETS: ThemePreset[] = [
  new ThemePreset(
    'lunar_light', '钛空雅素', '清透典雅素白瓷与晨光深空天青',
    '#0284C7', '#F8FAFC', '#FFFFFF', '#F1F5F9', '#0F172A', '#475569', '#E2E8F0', '#E7EEF9', false
  ),
  new ThemePreset(
    'porcelain_jade', '温润白玉', '温润白瓷与澄澈青玉',
    '#059669', '#F9FAF8', '#FFFFFF', '#F0F4F1', '#132A1C', '#4B6354', '#DCE7DF', '#E1F3EA', false
  ),
  new ThemePreset(
    'sunlit_amber', '晨曦暖杏', '晨光暖金与柔润杏色',
    '#D97706', '#FFFDF9', '#FFFFFF', '#FBF4EB', '#291B0D', '#785635', '#EDE0D1', '#FFF0D6', false
  ),
  new ThemePreset(
    'cherry_blossom', '春樱落粉', '落樱花瓣与柔雾淡粉',
    '#E11D48', '#FDF8F9', '#FFFFFF', '#FCEEF1', '#33131D', '#7C4355', '#F4D4DC', '#FCE6EC', false
  ),
  new ThemePreset(
    'frosted_silver', '霜银极简', '冷调金属铝银与深空岩灰',
    '#2563EB', '#F1F5F9', '#FFFFFF', '#E2E8F0', '#0F172A', '#475569', '#CBD5E1', '#E3EBFA', false
  ),
  new ThemePreset(
    'matcha_mist', '薄雾青茶', '清晨山岚与冷泡初茶',
    '#16A34A', '#F4F9F5', '#FFFFFF', '#E8F3EA', '#142918', '#49674E', '#D4E6D7', '#E1F3E5', false
  ),
  new ThemePreset(
    'soda_obsidian', '黑曜极光', '深空曜黑与现代薄荷绿',
    '#10B981', '#090B11', '#11141E', '#181C29', '#F8FAFC', '#94A3B8', '#1E2638', '#18362F', true
  ),
  new ThemePreset(
    'vintage_amber', '胡桃琥珀', '低噪暖木与金琥珀微光',
    '#F59E0B', '#0C0A08', '#161310', '#221C16', '#FEF3C7', '#D97706', '#2A221C', '#342712', true
  ),
  new ThemePreset(
    'aurora_emerald', '静谧苍翠', '北欧冷杉与静谧翡翠碧波',
    '#10B981', '#07120F', '#0D1C18', '#142923', '#ECFDF5', '#6EE7B7', '#1B362F', '#10352B', true
  ),
  new ThemePreset(
    'cyber_neon', '未来霓虹', '电子罗兰紫与脉冲深蓝',
    '#8B5CF6', '#0B0914', '#131022', '#1B162E', '#FDF4FF', '#C084FC', '#262040', '#25183D', true
  ),
  new ThemePreset(
    'sunset_glow', '落日余晖', '加州暮色珊瑚暖橙与微醺晚霞',
    '#FB923C', '#12090F', '#1D1019', '#2A1724', '#FFF1F2', '#FDA4AF', '#382030', '#3B1B23', true
  )
];

export function findTheme(id: string): ThemePreset {
  return THEME_PRESETS.find((theme: ThemePreset) => theme.id === id) ?? THEME_PRESETS[0];
}
