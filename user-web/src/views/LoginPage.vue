<template>
  <div class="page">
      <div class="card">
        <h1>共享充电宝 · 登录体验</h1>
        <p class="subtitle">选择一种方式模拟登录，体验后端接口。</p>

        <div class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab-btn', { active: activeTab === tab.value }]"
            type="button"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <form
          v-if="activeTab === 'account'"
          class="form"
          @submit.prevent="handleAccountSubmit"
        >
          <label>
            <span>账号</span>
            <input v-model.trim="accountForm.username" required placeholder="请输入账号" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="accountForm.password" type="password" required placeholder="请输入密码" />
          </label>
          <div class="form-actions">
            <button type="button" class="link" @click="handleRegister">
              没有账号？一键注册并登录
            </button>
            <button :disabled="loading" type="submit">登录</button>
          </div>
        </form>

        <form
          v-else-if="activeTab === 'email'"
          class="form"
          @submit.prevent="handleEmailLogin"
        >
          <label>
            <span>邮箱</span>
            <input v-model.trim="emailForm.email" type="email" required placeholder="example@qq.com" />
          </label>
          <label class="code-row">
            <span>验证码</span>
            <input v-model.trim="emailForm.code" required placeholder="请输入验证码" />
            <button type="button" class="secondary" :disabled="countdown > 0 || loading" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </label>
          <div class="form-actions">
            <button :disabled="loading" type="submit">登录 / 注册</button>
          </div>
        </form>

        <form
          v-else
          class="form"
          @submit.prevent="handleMiniLogin"
        >
          <label>
            <span>手机号</span>
            <input v-model.trim="miniForm.phone" required placeholder="11位手机号" />
          </label>
          <label>
            <span>昵称</span>
            <input v-model.trim="miniForm.nickname" placeholder="可选昵称" />
          </label>
          <div class="form-actions">
            <span class="fake-code">模拟 code: {{ miniForm.code }}</span>
            <button :disabled="loading" type="submit">模拟登录</button>
          </div>
          <p class="tip">此模式无需真实微信 code，主要用于演示余额扣款等流程。</p>
        </form>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="error" class="error">{{ error }}</p>
      </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  accountLogin,
  accountRegister,
  emailLogin,
  sendEmailCode,
  fetchProfile,
  mockMiniLogin
} from '../api/user';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('account');
const tabs = [
  { label: '账号密码', value: 'account' },
  { label: '邮箱验证码', value: 'email' },
  { label: '模拟小程序', value: 'mini' }
];
const loading = ref(false);
const error = ref('');
const countdown = ref(0);
let countdownTimer = null;
const notice = ref('');

const accountForm = ref({
  username: '',
  password: ''
});

const emailForm = ref({
  email: '',
  code: '',
  scene: 'LOGIN',
  nickname: ''
});

const miniForm = ref({
  code: 'mock-code',
  nickname: '测试用户',
  avatarUrl: 'https://cdn.jsdelivr.net/gh/avatars-delta/placeholder@main/avatar.png',
  phone: ''
});

const canSendCode = computed(() => countdown.value === 0 && !loading.value);

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      router.replace('/profile');
    }
  },
  { immediate: true }
);

function setError(message) {
  if (message) {
    notice.value = '';
  }
  error.value = message || '';
  if (message) {
    window.setTimeout(() => {
      error.value = '';
    }, 4000);
  }
}

function setNotice(message) {
  notice.value = message || '';
  if (message) {
    window.setTimeout(() => {
      notice.value = '';
    }, 4000);
  }
}

async function afterLogin(result) {
  const payload = result?.data ?? result;
  const token = payload?.accessToken;
  if (!token) {
    throw new Error('未获取到 accessToken');
  }
  authStore.setToken(token);
  try {
    const profile = await fetchProfile();
    const profileData = profile?.data ?? profile;
    authStore.setUserInfo(profileData);
  } catch (err) {
    // 如果刷新用户信息失败，也能先进入页面
    console.warn('获取用户信息失败:', err.message);
  }
  const userFromPayload = payload?.user;
  if (userFromPayload) {
    authStore.setUserInfo(userFromPayload);
  }
  router.replace('/profile');
}

