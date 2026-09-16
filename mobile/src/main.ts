import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { applyTheme, getTheme } from './data/theme'
import { armAllReminders } from './data/reminders'
import { watchPermissionReArm } from './data/autoRearm'
import './styles/app.css'

applyTheme(getTheme())
// 冷启动就把库里的提醒意图重新排进系统；只查权限不弹权限，避免一进来就被弹窗。
// 权限晚到（首装先拒绝、之后才在系统设置里打开）由回到前台时的自动补排兜住。
void armAllReminders()
watchPermissionReArm()
createApp(App).use(router).mount('#app')
