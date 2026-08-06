import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import QueryPage from '@/views/Query.vue'
import QueryResultPage from '@/views/QueryResult.vue'
import SurveyPage from '@/views/Survey.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: Home },
    { path: '/q/:code', name: 'query', component: QueryPage },
    { path: '/q/:code/result', name: 'query-result', component: QueryResultPage },
    { path: '/s/:code', name: 'survey', component: SurveyPage }
  ],
  scrollBehavior: () => ({ top: 0 })
})

export default router
