import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { applyTheme, getTheme } from './data/theme'
import { armAllReminders } from './data/reminders'
import { watchPermissionReArm } from './data/autoRearm'
import { runAutoBackup } from './data/autoBackup'
import './styles/app.css'

applyTheme(getTheme())
// 冷启动就把库里的提醒意图重新排进系统；只查权限不弹权限，避免一进来就被弹窗。
// 权限晚到（首装先拒绝、之后才在系统设置里打开）由回到前台时的自动补排兜住。
void armAllReminders()
watchPermissionReArm()
// 冷启动自动备份：当天一份、轮转保留 7 份，落在 App 专属外部目录。
// 不 await——它绝不抛错也不阻塞首屏，失败现场会记给「我的」页展示。
void runAutoBackup()
createApp(App).use(router).mount('#app')
