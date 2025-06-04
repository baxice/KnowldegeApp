<template>
  <div class="login-container">
    <div class="login-card">
      <h2>登录</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="el-icon-user"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" placeholder="密码" show-password prefix-icon="el-icon-lock"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" class="login-button">登录</el-button>
        </el-form-item>
        <div class="login-links">
          <router-link to="/register">没有账号？立即注册</router-link>
        </div>
      </el-form>
      <el-alert v-if="message" :title="message" type="error" show-icon />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const loginFormRef = ref(null)
const router = useRouter()
const userStore = useUserStore()

const loginForm = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const message = ref('')

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度必须在4-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  message.value = ''
  
  try {
    await userStore.login(loginForm)
    ElMessage({
      message: '登录成功',
      type: 'success'
    })
    router.push('/')
  } catch (error) {
    console.error('Login error:', error)
    message.value = error.response?.data?.message || '登录失败，请检查您的用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-dark, #181920);
}

.login-card {
  width: 400px;
  padding: 30px;
  background-color: var(--surface-dark, #242730);
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-dark, #f2f2f0);
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.login-links {
  margin-top: 20px;
  text-align: center;
}

.login-links a {
  color: var(--color-blue, #66d9ff);
  text-decoration: none;
  font-size: 14px;
}

.login-links a:hover {
  text-decoration: underline;
}

.login-links span {
  color: var(--text-muted, #888);
  font-size: 14px;
}
</style> 