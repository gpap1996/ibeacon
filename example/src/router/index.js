import { createRouter, createWebHistory } from 'vue-router';

import EstimoteView from '../views/EstimoteView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'estimote',
      component: EstimoteView,
    },
  ],
});

export default router;
