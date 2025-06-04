import { defineStore } from 'pinia'




export const useThemeStore = defineStore('theme', {
  state: () => ({
    isDarkTheme: true
  }),
  
  getters: {
    currentTheme: (state) => state.isDarkTheme ? 'dark' : 'light',
    themeClass: (state) => state.isDarkTheme ? 'theme-dark' : 'theme-light'
  },
  
  actions: {
    toggleTheme() {
      this.isDarkTheme = !this.isDarkTheme
      this.applyTheme()
      this.saveTheme()
    },
    
    setTheme(isDark) {
      this.isDarkTheme = isDark
      this.applyTheme()
      this.saveTheme()
    },
    
    applyTheme() {
      const html = document.documentElement
      
      // 移除所有主题类
      html.classList.remove('theme-dark', 'theme-light')
      
      // 添加当前主题类
      html.classList.add(this.themeClass)
      
      // 更新meta标签（用于移动端状态栏）
      this.updateMetaThemeColor()
    },
    
    updateMetaThemeColor() {
      let metaThemeColor = document.querySelector('meta[name="theme-color"]')
      if (!metaThemeColor) {
        metaThemeColor = document.createElement('meta')
        metaThemeColor.name = 'theme-color'
        document.head.appendChild(metaThemeColor)
      }
      
      // 设置状态栏颜色
      const themeColor = this.isDarkTheme 
        ? 'hsl(235, 15%, 15%)' // 暗色主题的surface-dark
        : 'hsl(235, 15%, 99%)' // 亮色主题的surface-light
      
      metaThemeColor.content = themeColor
    },
    
    saveTheme() {
      localStorage.setItem('theme', this.currentTheme)
    },
    
    loadTheme() {
      const savedTheme = localStorage.getItem('theme')
      if (savedTheme) {
        this.isDarkTheme = savedTheme === 'dark'
      } else {
        // 如果没有保存的主题，检查系统偏好
        this.isDarkTheme = this.getSystemThemePreference()
      }
    },
    
    getSystemThemePreference() {
      if (typeof window !== 'undefined' && window.matchMedia) {
        return window.matchMedia('(prefers-color-scheme: dark)').matches
      }
      return true // 默认暗色主题
    },
    
    initTheme() {
      this.loadTheme()
      this.applyTheme()
      
      // 监听系统主题变化
      if (typeof window !== 'undefined' && window.matchMedia) {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
        mediaQuery.addEventListener('change', (e) => {
          // 只有在用户没有手动设置主题时才跟随系统
          if (!localStorage.getItem('theme')) {
            this.setTheme(e.matches)
          }
        })
      }
    },
    
    resetToSystemTheme() {
      localStorage.removeItem('theme')
      this.isDarkTheme = this.getSystemThemePreference()
      this.applyTheme()
    }
  }
}) 