import { defineConfig } from "vite";
import { createVuePlugin } from "vite-plugin-vue2";
import path from "path";
import graphqlPlugin from "@rollup/plugin-graphql";
import Components from "unplugin-vue-components/vite";
import { VuetifyResolver } from "unplugin-vue-components/resolvers";

// https://vitejs.dev/config/
export default defineConfig({
  base: "/app/assets/",
  plugins: [
    createVuePlugin(),
    graphqlPlugin(),
    Components({ resolvers: [VuetifyResolver()] }),
  ],
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
  // silence sass/css warning https://github.com/vitejs/vite/issues/6333
  css: {
    postcss: {
      plugins: [
        {
          postcssPlugin: "internal:charset-removal",
          AtRule: {
            charset: (atRule) => {
              if (atRule.name === "charset") {
                atRule.remove();
              }
            },
          },
        },
      ],
    },
  },
});