async function handleAccountSubmit() {
  setError('');
  try {
    loading.value = true;
    const result = await accountLogin(accountForm.value);
    await afterLogin(result);
  } catch (err) {
    setError(err.message);
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  setError('');
  try {
    loading.value = true;
    const payload = {
      username: accountForm.value.username || `test_${Date.now()}`,
      password: accountForm.value.password || '123456',
      confirmPassword: accountForm.value.password || '123456',
      nickname: accountForm.value.username || '新用户'
    };
    const result = await accountRegister(payload);
    await afterLogin(result);
  } catch (err) {
    setError(err.message);
  } finally {
    loading.value = false;
  }
}

async function handleSendCode() {
  if (!canSendCode.value) {
    return;
  }
  if (!emailForm.value.email) {
    setError('请先填写邮箱');
    return;
  }
  try {
    loading.value = true;
    setError('');
    setNotice('');
    const resp = await sendEmailCode({ email: emailForm.value.email, scene: emailForm.value.scene });
    const payload = resp?.data ?? resp;
    if (payload?.mockCode) {
      emailForm.value.code = payload.mockCode;
      setNotice(`模拟验证码：${payload.mockCode}`);
    }
    countdown.value = 60;
    countdownTimer = window.setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        window.clearInterval(countdownTimer);
      }
    }, 1000);
  } catch (err) {
    setError(err.message);
  } finally {
    loading.value = false;
  }
}

async function handleEmailLogin() {
  setError('');
  try {
    loading.value = true;
    const payload = {
      ...emailForm.value,
      nickname: emailForm.value.nickname || `邮箱用户${Date.now()}`
    };
    const result = await emailLogin(payload);
    await afterLogin(result);
  } catch (err) {
    setError(err.message);
  } finally {
    loading.value = false;
  }
}

async function handleMiniLogin() {
  setError('');
  if (!miniForm.value.phone) {
    setError('请输入手机号');
    return;
  }
  try {
    loading.value = true;
    // 随机生成一个模拟 code，便于后台识别不同登录
    const mockCode = `mock_${Date.now()}`;
    miniForm.value.code = mockCode;
    const result = await mockMiniLogin({
      ...miniForm.value,
      code: mockCode
    });
    await afterLogin(result);
  } catch (err) {
    setError(err.message);
  } finally {
    loading.value = false;
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 16px;
  background: linear-gradient(160deg, #409eff 0%, #53c2c8 100%);
}

.card {
  width: min(640px, 100%);
  background: #fff;
  border-radius: 18px;
  padding: 32px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

h1 {
  margin: 0 0 12px;
  font-size: 26px;
  color: #1f2d3d;
}

.subtitle {
  margin: 0 0 24px;
  color: #5c6b77;
}

.tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}

.tab-btn {
  border: none;
  border-radius: 12px;
  background: #f1f2f5;
  padding: 12px 0;
  font-size: 15px;
  cursor: pointer;
  color: #4d5661;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: linear-gradient(120deg, #409eff, #36cfc9);
  color: #fff;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.35);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #4d5661;
  font-size: 14px;
}

input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #d8dde4;
  font-size: 15px;
}

input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

button {
  border: none;
  cursor: pointer;
  border-radius: 10px;
  padding: 12px 18px;
  background: #409eff;
  color: #fff;
  font-size: 15px;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

button.secondary {
  background: #f0f2f5;
  color: #4d5661;
}

button.link {
  background: transparent;
  color: #409eff;
  padding: 0;
}

.error {
  margin-top: 18px;
  color: #f56c6c;
  text-align: center;
}

.notice {
  margin-top: 18px;
  color: #18b57f;
  text-align: center;
}

.tip {
  margin-top: 12px;
  color: #667281;
  font-size: 13px;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr 140px;
  gap: 12px;
}

.fake-code {
  font-size: 13px;
  color: #8492a6;
}

@media (max-width: 520px) {
  .card {
    padding: 24px 20px;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  button.link {
    align-self: flex-start;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
