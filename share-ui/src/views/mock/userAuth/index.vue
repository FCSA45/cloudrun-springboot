<template>
  <div class="mock-auth-page">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="form-card">
          <template #header>
            <div class="card-header">小程序用户模拟登录/注册</div>
          </template>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="账号注册" name="accountRegister">
              <el-form ref="accountRegisterFormRef" :model="forms.accountRegister" :rules="rules.accountRegister" label-width="96px">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="forms.accountRegister.username" placeholder="请输入用户名" maxlength="30" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                  <el-input v-model="forms.accountRegister.password" type="password" placeholder="至少 6 位" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="forms.accountRegister.confirmPassword" type="password" placeholder="再次输入密码" show-password />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="forms.accountRegister.nickname" placeholder="可选" maxlength="30" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="loading.accountRegister" @click="handleAccountRegister">注册并登录</el-button>
                  <el-button @click="resetAccountRegister">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="账号登录" name="accountLogin">
              <el-form ref="accountLoginFormRef" :model="forms.accountLogin" :rules="rules.accountLogin" label-width="96px">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="forms.accountLogin.username" placeholder="请输入用户名" maxlength="30" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                  <el-input v-model="forms.accountLogin.password" type="password" placeholder="请输入密码" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="loading.accountLogin" @click="handleAccountLogin">登录</el-button>
                  <el-button @click="resetAccountLogin">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="邮箱验证码登录" name="emailLogin">
              <el-form ref="emailLoginFormRef" :model="forms.emailLogin" :rules="rules.emailLogin" label-width="96px">
                <el-form-item label="邮箱地址" prop="email">
                  <el-input v-model="forms.emailLogin.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                  <el-input v-model="forms.emailLogin.code" placeholder="请输入验证码" maxlength="6">
                    <template #append>
                      <el-button :loading="loading.sendEmailCode" @click="handleSendEmailCode">发送验证码</el-button>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="验证码场景" prop="scene">
                  <el-select v-model="forms.emailLogin.scene" placeholder="选择验证码场景">
                    <el-option v-for="item in emailScenes" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="forms.emailLogin.nickname" placeholder="可选" maxlength="30" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="loading.emailLogin" @click="handleEmailLogin">登录</el-button>
                  <el-button @click="resetEmailLogin">重置</el-button>
                </el-form-item>
                <el-alert type="info" show-icon :closable="false" title="提示">
                  验证码登录时，如果邮箱未注册会自动创建账号。
                </el-alert>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="小程序模拟登录" name="miniLogin">
              <el-form ref="miniLoginFormRef" :model="forms.miniLogin" :rules="rules.miniLogin" label-width="120px">
                <el-form-item label="临时代码" prop="code">
                  <el-input v-model="forms.miniLogin.code" placeholder="任意字符串即可" />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="forms.miniLogin.nickname" placeholder="可选" maxlength="30" />
                </el-form-item>
                <el-form-item label="头像地址">
                  <el-input v-model="forms.miniLogin.avatarUrl" placeholder="可选" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="forms.miniLogin.phone" placeholder="可选" maxlength="20" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="loading.miniLogin" @click="handleMiniLogin">模拟登录</el-button>
                  <el-button @click="resetMiniLogin">重置</el-button>
                </el-form-item>
                <el-alert type="info" show-icon :closable="false" title="提示">
                  启用了模拟支付后，系统会为新用户自动生成初始钱包余额，可直接测试下单扣款流程。
                </el-alert>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="card-header">登录结果</div>
          </template>
          <div v-if="!loginState.token">
            <el-empty description="还没有登录数据" />
          </div>
          <div v-else>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="访问令牌">
                <el-input v-model="loginState.token" type="textarea" readonly autosize></el-input>
              </el-descriptions-item>
              <el-descriptions-item label="是否新用户">{{ loginState.newUser ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="过期秒数">{{ loginState.expiresIn ?? '未知' }}</el-descriptions-item>
            </el-descriptions>
            <el-divider />
            <div class="section-title">登录响应中的用户信息</div>
            <pre class="json-viewer">{{ formattedUser }}</pre>
            <el-divider />
            <div class="profile-header">
              <span class="section-title">刷新后的用户信息</span>
              <el-button size="small" type="primary" :loading="loading.profile" @click="refreshProfile">刷新</el-button>
            </div>
            <pre class="json-viewer">{{ formattedProfile }}</pre>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { accountRegister, accountLogin, emailSendCode, emailLogin, miniLogin, getLoginUserInfoWithToken } from '@/api/mock/userAuth'

const activeTab = ref('accountRegister')

const accountRegisterFormRef = ref()
const accountLoginFormRef = ref()
const emailLoginFormRef = ref()
const miniLoginFormRef = ref()

const emailScenes = [
  { label: '登录 LOGIN', value: 'LOGIN' },
  { label: '注册 REGISTER', value: 'REGISTER' },
  { label: '重置密码 RESET_PASSWORD', value: 'RESET_PASSWORD' }
]

const forms = reactive({
  accountRegister: {
    username: '',
    password: '',
    confirmPassword: '',
    nickname: ''
  },
  accountLogin: {
    username: '',
    password: ''
  },
  emailLogin: {
    email: '',
    code: '',
    nickname: '',
    scene: 'LOGIN'
  },
  miniLogin: {
    code: 'mock-code',
    nickname: '',
    avatarUrl: '',
    phone: ''
  }
})

const rules = {
  accountRegister: {
    username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
    password: [{ required: true, message: '密码不能为空', trigger: 'blur' }, { min: 6, message: '密码不少于 6 位', trigger: 'blur' }],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      {
        validator: (_, value, callback) => {
          if (value !== forms.accountRegister.password) {
            callback(new Error('两次输入的密码不一致'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  },
  accountLogin: {
    username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
    password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
  },
  emailLogin: {
    email: [
      { required: true, message: '邮箱不能为空', trigger: 'blur' },
      { type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur', 'change'] }
    ],
    code: [{ required: true, message: '验证码不能为空', trigger: 'blur' }],
    scene: [{ required: true, message: '请选择验证码场景', trigger: 'change' }]
  },
  miniLogin: {
    code: [{ required: true, message: '临时代码不能为空', trigger: 'blur' }]
  }
}

const loading = reactive({
  accountRegister: false,
  accountLogin: false,
  emailLogin: false,
  miniLogin: false,
  sendEmailCode: false,
  profile: false
})

const loginState = reactive({
  token: '',
  expiresIn: null,
  newUser: false,
  user: null
})

const profileInfo = ref(null)

const formattedUser = computed(() => loginState.user ? JSON.stringify(loginState.user, null, 2) : '暂无数据')
const formattedProfile = computed(() => profileInfo.value ? JSON.stringify(profileInfo.value, null, 2) : '点击上方按钮刷新')

const handleLoginSuccess = async (data, message) => {
  if (!data) {
    ElMessage.warning('接口未返回数据')
    return
  }
  loginState.token = data.accessToken || ''
  loginState.expiresIn = data.expiresIn ?? null
  loginState.newUser = !!data.newUser
  loginState.user = data.user || null
  profileInfo.value = null
  if (loginState.token) {
    await refreshProfile()
  }
  ElMessage.success(message)
}

const handleAccountRegister = () => {
  accountRegisterFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.accountRegister = true
    try {
      const res = await accountRegister(forms.accountRegister)
      await handleLoginSuccess(res.data, '账号注册成功')
    } finally {
      loading.accountRegister = false
    }
  })
}

const handleAccountLogin = () => {
  accountLoginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.accountLogin = true
    try {
      const res = await accountLogin(forms.accountLogin)
      await handleLoginSuccess(res.data, '账号登录成功')
    } finally {
      loading.accountLogin = false
    }
  })
}

const handleSendEmailCode = async () => {
  if (!forms.emailLogin.email) {
    ElMessage.warning('请先填写邮箱地址')
    return
  }
  loading.sendEmailCode = true
  try {
    await emailSendCode({ email: forms.emailLogin.email, scene: forms.emailLogin.scene })
    ElMessage.success('验证码已发送，请查看邮箱')
  } finally {
    loading.sendEmailCode = false
  }
}

const handleEmailLogin = () => {
  emailLoginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.emailLogin = true
    try {
      const res = await emailLogin(forms.emailLogin)
      await handleLoginSuccess(res.data, '邮箱登录成功')
    } finally {
      loading.emailLogin = false
    }
  })
}

const handleMiniLogin = () => {
  miniLoginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.miniLogin = true
    try {
      const res = await miniLogin(forms.miniLogin)
      await handleLoginSuccess(res.data, '小程序模拟登录成功')
    } finally {
      loading.miniLogin = false
    }
  })
}

