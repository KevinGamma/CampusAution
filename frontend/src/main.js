import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/style.css'          // SDU brand overrides — must come after EP CSS
import router from './router/index.js'
import App from './App.vue'

createApp(App)
  .use(ElementPlus)
  .use(router)
  .mount('#app')
