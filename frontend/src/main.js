import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './assets/styles/main.scss'
import { useUserStore } from './stores/user'

// 导入Obsidian主题样式
import '@/assets/styles/obsidian-theme.css'

// 导入FontAwesome图标（如果需要）
// import '@fortawesome/fontawesome-free/css/all.css'

const app = createApp(App)
const pinia = createPinia()

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 初始化用户信息
const userStore = useUserStore()
userStore.initUser().finally(() => {
  app.mount('#app')
}) 