import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../store/user';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import StoryView from '../views/StoryView.vue';

const routes = [
  { path: '/', name: 'Login', component: LoginView },
  { path: '/home', name: 'Home', component: HomeView, meta: { requiresAuth: true } },
  { path: '/story/:id', name: 'Story', component: StoryView, meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  if (to.meta.requiresAuth && !userStore.token) {
    return next({ path: '/' });
  }
  if (to.path === '/' && userStore.token) {
    return next({ path: '/home' });
  }
  next();
});

export default router;
