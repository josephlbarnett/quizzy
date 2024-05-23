import { createVuetify } from "vuetify";
import colors from "vuetify/util/colors";
import "vuetify/styles";

const colorScheme = {
  primary: colors.indigo.base,
  secondary: colors.indigo.darken2,
  accent: colors.teal.darken1, //'#82B1FF',
  error: "#FF5252",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
};
const vuetify = createVuetify({
  theme: {
    themes: {
      dark: { dark: true, colors: colorScheme },
      light: { dark: false, colors: colorScheme },
    },
  },
  defaults: {
    VCardTitle: {
      class: 'd-flex',
    },
  },
});

export default vuetify;
