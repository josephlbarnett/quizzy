import { createRouter, createWebHashHistory } from "vue-router";

const routes = [
  {
    path: "/",
    name: "Current Questions",
    component: () => import("../views/CurrentQuestions.vue"),
  },
  {
    path: "/write",
    name: "Future Questions",
    component: () => import("../views/FutureQuestions.vue"),
  },
  {
    path: "/review",
    name: "Completed Questions",
    component: () => import("../views/CompletedQuestions.vue"),
  },
  {
    path: "/users",
    name: "Users",
    component: () => import("../views/Users.vue"),
  },
  {
    path: "/grade",
    name: "Grade",
    component: () => import("../views/Grade.vue"),
  },
  {
    path: "/me",
    name: "User",
    component: () => import("../views/User.vue"),
  },
  {
    path: "/quiz",
    name: "quiz",
    component: () => import("../views/PopQuiz.vue"),
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;
