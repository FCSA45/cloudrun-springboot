<template>
  <div class="page">
    <div class="card" v-if="authStore.userInfo">
      <div class="header">
        <img :src="avatar" alt="avatar" class="avatar" />
        <div>
          <h2>{{ displayName }}</h2>
          <p>ID / OpenId：{{ authStore.userInfo.wxOpenId || '无' }}</p>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <span class="label">账号</span>
          <span>{{ authStore.userInfo.username || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">邮箱</span>
          <span>{{ authStore.userInfo.email || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">押金状态</span>
          <span>{{ authStore.userInfo.depositStatus === 1 ? '已免押' : '需押金' }}</span>
        </div>
        <div class="info-item">
          <span class="label">可用余额</span>
          <span class="money">￥{{ formatMoney(authStore.userInfo.balance) }}</span>
        </div>
        <div class="info-item">
          <span class="label">冻结金额</span>
          <span class="money">￥{{ formatMoney(authStore.userInfo.frozenAmount) }}</span>
        </div>
      </div>

      <div class="actions">
        <button type="button" @click="refresh">刷新信息</button>
        <button type="button" class="logout" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <p v-else class="empty">暂无用户信息，请返回登录页重新登录。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { fetchProfile } from '../api/user';

const router = useRouter();
const authStore = useAuthStore();

const avatar = computed(() => authStore.userInfo?.avatar || 'https://cdn.jsdelivr.net/gh/avatars-delta/placeholder@main/avatar.png');
const displayName = computed(() => authStore.userInfo?.nickname || authStore.userInfo?.username || '未命名用户');

function formatMoney(value) {
  if (!value && value !== 0) {
    return '0.00';
  }
  try {
    return Number(value).toFixed(2);
  } catch (err) {
    return value;
  }
}

async function refresh() {
  const profile = await fetchProfile();
  authStore.setUserInfo(profile?.data || profile);
}

function handleLogout() {
  authStore.logout();
  router.replace('/');
}

if (!authStore.isLoggedIn) {
  router.replace('/');
} else if (!authStore.userInfo) {
  refresh();
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #eef2f8;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 16px;
}

.card {
  width: min(720px, 100%);
  background: #fff;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 16px 36px rgba(31, 45, 61, 0.12);
}

.header {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #409eff;
}

h2 {
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  background: #f6f9fc;
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  color: #8492a6;
  font-size: 13px;
}

.money {
  font-weight: 600;
  font-size: 18px;
  color: #36cfc9;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

button {
  border: none;
  cursor: pointer;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 15px;
  background: #409eff;
  color: #fff;
}

button.logout {
  background: #f56c6c;
}

.empty {
  color: #4d5c6b;
  font-size: 16px;
}

@media (max-width: 480px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
