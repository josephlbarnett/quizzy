import gql from "graphql-tag";
<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <div class="d-flex align-center">
        <v-img
          alt="Quizzy"
          class="shrink"
          contain
          src="./assets/logo.png"
          transition="scale-transition"
          width="40"
        />
      </div>
      <ApolloQuery :query="require('@/graphql/CurrentUser.gql')">
        <template v-slot="{ result: { error, data } /*, isLoading*/ }">
          <div v-if="data && data.user">
            <v-btn-toggle>
              <v-btn to="/" color="accent">
                <span>current questions</span>
              </v-btn>
              <v-btn to="/review" color="accent">
                <span>completed questions</span>
              </v-btn>
              <v-btn
                to="/write"
                :outlined="true"
                color="error"
                v-if="data.user.admin"
              >
                <span>future questions</span>
              </v-btn>
              <v-btn
                to="/grade"
                :outlined="true"
                color="error"
                v-if="data.user.admin"
              >
                <span>grading</span>
              </v-btn>
              <v-btn
                to="/users"
                :outlined="true"
                color="error"
                v-if="data.user.admin"
              >
                <span>users</span>
              </v-btn>
            </v-btn-toggle>
          </div>
          <div v-else>
            Please login.
          </div>
        </template>
      </ApolloQuery>
      <v-spacer />
      <ApolloQuery :query="require('@/graphql/CurrentUser.gql')">
        <template v-slot="{ result: { error, data } /*, isLoading*/ }">
          <div v-if="data && data.user">
            <ApolloMutation
              :mutation="require('@/graphql/Logout.gql')"
              :refetch-queries="() => [`currentUser`]"
              :await-refetch-queries="true"
            >
              <template v-slot="{ mutate, loading /*, error*/ }">
                <v-btn-toggle>
                  <v-btn text to="/me">{{ data.user.name }}</v-btn>
                  <v-btn
                    :disabled="loading"
                    label="Logout"
                    @click="doLogout(mutate)"
                    color="error"
                    outlined
                    >Logout</v-btn
                  >
                </v-btn-toggle>
              </template>
            </ApolloMutation>
          </div>
        </template>
      </ApolloQuery>
    </v-app-bar>

    <v-content>
      <Login> </Login>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Login from "@/components/Login.vue";

export default Vue.extend({
  name: "App",
  data: () => ({}),
  components: {
    Login,
  },
  methods: {
    doLogout(mutate: Function) {
      this.$apollo.getClient().resetStore();
      mutate();
    },
  },
});
</script>
