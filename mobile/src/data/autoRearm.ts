import { Capacitor } from '@capacitor/core'
import { App as CapApp } from '@capacitor/app'
import { notificationPermissionState } from './notifications'
import { armAllReminders, lastReminderReport, type ReminderReport } from './reminders'

/**
 * 权限晚到自动补排。
 *
 * 冷启动时通知权限往往还没给（Android 13+ 首装常态，而且启动流程刻意不弹权限窗），
 * armAllReminders 会整批 denied；等用户真正把权限打开再回到应用时，没有任何东西
 * 会替他重排——这就是 0.2.8 发布说明里「要去我的页手动点一次重新排入系统」的摩擦。
 * 这里监听回到前台：权限已给、但上一轮排入里出现过 denied（或从未排过）时，自动补排一遍。
 */
export function shouldReArm(report: ReminderReport | null, permission: 'granted' | 'denied' | 'unsupported'): boolean {
  if (permission !== 'granted') return false
  return !report || report.denied > 0
}

export function watchPermissionReArm(): void {
  if (!Capacitor.isNativePlatform()) return
  void CapApp.addListener('appStateChange', async (state) => {
    if (!state.isActive) return
    const permission = await notificationPermissionState()
    if (!shouldReArm(lastReminderReport(), permission)) return
    void armAllReminders()
  })
}
