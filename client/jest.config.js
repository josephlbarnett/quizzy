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
  ],
  coverageReporters: ["lcov", "text-summary"],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  transform: {
    "^.+\\.js$": "babel-jest",
    "^.+\\.ts$": "babel-jest",
    "^.+\\.vue$": "vue-jest",
    "^.+\\.gql$": "jest-transform-graphql",
  },
  setupFilesAfterEnv: ["<rootDir>/tests/unit/setup.ts"],
};
