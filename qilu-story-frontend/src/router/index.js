import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/',
    name: 'Book',
    // BookView is rendered directly in App.vue, so we use a minimal component
    component: { template: '<div></div>' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Global guard for API 401 redirects
router.beforeEach((to, from, next) => {
  // Allow all navigation; BookView handles its own auth logic
  next()
})

export default router
