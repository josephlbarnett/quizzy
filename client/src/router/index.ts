import Vue from "vue";
import VueRouter from "vue-router";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    name: "Current Questions",
    component: () =>
      import(
        /* webpackChunkName: "currentQuestions" */ "../views/CurrentQuestions.vue"
      ),
  },
  {
    path: "/write",
    name: "Future Questions",
    component: () =>
      import(
        /* webpackChunkName: "futureQuestions" */ "../views/FutureQuestions.vue"
      ),
  },
  {
    path: "/review",
    name: "Completed Questions",
    component: () =>
      import(
        /* webpackChunkName: "completedQuestions" */ "../views/CompletedQuestions.vue"
      ),
  },
  {
    path: "/users",
    name: "Users",
    component: () =>
      import(/* webpackChunkName: "users" */ "../views/Users.vue"),
  },
  {
    path: "/me",
    name: "User",
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "user" */ "../views/User.vue"),
  },
];

const router = new VueRouter({
  routes,
});

export default router;
