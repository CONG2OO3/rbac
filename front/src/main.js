import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '@kousum/semi-ui-vue/dist/_base/base.css'
import './styles/index.css'
import './styles/theme-accent.css'
import { applyThemeColor, getThemeColor } from './utils/themeColor'

applyThemeColor(getThemeColor())

createApp(App).use(router).mount('#app')
