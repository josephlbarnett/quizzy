import { ApolloProvider, createApolloProvider } from "@vue/apollo-option";
import {
  ApolloClient,
  createHttpLink,
  InMemoryCache,
} from "@apollo/client/core";
import { ApolloProviderOptions } from "@vue/apollo-option/types/apollo-provider";

// Install the vue plugin

// Name of the localStorage item
const AUTH_TOKEN = "apollo-token";

// Http endpoint
const httpEndpoint =
  import.meta.env.VITE_GRAPHQL_HTTP || "http://localhost:8081/app/graphql";
// Files URL root
export const filesRoot =
  import.meta.env.VITE_FILES_ROOT ||
  httpEndpoint.substr(0, httpEndpoint.indexOf("/graphql"));

// Config
const defaultOptions = {
  // You can use `https` for secure connection (recommended in production)
  httpEndpoint,
  // You can use `wss` for secure connection (recommended in production)
  // Use `null` to disable subscriptions
  wsEndpoint: undefined,
  //import.meta.env.VUE_APP_GRAPHQL_WS || "ws://localhost:8081/app/graphql",
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
export function createProvider(
  options: ApolloProviderOptions | null = null,
): ApolloProvider {
  // Create apollo client
  const apolloClient = new ApolloClient({
    link: createHttpLink({
      uri: defaultOptions.httpEndpoint,
      ...defaultOptions.httpLinkOptions,
    }),
    cache: new InMemoryCache(),
    ...defaultOptions,
  });

  // Create vue apollo provider
  const client = options?.defaultClient ? options.defaultClient : apolloClient;
  return createApolloProvider({
    defaultClient: client,
    defaultOptions: {
      $query: {
        // fetchPolicy: 'cache-and-network',
      },
    },
    // errorHandler(error) {
    // eslint-disable-next-line no-console
    // console.log(
    //   "%cError",
    //   "background: red; color: white; padding: 2px 4px; border-radius: 3px; font-weight: bold;",
    //   error.message,
    // );
    // },
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
