<template>
  <div class="user-management">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="showAddUser" icon="el-icon-plus">添加用户</el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filters">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input 
            v-model="searchForm.username" 
            placeholder="搜索用户名" 
            clearable
            @change="searchUsers">
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-input 
            v-model="searchForm.email" 
            placeholder="搜索邮箱" 
            clearable
            @change="searchUsers">
            <template #prefix>
              <el-icon><message /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="searchUsers">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 用户列表 -->
    <div class="user-table">
      <el-table 
        :data="users" 
        style="width: 100%" 
        v-loading="loading"
        stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.active ? 'success' : 'danger'">
              {{ scope.row.active ? '活跃' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="viewUser(scope.row)">查看</el-button>
            <el-button size="small" type="primary" @click="editUser(scope.row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="scope.row.active ? 'danger' : 'success'"
              @click="toggleUserStatus(scope.row)">
              {{ scope.row.active ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </div>

    <!-- 用户详情对话框 -->
    <el-dialog 
      v-model="userDialog.visible" 
      :title="userDialog.title" 
      width="600px"
      :close-on-click-modal="false">
      <el-form 
        :model="userDialog.form" 
        :rules="userDialog.rules" 
        ref="userFormRef" 
        label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="userDialog.form.username" 
            :disabled="userDialog.mode === 'view'" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input 
            v-model="userDialog.form.email" 
            :disabled="userDialog.mode === 'view'" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="userDialog.mode === 'add'">
          <el-input 
            v-model="userDialog.form.password" 
            type="password" 
            show-password />
        </el-form-item>
        <el-form-item label="状态" v-if="userDialog.mode !== 'add'">
          <el-switch 
            v-model="userDialog.form.active" 
            :disabled="userDialog.mode === 'view'" />
        </el-form-item>
        <el-form-item label="注册时间" v-if="userDialog.mode !== 'add'">
          <el-input 
            :model-value="formatDate(userDialog.form.createdAt)" 
            disabled />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialog.visible = false">取消</el-button>
          <el-button 
            v-if="userDialog.mode !== 'view'" 
            type="primary" 
            @click="saveUser">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Message } from '@element-plus/icons-vue'

// 响应式数据
const loading = ref(false)
const users = ref([])
const userFormRef = ref(null)

// 搜索表单
const searchForm = reactive({
  username: '',
  email: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 用户对话框
const userDialog = reactive({
  visible: false,
  title: '',
  mode: 'view', // view, edit, add
  form: {
    id: null,
    username: '',
    email: '',
    password: '',
    active: true,
    createdAt: null
  },
  rules: {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 4, max: 50, message: '用户名长度在4-50个字符', trigger: 'blur' }
    ],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入有效邮箱', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
    ]
  }
})

// 方法
const fetchUsers = async () => {
  loading.value = true
  try {
    // 模拟API调用
    // 这里应该调用实际的用户管理API
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 模拟数据
    users.value = [
      {
        id: 1,
        username: 'admin',
        email: 'admin@example.com',
        active: true,
        createdAt: '2024-01-01T10:00:00'
      },
      {
        id: 2,
        username: 'user1',
        email: 'user1@example.com',
        active: true,
        createdAt: '2024-01-02T11:00:00'
      }
    ]
    pagination.total = 2
  } catch (error) {
    ElMessage.error('获取用户列表失败')
    console.error('获取用户失败:', error)
  } finally {
    loading.value = false
  }
}

const searchUsers = () => {
  pagination.page = 1
  fetchUsers()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.email = ''
  searchUsers()
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

const showAddUser = () => {
  userDialog.visible = true
  userDialog.title = '添加用户'
  userDialog.mode = 'add'
  userDialog.form = {
    id: null,
    username: '',
    email: '',
    password: '',
    active: true,
    createdAt: null
  }
}

const viewUser = (user) => {
  userDialog.visible = true
  userDialog.title = '查看用户'
  userDialog.mode = 'view'
  userDialog.form = { ...user }
}

const editUser = (user) => {
  userDialog.visible = true
  userDialog.title = '编辑用户'
  userDialog.mode = 'edit'
  userDialog.form = { ...user }
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    // 这里应该调用实际的保存API
    if (userDialog.mode === 'add') {
      ElMessage.success('用户添加成功')
    } else {
      ElMessage.success('用户更新成功')
    }
    
    userDialog.visible = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存用户失败:', error)
  }
}

const toggleUserStatus = async (user) => {
  const action = user.active ? '禁用' : '启用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${user.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里应该调用实际的API
    user.active = !user.active
    ElMessage.success(`用户${action}成功`)
  } catch {
    // 用户取消操作
  }
}

const handleSizeChange = (val) => {
  pagination.size = val
  fetchUsers()
}

const handleCurrentChange = (val) => {
  pagination.page = val
  fetchUsers()
}

// 生命周期
onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: var(--text-dark, #f2f2f0);
}

.filters {
  background: var(--surface-dark, #242730);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.user-table {
  background: var(--surface-dark, #242730);
  padding: 20px;
  border-radius: 8px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}

/* Element Plus 暗色主题适配 */
:deep(.el-table) {
  background-color: var(--surface-dark, #242730);
  color: var(--text-dark, #f2f2f0);
}

:deep(.el-table th) {
  background-color: var(--bg-dark, #181920);
  color: var(--text-dark, #f2f2f0);
}

:deep(.el-table td) {
  background-color: var(--surface-dark, #242730);
  color: var(--text-dark, #f2f2f0);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: var(--bg-dark, #181920);
}
</style> 