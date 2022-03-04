/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_GRAPHQL_HTTP: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
