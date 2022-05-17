const loader = require("graphql-tag/loader");

// hack to adjust to jest28 changes in jest-transform-graphql

module.exports = {
  process(src) {
    return {
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      code: loader.call({ cacheable() {} }, src),
    };
  },
};
