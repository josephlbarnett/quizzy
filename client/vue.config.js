module.exports = {
  transpileDependencies: ["vuetify", "apollo"],
  publicPath: "/app/assets",
  pluginOptions: { apollo: { lintGQL: false } },
  parallel: false,
  css: { extract: { ignoreOrder: true } },
};
