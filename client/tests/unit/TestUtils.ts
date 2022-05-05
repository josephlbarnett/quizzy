import flushPromises from "flush-promises";
import { Wrapper } from "@vue/test-utils";
import Vue from "vue";

export async function awaitVm<T extends Vue>(wrapper: Wrapper<T>) {
  await wrapper.vm.$nextTick();
  await flushPromises();
}
