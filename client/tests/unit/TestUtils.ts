import flushPromises from "flush-promises";
import { VueWrapper } from "@vue/test-utils";

export async function awaitVm<T>(wrapper: VueWrapper<T>) {
  await wrapper.vm.$nextTick();
  await flushPromises();
}
