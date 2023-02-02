import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import { createProvider } from "./vue-apollo";
import "regenerator-runtime/runtime";
import { createPinia, PiniaVuePlugin } from "pinia";

Vue.config.productionTip = false;
Vue.use(PiniaVuePlugin);

new Vue({
  router,
  vuetify,
  apolloProvider: createProvider(),
  pinia: createPinia(),
  render: (h) => h(App),
}).$mount("#app");
