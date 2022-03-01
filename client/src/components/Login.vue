<template>
  <ApolloQuery :query="require('../graphql/CurrentUser.gql')">
    <template v-slot="{ result: { error, data }, isLoading }">
      <!-- Loading -->
      <v-progress-circular :indeterminate="true" v-if="isLoading" />

      <!-- Error -->
      <div v-else-if="error" class="error apollo">An error occurred</div>

      <!-- Result -->
      <!--            <v-app-bar v-else-if="data.user" class="result apollo">-->
      <div v-else-if="data && data.user" class="result apollo">
        <router-view />
      </div>
      <!--              </v-app-bar>-->

      <!-- No result -->
      <div v-else class="no-result apollo">
        <ApolloMutation
          :mutation="require('../graphql/Login.gql')"
          :variables="{
            email,
            pass,
          }"
          :refetch-queries="() => [`CurrentUser`]"
          :await-refetch-queries="true"
          @done="loggedin"
          @error="loggedin({ data: { login: false } })"
        >
          <template v-slot="{ mutate, loading /*, error*/ }">
            <v-container v-if="!resetPage && !completePage">
              <v-text-field
                v-model="email"
                label="Email"
                @keypress="(e) => key(e, mutate)"
              />
              <v-text-field
                v-model="pass"
                label="Password"
                type="password"
                @keypress="(e) => key(e, mutate)"
              />
              <v-btn
                :disabled="loading || !email || !pass"
                label="Login"
                :block="true"
                color="accent"
                @click="clickLogin(mutate)"
                >Login</v-btn
              >
              <v-progress-circular :indeterminate="true" v-if="loading" />
              <router-link to="/initreset">Forgot Password?</router-link>
            </v-container>
            <v-container v-else-if="resetPage">
              <ApolloMutation
                :mutation="require('../graphql/RequestPasswordReset.gql')"
                :variables="{
                  email,
                }"
                @done="initReset"
                @error="initReset({ data: { requestPasswordReset: false } })"
              >
                <template v-slot="{ mutate, loading /*, error*/ }">
                  <v-text-field
                    v-model="email"
                    label="Email"
                    @keypress="(e) => key(e, mutate)"
                  />
                  <v-btn
                    :disabled="loading || !email"
                    label="Forgot Password"
                    :block="true"
                    color="accent"
                    @click="mutate()"
                    >Forgot Password</v-btn
                  >
                  <v-progress-circular :indeterminate="true" v-if="loading" />
                </template>
              </ApolloMutation>
              <router-link to="/">Back to Login</router-link>
            </v-container>
            <v-container v-else>
              <ApolloMutation
                :mutation="require('../graphql/CompletePasswordReset.gql')"
                :variables="{
                  email,
                  code: resetCode,
                  newPass: newPassword,
                }"
                @done="completeReset"
                @error="
                  completeReset({ data: { completePasswordReset: false } })
                "
              >
                <template v-slot="{ mutate, loading /*, error*/ }">
                  <v-text-field
                    v-model="email"
                    label="Email"
                    @keypress="(e) => key(e, mutate)"
                  />
                  <v-text-field
                    v-model="resetCode"
                    label="Password Reset Code"
                    @keypress="(e) => key(e, mutate)"
                  />
                  <v-text-field
                    v-model="newPasswordOne"
                    label="New Password"
                    type="password"
                    @keypress="(e) => key(e, mutate)"
                  />
                  <v-text-field
                    v-model="newPasswordTwo"
                    label="Confirm New Password"
                    type="password"
                    @keypress="(e) => key(e, mutate)"
                  />
                  <v-btn
                    :disabled="loading || !newPassword || !resetCode"
                    label="Set New Password"
                    :block="true"
                    color="accent"
                    @click="mutate()"
                    >Set New Password</v-btn
                  >
                  <v-progress-circular :indeterminate="true" v-if="loading" />
                </template>
              </ApolloMutation>
              <router-link to="/">Back to Login</router-link>
            </v-container>
          </template>
        </ApolloMutation>
      </div>
      <v-snackbar v-if="failedLogin" v-model="failedLogin" color="error">
        Couldn't login, try again.
        <template v-slot:action="{ attrs }">
          <v-btn v-bind="attrs" @click="failedLogin = false">OK</v-btn>
        </template>
      </v-snackbar>
      <v-snackbar v-if="failedReset" v-model="failedReset" color="error"
        >Could not reset password, try again.
        <template v-slot:action="{ attrs }">
          <v-btn v-bind="attrs" @click="failedReset = false"
            >OK</v-btn
          ></template
        >
      </v-snackbar>
      <v-snackbar v-if="successReset" v-model="successReset" color="accent"
        >Password reset successfully. Please login.
        <template v-slot:action="{ attrs }">
          <v-btn v-bind="attrs" @click="successReset = false"
            >OK</v-btn
          ></template
        >
      </v-snackbar>
      <v-snackbar v-if="initedReset" v-model="initedReset" color="accent"
        >A password reset code has been sent to your email, enter it to reset
        your password.
        <template v-slot:action="{ attrs }">
          <v-btn v-bind="attrs" @click="initedReset = false"
            >OK</v-btn
          ></template
        >
      </v-snackbar>
    </template>
  </ApolloQuery>
</template>
<script lang="ts">
import Vue from "vue";

export default Vue.extend({
  name: "Login",
  data: function () {
    return {
      email: this.$route.query.email || "",
      pass: "",
      failedLogin: false,
      initedReset: false,
      failedReset: false,
      successReset: false,
      resetCode: this.$route.query.code,
      newPasswordOne: "",
      newPasswordTwo: "",
    };
  },
  computed: {
    resetPage: function () {
      return this.$route.path == "/initreset";
    },
    completePage: function () {
      return this.$route.path == "/passreset";
    },
    newPassword: function (): string | null {
      if (this.newPasswordOne == this.newPasswordTwo) {
        return this.newPasswordOne;
      } else {
        return null;
      }
    },
  },
  methods: {
    clickLogin(mutate: () => void) {
      this.failedLogin = false;
      mutate();
    },
    loggedin({ data: { login } }: { data: { login: boolean } }) {
      this.failedLogin = !login;
    },
    initReset({
      data: { requestPasswordReset },
    }: {
      data: { requestPasswordReset: boolean };
    }) {
      if (requestPasswordReset) {
        this.initedReset = true;
        this.newPasswordOne = "";
        this.newPasswordTwo = "";
        this.$router.push("/passreset");
      }
    },
    completeReset({
      data: { completePasswordReset },
    }: {
      data: { completePasswordReset: boolean };
    }) {
      if (completePasswordReset) {
        this.successReset = true;
        this.pass = "";
        this.$router.push("/");
      } else {
        this.failedReset = true;
      }
    },
    key({ key }: { key: string }, mutate: () => void) {
      if (key == "Enter") {
        mutate();
      }
    },
  },
});
</script>
