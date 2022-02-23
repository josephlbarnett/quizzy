module.exports = {
  transpileDependencies: ["vuetify", "apollo"],
  publicPath: "/app/assets",
  pluginOptions: { apollo: { lintGQL: false } },
  parallel: false,
  css: { extract: { ignoreOrder: true } },
  chainWebpack: (config) => {
    config.module
      .rule("graphql")
      .test(/\.(gql|graphql)$/)
      .use("graphql-tag/loader")
      .loader("graphql-tag/loader")
      .end();
  },
};
