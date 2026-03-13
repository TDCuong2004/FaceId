import { createRouter, createWebHistory } from "vue-router"

import Login from "../components/Login.vue"
import FaceApp from "../components/FaceApp.vue"

const routes = [
  {
    path: "/",
    component: Login
  },
  {
    path: "/app",
    component: FaceApp
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router