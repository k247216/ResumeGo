import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { applyTheme, getTheme } from './data/theme'
import { armAllReminders } from './data/reminders'
import './styles/app.css'

applyTheme(getTheme())
// 冷启动就把库里的提醒意图重新排进系统；只查权限不弹权限，避免一进来就被弹窗。
void armAllReminders()
createApp(App).use(router).mount('#app')
