<template>
  <ApolloQuery :query="userQuery">
    <template v-slot="{ result: { error, data }, isLoading }">
      <!-- Loading -->
      <v-progress-circular indeterminate="true" v-if="isLoading" />

      <!-- Error -->
      <div v-else-if="error" class="error apollo">An error occurred</div>

      <!-- Result -->
      <!--            <v-app-bar v-else-if="data.user" class="result apollo">-->
      <div v-else-if="data.user" class="result apollo">
        <router-view />
      </div>
      <!--              </v-app-bar>-->

      <!-- No result -->
      <div v-else class="no-result apollo">
        <ApolloMutation
          :mutation="loginQuery"
          :variables="{
            email,
            pass
          }"
          :refetch-queries="() => [`currentUser`]"
          :await-refetch-queries="true"
          @done="loggedin"
          @error="loggedin({ data: { login: false } })"
        >
          <template v-slot="{ mutate, loading /*, error*/ }">
            <v-container>
              <v-text-field v-model="email" label="Email" @keypress="key" />
              <v-text-field
                v-model="pass"
                label="Password"
                type="password"
                @keypress="key"
              />
              <v-btn
                :disabled="loading"
                label="Login"
                @click="clickLogin(mutate)"
                >Login</v-btn
              >
              <v-snackbar v-model="failedLogin">
                Couldn't log in, try again.
                <v-btn @click="failedLogin = false">OK</v-btn>
              </v-snackbar>
              <v-progress-circular indeterminate="true" v-if="loading" />
            </v-container>
          </template>
        </ApolloMutation>
      </div>
    </template>
  </ApolloQuery>
</template>
<script lang="ts">
import Vue from "vue";
import gql from "graphql-tag";

export default Vue.extend({
  name: "Login",
  data: () => ({
    email: "",
    pass: "",
    failedLogin: false,
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
  methods: {
    submit() {
      console.log(`submit ${this.email}`);
    },
    clickLogin(mutate: Function) {
      this.failedLogin = false;
      mutate();
    },
    loggedin({ data: { login } }: { data: { login: boolean } }) {
      this.failedLogin = !login;
    },
    key({ key }: { key: string }) {
      if (key == "Enter") {
        this.submit();
      }
    }
  }
});
</script>
