<template>
  <ApolloQuery :query="require('../graphql/CurrentUser.gql')">
    <template v-slot="{ result: { error, data }, isLoading }">
      <!-- Loading -->
      <v-progress-circular :indeterminate="true" v-if="isLoading" />

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
          :mutation="require('../graphql/Login.gql')"
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
              <v-text-field
                v-model="email"
                label="Email"
                @keypress="e => key(e, mutate)"
              />
              <v-text-field
                v-model="pass"
                label="Password"
                type="password"
                @keypress="e => key(e, mutate)"
              />
              <v-btn
                :disabled="loading"
                label="Login"
                block="true"
                color="accent"
                @click="clickLogin(mutate)"
                >Login</v-btn
              >
              <v-snackbar v-model="failedLogin">
                Couldn't log in, try again.
                <v-btn @click="failedLogin = false">OK</v-btn>
              </v-snackbar>
              <v-progress-circular :indeterminate="true" v-if="loading" />
            </v-container>
          </template>
        </ApolloMutation>
      </div>
    </template>
  </ApolloQuery>
</template>
<script lang="ts">
import Vue from "vue";

export default Vue.extend({
  name: "Login",
  data: () => ({
    email: "",
    pass: "",
    failedLogin: false
  }),
  methods: {
    clickLogin(mutate: Function) {
      this.failedLogin = false;
      mutate();
    },
    loggedin({ data: { login } }: { data: { login: boolean } }) {
      this.failedLogin = !login;
    },
    key({ key }: { key: string }, mutate: Function) {
      if (key == "Enter") {
        mutate();
      }
    }
  }
});
</script>
