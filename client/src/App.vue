<template>
  <v-app>
    <ApolloQuery
      :query="require('@/graphql/CurrentUser.gql')"
      @result="(result) => setTitle(result)"
    >
      <template #default="{ result: { /*error,*/ data }, isLoading }">
        <v-app-bar app color="primary" dark>
          <v-app-bar-nav-icon
            v-if="data && data.user"
            @click="navDrawMini = !navDrawMini"
          ></v-app-bar-nav-icon>
          <span v-else-if="!isLoading">Please login.</span>
          <v-spacer />
          <v-menu v-if="data && data.user" open-on-hover>
            <template #activator="{ on }">
              <v-icon v-on="on">mdi-account</v-icon>
            </template>
            <v-list>
              <v-list-item text to="/me">
                <v-list-item-icon
                  ><v-icon>mdi-account</v-icon></v-list-item-icon
                >
                <v-list-item-title>{{ data.user.name }}</v-list-item-title>
              </v-list-item>
              <ApolloMutation
                :mutation="require('@/graphql/Logout.gql')"
                :refetch-queries="() => [`CurrentUser`]"
                :await-refetch-queries="true"
                @done="loggedOut"
              >
                <template #default="{ mutate, loading /*, error*/ }">
                  <v-list-item
                    :disabled="loading"
                    label="Logout"
                    color="error"
                    outlined
                    @click="doLogout(mutate)"
                  >
                    <v-list-item-icon
                      ><v-icon>mdi-logout-variant</v-icon></v-list-item-icon
                    >
                    <v-list-item-title>Logout</v-list-item-title></v-list-item
                  >
                </template>
              </ApolloMutation></v-list
            >
          </v-menu>
        </v-app-bar>
        <v-navigation-drawer
          v-if="data && data.user"
          :mini-variant="navDrawMini"
          app
          stateless
          permanent
        >
          <v-list nav>
            <v-list-item two-line @click="navDrawMini = !navDrawMini">
              <v-list-item-icon>
                <v-img alt="Quizzy" :src="logo" width="24" />
              </v-list-item-icon>
              <v-list-item-title>{{
                data.user.instance.name
              }}</v-list-item-title>
            </v-list-item>
            <v-divider />
            <v-list-item
              v-for="navLink in commonLinks"
              :key="navLink.link"
              :to="navLink.link"
              color="accent"
              @click="navDrawMini = true"
            >
              <v-list-item-icon>
                <v-tooltip :disabled="!navDrawMini" bottom>
                  <template #activator="{ on }">
                    <v-icon v-on="on">{{ navLink.icon }}</v-icon></template
                  >
                  {{ navLink.title }}
                </v-tooltip></v-list-item-icon
              >
              <v-list-item-title>{{ navLink.title }}</v-list-item-title>
            </v-list-item>
            <span v-if="data && data.user && data.user.admin">
              <v-divider />
              <v-list-item
                v-for="navLink in adminLinks"
                :key="navLink.link"
                :to="navLink.link"
                :outlined="true"
                color="error"
                @click="navDrawMini = true"
              >
                <v-list-item-icon>
                  <v-tooltip :disabled="!navDrawMini" bottom>
                    <template #activator="{ on }">
                      <v-icon v-on="on">{{ navLink.icon }}</v-icon></template
                    >
                    {{ navLink.title }}
                  </v-tooltip></v-list-item-icon
                >
                <v-list-item-title>{{ navLink.title }}</v-list-item-title>
              </v-list-item></span
            >
          </v-list>
        </v-navigation-drawer>
        <v-main>
          <Login> </Login>
        </v-main>
      </template>
    </ApolloQuery>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Login from "@/components/Login.vue";
import logo from "@/assets/logo.png";

export default Vue.extend({
  name: "App",
  components: {
    Login,
  },
  data: () => ({
    navDrawMini: true,
    logo: logo,
    commonLinks: [
      { title: "Current Questions", link: "/", icon: "mdi-file-find" },
      { title: "Completed Questions", link: "/review", icon: "mdi-history" },
    ],
    adminLinks: [
      { title: "Future Questions", link: "/write", icon: "mdi-pencil" },
      {
        title: "Grading",
        link: "/grade",
        icon: "mdi-checkbox-multiple-marked-circle",
      },
      { title: "Users", link: "/users", icon: "mdi-account-multiple" },
    ],
  }),
  methods: {
    doLogout(mutate: () => void) {
      this.$apollo.getClient().resetStore();
      mutate();
    },
    loggedOut() {
      window.location.reload();
    },
    setTitle(obj: { data: { user: { instance: { name: string } } } }) {
      if (obj.data && obj.data.user && obj.data.user.instance) {
        document.title = obj.data.user.instance.name;
      }
    },
  },
});
</script>