const refreshProfile = async () => {
  if (!loginState.token) {
    ElMessage.warning('请先完成登录')
    return
  }
  loading.profile = true
  try {
    const res = await getLoginUserInfoWithToken(loginState.token)
    profileInfo.value = res.data || null
  } finally {
    loading.profile = false
  }
}

const resetAccountRegister = () => {
  forms.accountRegister.username = ''
  forms.accountRegister.password = ''
  forms.accountRegister.confirmPassword = ''
  forms.accountRegister.nickname = ''
}

const resetAccountLogin = () => {
  forms.accountLogin.username = ''
  forms.accountLogin.password = ''
}

const resetEmailLogin = () => {
  forms.emailLogin.email = ''
  forms.emailLogin.code = ''
  forms.emailLogin.nickname = ''
  forms.emailLogin.scene = 'LOGIN'
}

const resetMiniLogin = () => {
  forms.miniLogin.code = 'mock-code'
  forms.miniLogin.nickname = ''
  forms.miniLogin.avatarUrl = ''
  forms.miniLogin.phone = ''
}
</script>

<style scoped>
.mock-auth-page {
  padding: 16px;
}

.form-card {
  min-height: 560px;
}

.card-header {
  font-weight: 600;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
  display: inline-block;
}

.json-viewer {
  background-color: #f7f9fc;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-card {
  min-height: 560px;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
</style>
