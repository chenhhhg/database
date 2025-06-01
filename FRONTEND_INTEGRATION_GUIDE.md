# 前端集成指南 - TPC-H数据库管理系统

## 📱 前端架构建议

### 技术栈推荐
- **框架**: Vue 3 / React 18 / Angular 15+
- **UI组件库**: Element Plus / Ant Design / Material-UI
- **状态管理**: Vuex/Pinia / Redux / NgRx
- **路由**: Vue Router / React Router / Angular Router
- **HTTP客户端**: Axios
- **图表库**: ECharts / Chart.js
- **表格组件**: 支持虚拟滚动的高性能表格组件

---

## 🎯 菜单结构实现

### 菜单配置文件 (menu-config.js)

```javascript
export const menuConfig = [
  {
    id: 'dashboard',
    title: '首页',
    icon: 'el-icon-house',
    path: '/dashboard',
    component: 'Dashboard',
    meta: { requiresAuth: true, roles: ['user', 'admin'] }
  },
  {
    id: 'user-management',
    title: '用户管理',
    icon: 'el-icon-user',
    path: '/user',
    meta: { requiresAuth: true, roles: ['admin'] },
    children: [
      {
        id: 'user-list',
        title: '用户列表',
        path: '/user/list',
        component: 'UserList'
      },
      {
        id: 'user-audit',
        title: '用户审核',
        path: '/user/audit',
        component: 'UserAudit'
      },
      {
        id: 'role-management',
        title: '角色管理',
        path: '/user/roles',
        component: 'RoleManagement'
      }
    ]
  },
  {
    id: 'data-management',
    title: '数据管理',
    icon: 'el-icon-document',
    path: '/data',
    meta: { requiresAuth: true, roles: ['user', 'admin'] },
    children: [
      {
        id: 'tpc-generation',
        title: 'TPC数据生成',
        path: '/data/tpc-generation',
        component: 'TPCGeneration'
      },
      {
        id: 'data-import-export',
        title: '数据导入导出',
        path: '/data/import-export',
        component: 'DataImportExport'
      },
      {
        id: 'foreign-key',
        title: '外键管理',
        path: '/data/foreign-key',
        component: 'ForeignKeyManagement'
      },
      {
        id: 'path-management',
        title: '数据路径管理',
        path: '/data/paths',
        component: 'PathManagement'
      }
    ]
  },
  {
    id: 'data-query',
    title: '数据查询',
    icon: 'el-icon-search',
    path: '/query',
    meta: { requiresAuth: true, roles: ['user', 'admin'] },
    children: [
      {
        id: 'customer-query',
        title: '客户信息查询',
        path: '/query/customer',
        component: 'CustomerQuery'
      },
      {
        id: 'basic-query',
        title: '基础数据查询',
        path: '/query/basic',
        component: 'BasicQuery'
      },
      {
        id: 'custom-query',
        title: '自定义查询',
        path: '/query/custom',
        component: 'CustomQuery'
      }
    ]
  },
  {
    id: 'data-analysis',
    title: '数据分析',
    icon: 'el-icon-data-analysis',
    path: '/analysis',
    meta: { requiresAuth: true, roles: ['user', 'admin'] },
    children: [
      {
        id: 'tpch-queries',
        title: 'TPC-H标准查询',
        path: '/analysis/tpch',
        component: 'TPCHQueries'
      },
      {
        id: 'execution-plan',
        title: '执行计划分析',
        path: '/analysis/execution-plan',
        component: 'ExecutionPlanAnalysis'
      },
      {
        id: 'performance-monitor',
        title: '性能监控',
        path: '/analysis/performance',
        component: 'PerformanceMonitor'
      }
    ]
  },
  {
    id: 'system-settings',
    title: '系统设置',
    icon: 'el-icon-setting',
    path: '/settings',
    meta: { requiresAuth: true, roles: ['admin'] },
    children: [
      {
        id: 'database-config',
        title: '数据库配置',
        path: '/settings/database',
        component: 'DatabaseConfig'
      },
      {
        id: 'system-params',
        title: '系统参数',
        path: '/settings/params',
        component: 'SystemParams'
      },
      {
        id: 'log-management',
        title: '日志管理',
        path: '/settings/logs',
        component: 'LogManagement'
      }
    ]
  }
];
```

