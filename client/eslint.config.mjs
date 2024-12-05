import globals from "globals";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";
import vueTsEslintConfig from "@vue/eslint-config-typescript";
import prettierConfig from "@vue/eslint-config-prettier";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default [{
    ignores: ["**/generated/**/*"],
}, ...compat.extends(
    "plugin:vue/vue3-recommended",
    "eslint:recommended",
), ...vueTsEslintConfig(), {
    languageOptions: {
        globals: {
            ...globals.node,
        },

        ecmaVersion: 2020,
        sourceType: "commonjs",
    },

    rules: {
        "vue/custom-event-name-casing": ["error", {
            ignores: ["/^[a-z]+(?:-[a-z]+)*:[a-z]+(?:-[a-z]+)*$/u"],
        }],

        "vue/valid-v-slot": ["error", {
            allowModifiers: true,
        }],
    },
}, {
    files: ["**/__tests__/*.{j,t}s?(x)", "**/tests/unit/**/*.spec.{j,t}s?(x)"],

    languageOptions: {
        globals: {
            ...globals.jest,
        },
    },
}, ...compat.extends("plugin:@graphql-eslint/operations-recommended").map(config => ({
    ...config,
    files: ["**/*.gql"],
})), {
    files: ["**/*.gql"],

    languageOptions: {
        ecmaVersion: 5,
        sourceType: "script",

        parserOptions: {
            graphQLConfig: {
                schema: "./target/dependency/schema.graphql",
                documents: "./src/**/*.gql",
            },
        },
    },
}, {
    files: ["**/*.vue"],
}, prettierConfig];
