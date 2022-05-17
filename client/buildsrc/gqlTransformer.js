const loader = require('graphql-tag/loader');

// hack to adjust to jest28 changes in jest-transform-graphql

module.exports = {
  process(src) {
    return {code: loader.call({ cacheable() {} }, src)};
  },
};
