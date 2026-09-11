import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.resumego.zhida.mobile',
  appName: '职达',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
  plugins: {
    LocalNotifications: {
      // 不指定时插件退回 android.R.drawable.ic_dialog_info，状态栏里是个通用的「i」，认不出是职达的提醒。
      smallIcon: 'ic_stat_zhida',
      iconColor: '#168b68',
    },
  },
}

export default config
