import Vue from "vue";
import Vuetify from "vuetify/lib";
import colors from "vuetify/lib/util/colors";

Vue.use(Vuetify);
const colorScheme = {
  primary: colors.indigo.base,
  secondary: colors.indigo.darken2,
  accent: colors.teal.darken1, //'#82B1FF',
  error: "#FF5252",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
};
export default new Vuetify({
  theme: {
    themes: {
      dark: colorScheme,
      light: colorScheme,
    },
  },
});
