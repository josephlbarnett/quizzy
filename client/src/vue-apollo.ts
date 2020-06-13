import Vue from "vue";
import VueApollo, { ApolloProvider } from "vue-apollo";
// @ts-ignore
import { createApolloClient } from "vue-cli-plugin-apollo/graphql-client";

// Install the vue plugin
Vue.use(VueApollo);

// Name of the localStorage item
const AUTH_TOKEN = "apollo-token";

// Http endpoint
const httpEndpoint =
  process.env.VUE_APP_GRAPHQL_HTTP || "http://localhost:8081/app/graphql";
// Files URL root
export const filesRoot =
  process.env.VUE_APP_FILES_ROOT ||
  httpEndpoint.substr(0, httpEndpoint.indexOf("/graphql"));

Vue.prototype.$filesRoot = filesRoot;

// Config
const defaultOptions = {
  // You can use `https` for secure connection (recommended in production)
  httpEndpoint,
  // You can use `wss` for secure connection (recommended in production)
  // Use `null` to disable subscriptions
  wsEndpoint: undefined,
  //process.env.VUE_APP_GRAPHQL_WS || "ws://localhost:8081/app/graphql",
  // LocalStorage token
  tokenName: AUTH_TOKEN,
  // Enable Automatic Query persisting with Apollo Engine
  persisting: false,
  // Use websockets for everything (no HTTP)
  // You need to pass a `wsEndpoint` for this to work
  websocketsOnly: false,
  // Is being rendered on the server?
  ssr: false,
  httpLinkOptions: {
    credentials: "include",
  },

  // Override default apollo link
  // note: don't override httpLink here, specify httpLink options in the
  // httpLinkOptions property of defaultOptions.
  // link: myLink

  // Override default cache
  // cache: myCache

  // Override the way the Authorization header is set
  // getAuth: (tokenName) => ...

  // Additional ApolloClient options
  // apollo: { ... }

  // Client local data (see apollo-link-state)
  // clientState: { resolvers: { ... }, defaults: { ... } }
};

// Call this in the Vue app file
export function createProvider(options = {}): ApolloProvider {
  // Create apollo client
  const { apolloClient } = createApolloClient({
    ...defaultOptions,
    ...options,
  });

  // Create vue apollo provider
  return new VueApollo({
    defaultClient: apolloClient,
    defaultOptions: {
      $query: {
        // fetchPolicy: 'cache-and-network',
      },
    },
    errorHandler(error) {
      // eslint-disable-next-line no-console
      console.log(
        "%cError",
        "background: red; color: white; padding: 2px 4px; border-radius: 3px; font-weight: bold;",
        error.message
      );
    },
  });
}
//
// // Manually call this when user log in
// export async function onLogin(apolloClient, token: string) {
//   if (typeof localStorage !== "undefined" && token) {
//     localStorage.setItem(AUTH_TOKEN, token);
//   }
//   try {
//     await apolloClient.resetStore();
//   } catch (e) {
//     // eslint-disable-next-line no-console
//     console.log("%cError on cache reset (login)", "color: orange;", e.message);
//   }
// }
//
// // Manually call this when user log out
// export async function onLogout(apolloClient) {
//   if (typeof localStorage !== "undefined") {
//     localStorage.removeItem(AUTH_TOKEN);
//   }
//   try {
//     await apolloClient.resetStore();
//   } catch (e) {
//     // eslint-disable-next-line no-console
//     console.log("%cError on cache reset (logout)", "color: orange;", e.message);
//   }
// }
