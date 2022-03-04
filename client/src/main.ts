import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify.ts";
import { createProvider } from "./vue-apollo.ts";
import "regenerator-runtime/runtime";

Vue.config.productionTip = false;

new Vue({
  router,
  vuetify,
  apolloProvider: createProvider(),
  render: (h) => h(App),
}).$mount("#app");
