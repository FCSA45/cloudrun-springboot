import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '../views/LoginPage.vue';
import ProfilePage from '../views/ProfilePage.vue';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'login',
    component: LoginPage
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfilePage
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  if (to.name === 'profile' && !authStore.isLoggedIn) {
    next({ name: 'login' });
    return;
  }
  if (to.name === 'login' && authStore.isLoggedIn) {
    next({ name: 'profile' });
    return;
  }
  next();
});

export default router;
