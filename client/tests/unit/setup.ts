import Vue from "vue";
import Vuetify from "vuetify";
import VueApollo from "vue-apollo";
import "regenerator-runtime/runtime";
import { PiniaVuePlugin } from "pinia";
import { beforeAll, vi } from "vitest";

vi.mock("vue", async () => {
  const Vue = await vi.importActual("vue");
  Vue.default.config.productionTip = false;
  Vue.default.config.devtools = false;
  return Vue;
});

beforeAll(() => {
  Vue.use(VueApollo);
  Vue.use(Vuetify);
  Vue.use(PiniaVuePlugin);
});
