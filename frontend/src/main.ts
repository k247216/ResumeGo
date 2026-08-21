import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

// 挂载前同步持久化主题，保证沉浸式路由（简历编辑器）与深链直达也遵循夜间模式。
document.body.dataset.theme = localStorage.getItem('resumego:theme') === 'dark' ? 'dark' : 'light'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