---

## 🔐 认证模块实现

### 1. 认证状态管理 (store/auth.js)

```javascript
// Vue 3 + Pinia 示例
import { defineStore } from 'pinia';
import { api } from '@/utils/api';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    isAuthenticated: false,
    loading: false
  }),

  getters: {
    isAdmin: (state) => state.user?.role === 3,
    isUser: (state) => state.user?.role >= 2,
    userRole: (state) => {
      const roleMap = { 1: '待审核', 2: '普通用户', 3: '管理员' };
      return roleMap[state.user?.role] || '未知';
    }
  },

  actions: {
    async login(credentials) {
      this.loading = true;
      try {
        const response = await api.post('/customer/login', credentials);
        
        if (response.data.code === 200) {
          const { token, username, userId, role, expiresIn } = response.data.data;
          
          this.token = token;
          this.user = { username, userId, role };
          this.isAuthenticated = true;
          
          // 保存到本地存储
          localStorage.setItem('token', token);
          localStorage.setItem('user', JSON.stringify(this.user));
          
          // 设置token过期时间
          setTimeout(() => {
            this.logout();
          }, expiresIn);
          
          return { success: true };
        } else {
          return { success: false, message: response.data.msg };
        }
      } catch (error) {
        return { success: false, message: error.message };
      } finally {
        this.loading = false;
      }
    },

    async register(userData) {
      this.loading = true;
      try {
        const response = await api.post('/customer/register', userData);
        return {
          success: response.data.code === 200,
          message: response.data.msg
        };
      } catch (error) {
        return { success: false, message: error.message };
      } finally {
        this.loading = false;
      }
    },

    logout() {
      this.token = null;
      this.user = null;
      this.isAuthenticated = false;
      
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      
      // 重定向到登录页
      window.location.href = '/login';
    },

    initAuth() {
      const token = localStorage.getItem('token');
      const user = localStorage.getItem('user');
      
      if (token && user) {
        this.token = token;
        this.user = JSON.parse(user);
        this.isAuthenticated = true;
      }
    }
  }
});
```

### 2. API请求拦截器 (utils/api.js)

```javascript
import axios from 'axios';
import { useAuthStore } from '@/store/auth';
import { ElMessage } from 'element-plus';

// 创建axios实例
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8001',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    
    // 添加token到请求头
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const authStore = useAuthStore();
    
    // 处理401未授权错误
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      authStore.logout();
      return;
    }
    
    // 处理403权限不足错误
    if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法访问');
      return;
    }
    
    // 处理其他错误
    const message = error.response?.data?.msg || error.message || '请求失败';
    ElMessage.error(message);
    
    return Promise.reject(error);
  }
);

export { api };
```

### 3. 路由守卫 (router/index.js)

```javascript
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/auth';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true, roles: ['user', 'admin'] }
  },
  // ... 其他路由
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      next('/login');
      return;
    }
    
    // 检查角色权限
    if (to.meta.roles) {
      const userRole = authStore.user?.role;
      const hasPermission = to.meta.roles.some(role => {
        if (role === 'admin') return userRole === 3;
        if (role === 'user') return userRole >= 2;
        return false;
      });
      
      if (!hasPermission) {
        ElMessage.error('权限不足，无法访问该页面');
        next('/dashboard');
        return;
      }
    }
  }
  
  next();
});

export default router;
```

---

## 📊 核心页面组件示例

### 1. 登录页面 (views/Login.vue)

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>TPC-H数据库管理系统</h2>
      
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="用户名"
                prefix-icon="el-icon-user"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="密码"
                prefix-icon="el-icon-lock"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                style="width: 100%"
                :loading="authStore.loading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input 
                v-model="registerForm.username" 
                placeholder="用户名"
                prefix-icon="el-icon-user"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="密码"
                prefix-icon="el-icon-lock"
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                placeholder="确认密码"
                prefix-icon="el-icon-lock"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="success" 
                style="width: 100%"
                :loading="authStore.loading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { ElMessage } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('login');
