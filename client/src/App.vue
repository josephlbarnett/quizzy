import gql from "graphql-tag";
<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <div class="d-flex align-center">
        <v-img
          alt="Quizzy"
          class="shrink mr-2"
          contain
          src="./assets/logo.png"
          transition="scale-transition"
          width="40"
        />
      </div>
      <ApolloQuery :query="userQuery">
        <template v-slot="{ result: { error, data } /*, isLoading*/ }">
          <ApolloMutation
            :mutation="logoutQuery"
            :refetch-queries="() => [`currentUser`]"
            :await-refetch-queries="true"
            v-if="data.user"
          >
            <template v-slot="{ mutate, loading /*, error*/ }">
              {{ data.user.name }}
              <v-btn :disabled="loading" label="Logout" @click="mutate()"
                >Logout</v-btn
              >
            </template>
          </ApolloMutation>
        </template>
      </ApolloQuery>
      <v-spacer />
      <v-btn to="/">
        <span>home</span>
      </v-btn>

      <v-btn to="/about">
        <span>about</span>
      </v-btn>
    </v-app-bar>

    <v-content>
      <Login> </Login>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Login from "@/components/Login.vue";
import gql from "graphql-tag";

export default Vue.extend({
  name: "App",
  data: () => ({
    userQuery: gql`
      query currentUser {
        user {
          name
        }
      }
    `,
    loginQuery: gql`
      mutation login($email: String!, $pass: String!) {
        login(email: $email, pass: $pass)
      }
    `,
    logoutQuery: gql`
      mutation logout {
        logout
      }
    `
  }),
  components: {
    Login
  }
});
</script>
