import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { applyTheme, getTheme } from './data/theme'
import './styles/app.css'

applyTheme(getTheme())
createApp(App).use(router).mount('#app')