const loginFormRef = ref();
const registerFormRef = ref();

const loginForm = reactive({
  username: '',
  password: ''
});

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
});

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const handleLogin = async () => {
  const valid = await loginFormRef.value.validate();
  if (!valid) return;
  
  const result = await authStore.login(loginForm);
  if (result.success) {
    ElMessage.success('登录成功');
    router.push('/dashboard');
  } else {
    ElMessage.error(result.message);
  }
};

const handleRegister = async () => {
  const valid = await registerFormRef.value.validate();
  if (!valid) return;
  
  const result = await authStore.register({
    username: registerForm.username,
    password: registerForm.password
  });
  
  if (result.success) {
    ElMessage.success('注册成功，请等待管理员审核');
    activeTab.value = 'login';
    registerForm.username = '';
    registerForm.password = '';
    registerForm.confirmPassword = '';
  } else {
    ElMessage.error(result.message);
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-tabs {
  margin-top: 20px;
}
</style>
```

### 2. 客户查询页面 (views/CustomerQuery.vue)

```vue
<template>
  <div class="customer-query">
    <el-card class="search-card">
      <template #header>
        <span>客户信息查询</span>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="searchForm" label-width="120px" inline>
        <el-form-item label="客户ID">
          <el-input v-model="searchForm.customerId" placeholder="请输入客户ID" />
        </el-form-item>
        
        <el-form-item label="客户姓名">
          <el-input v-model="searchForm.customerName" placeholder="请输入客户姓名" />
        </el-form-item>
        
        <el-form-item label="国家">
          <el-select v-model="searchForm.nationKey" placeholder="请选择国家">
            <el-option
              v-for="nation in nations"
              :key="nation.key"
              :label="nation.name"
              :value="nation.key"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="市场细分">
          <el-select v-model="searchForm.marketSegment" placeholder="请选择市场细分">
            <el-option label="建筑" value="BUILDING" />
            <el-option label="汽车" value="AUTOMOBILE" />
            <el-option label="机械" value="MACHINERY" />
            <el-option label="家具" value="FURNITURE" />
            <el-option label="家庭" value="HOUSEHOLD" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="账户余额">
          <el-input-number v-model="searchForm.balanceMin" placeholder="最小值" />
          <span style="margin: 0 10px;">-</span>
          <el-input-number v-model="searchForm.balanceMax" placeholder="最大值" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 结果表格 -->
    <el-card class="result-card">
      <template #header>
        <div class="result-header">
          <span>查询结果 ({{ pagination.total }} 条)</span>
          <span v-if="executionTime" class="execution-time">
            执行时间: {{ executionTime }}ms
          </span>
        </div>
      </template>
      
      <el-table
        :data="customers"
        v-loading="loading"
        style="width: 100%"
        height="500"
      >
        <el-table-column prop="cCustkey" label="客户ID" width="100" />
        <el-table-column prop="cName" label="客户姓名" width="150" />
        <el-table-column prop="cAddress" label="地址" width="200" show-overflow-tooltip />
        <el-table-column prop="cNationkey" label="国家代码" width="100" />
        <el-table-column prop="cPhone" label="电话" width="150" />
        <el-table-column prop="cAcctbal" label="账户余额" width="120">
          <template #default="{ row }">
            <span :class="{ 'negative-balance': row.cAcctbal < 0 }">
              {{ formatCurrency(row.cAcctbal) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="cMktsegment" label="市场细分" width="120" />
        <el-table-column prop="cComment" label="备注" show-overflow-tooltip />
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { api } from '@/utils/api';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const customers = ref([]);
const executionTime = ref(0);
const nations = ref([]);

const searchForm = reactive({
  customerId: '',
  customerName: '',
  nationKey: '',
  marketSegment: '',
  balanceMin: null,
  balanceMax: null
});

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
});

// 搜索客户
const handleSearch = async () => {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      size: pagination.size,
      sortBy: 'C_CUSTKEY',
      sortOrder: 'ASC'
    };
    
    // 清除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key];
      }
    });
    
    const response = await api.post('/customer/query', params);
    
    if (response.data.code === 200) {
      const data = response.data.data;
      customers.value = data.customers;
      pagination.total = data.total;
      executionTime.value = data.executionTime;
      
      ElMessage.success(`查询成功，共找到 ${data.total} 条记录`);
    }
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

