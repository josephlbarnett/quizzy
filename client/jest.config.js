module.exports = {
  collectCoverage: true,
  collectCoverageFrom: [
    "**/*.{js,jsx,vue,ts}",
    "!**/node_modules/**",
    "!**/vendor/**",
    "!**/coverage/**",
    "!**/src/generated/**",
    "!**/dist/**",
    "!**/target/**",
    "!**/*.spec.{js,jsx,vue,ts}",
    "!**/*.config.js",
  ],
  coverageReporters: ["lcov", "text-summary"],
  moduleFileExtensions: ["js", "json", "jsx", "ts", "tsx", "node", "vue"],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  testEnvironment: "jsdom",
  transform: {
    "^.+\\.(js|ts|d.ts)$": "babel-jest",
    "^.+\\.sass$": "jest-transform-stub",
    "^.+\\.vue$": "@vue/vue2-jest",
    "^.+\\.gql$": "./tests/unit/gqlTransformer", // "jest-transform-graphql",
  },
  transformIgnorePatterns: ["/node_modules/(?!vuetify)/"],
  setupFilesAfterEnv: ["<rootDir>/tests/unit/setup.ts"],
};
