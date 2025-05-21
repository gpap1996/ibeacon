import { createRouter, createWebHistory } from 'vue-router';

import EstimoteView from '../views/EstimoteView.vue';
import HomeView from '../views/HomeView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/estimote',
      name: 'estimote',
      component: EstimoteView,
    },
  ],
});

export default router;
