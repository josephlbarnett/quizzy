import "regenerator-runtime/runtime";
import { vi } from "vitest";
//
// vi.mock("vue", async () => {
//   const Vue = await vi.importActual("vue");
//   return Vue;
// });
//
// beforeAll(() => {
//   config.plugins.VueWrapper.install(VueApollo);
//   config.plugins.VueWrapper.install(createVuetify({}));
//   config.plugins.VueWrapper.install(PiniaVuePlugin);
// });

const ResizeObserverMock = vi.fn(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Stub the global ResizeObserver
vi.stubGlobal("ResizeObserver", ResizeObserverMock);
