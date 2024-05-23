import { defineConfig } from "vite";
import vuePlugin from "@vitejs/plugin-vue";
import path from "path";
import graphqlPlugin from "@rollup/plugin-graphql";
import vuetify from "vite-plugin-vuetify";

// https://vitejs.dev/config/
export default defineConfig({
  base: "/app/assets/",
  plugins: [vuePlugin(), graphqlPlugin(), vuetify({ autoImport: true })],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: { port: 8080 },
  build: {
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          if (id.includes("node_modules/vuetify/src/components")) {
            return "vuetifycomponents";
          } else if (id.includes("node_modules/vuetify")) {
            return "vuetify";
          } else if (id.includes("node_modules/vue")) {
            return "vue";
          } else if (
            id.includes("node_modules/moment-timezone") &&
            id.endsWith(".json")
          ) {
            return "moment-tz-data";
          } else if (id.includes("node_modules")) {
            return "vendor";
          }
        },
      },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    server: {
      deps: {
        inline: ["vuetify", "@vue/apollo-components"],
      },
    },
    setupFiles: "./tests/unit/setup.ts",
  },
});
