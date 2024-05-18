import { createApp } from "vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import { createProvider } from "./vue-apollo";
import { createPinia } from "pinia";
import App from "./App.vue";
import VueApolloPlugin from "@vue/apollo-components";

// Vue.config.productionTip = false;
// Vue.use(PiniaVuePlugin);
const pinia = createPinia();
const app = createApp(App);
app.use(router);
app.use(vuetify);
app.use(createProvider());
app.use(pinia);
app.use(VueApolloPlugin);
app.mount("#app");