// 重置搜索
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = key.includes('balance') ? null : '';
  });
  pagination.page = 1;
  customers.value = [];
  executionTime.value = 0;
};

// 分页处理
const handleSizeChange = (newSize) => {
  pagination.size = newSize;
  pagination.page = 1;
  handleSearch();
};

const handleCurrentChange = (newPage) => {
  pagination.page = newPage;
  handleSearch();
};

// 格式化货币
const formatCurrency = (value) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value);
};

// 初始化
onMounted(() => {
  // 可以加载国家列表等初始数据
});
</script>

<style scoped>
.customer-query {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.execution-time {
  color: #67c23a;
  font-size: 12px;
}

.negative-balance {
  color: #f56c6c;
}
</style>
```

---

## 🔧 工具类和通用组件

### 1. 通用响应处理 (utils/response.js)

```javascript
import { ElMessage, ElNotification } from 'element-plus';

export class ResponseHandler {
  static success(response, showMessage = true) {
    if (response.data.code === 200) {
      if (showMessage && response.data.msg) {
        ElMessage.success(response.data.msg);
      }
      return response.data.data;
    } else {
      if (showMessage) {
        ElMessage.error(response.data.msg || '操作失败');
      }
      throw new Error(response.data.msg || '操作失败');
    }
  }
  
  static error(error, showMessage = true) {
    const message = error.response?.data?.msg || error.message || '操作失败';
    if (showMessage) {
      ElMessage.error(message);
    }
    throw error;
  }
  
  static notification(type, title, message) {
    ElNotification({
      type,
      title,
      message,
      duration: 3000
    });
  }
}
```

### 2. 执行时间显示组件 (components/ExecutionTime.vue)

```vue
<template>
  <div class="execution-time" v-if="time">
    <el-icon><Clock /></el-icon>
    <span>执行时间: {{ formatTime(time) }}</span>
  </div>
</template>

<script setup>
import { Clock } from '@element-plus/icons-vue';

const props = defineProps({
  time: {
    type: Number,
    default: 0
  }
});

const formatTime = (ms) => {
  if (ms < 1000) {
    return `${ms}ms`;
  } else if (ms < 60000) {
    return `${(ms / 1000).toFixed(2)}s`;
  } else {
    const minutes = Math.floor(ms / 60000);
    const seconds = ((ms % 60000) / 1000).toFixed(2);
    return `${minutes}m ${seconds}s`;
  }
};
</script>

<style scoped>
.execution-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #67c23a;
  font-size: 12px;
}
</style>
```

---

## 📋 最佳实践建议

### 1. 状态管理
- 使用统一的状态管理库管理全局状态
- 区分本地状态和全局状态
- 合理使用缓存减少不必要的API调用

### 2. 错误处理
- 统一的错误处理机制
- 友好的错误提示信息
- 网络错误的重试机制

### 3. 性能优化
- 虚拟滚动处理大数据量
- 防抖处理搜索输入
- 图片懒加载
- 组件懒加载

### 4. 用户体验
- 加载状态指示
- 操作确认对话框
- 快捷键支持
- 响应式设计

### 5. 安全考虑
- XSS防护
- CSRF防护
- 敏感信息不在前端存储
- 路由权限控制

这个前端集成指南提供了完整的实现方案，可以帮助前端开发者快速构建一个功能完善的TPC-H数据库管理系统前端界面！ 