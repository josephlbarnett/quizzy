<template>
  <v-app>
    <ApolloQuery
      :query="CurrentUser"
      :variables="{ endTime: now }"
      @result="(result) => setTitle(result)"
    >
      <template #default="{ result: { /*error,*/ data }, isLoading }">
        <v-app-bar color="primary">
          <v-app-bar-nav-icon
            v-if="data && data.user"
            @click="navDrawMini = !navDrawMini"
          ></v-app-bar-nav-icon>
          <span v-else-if="!isLoading">Please login.</span>
          <v-spacer />
          <v-menu v-if="data && data.user" open-on-hover>
            <template #activator="{ props }">
              <v-icon v-bind="props">mdi-account</v-icon>
            </template>
            <v-list>
              <v-list-item text to="/me">
                <template #prepend>
                  <v-icon>mdi-account</v-icon>
                </template>
                <v-list-item-title>{{ data.user.name }}</v-list-item-title>
              </v-list-item>
              <ApolloMutation
                :mutation="Logout"
                :refetch-queries="() => [`CurrentUser`]"
                :await-refetch-queries="true"
              >
                <template #default="{ mutate, loading /*, error*/ }">
                  <v-list-item
                    :disabled="loading"
                    label="Logout"
                    color="error"
                    outlined
                    @click="doLogout(mutate)"
                  >
                    <template #prepend>
                      <v-icon>mdi-logout-variant</v-icon>
                    </template>
                    <v-list-item-title>Logout</v-list-item-title>
                  </v-list-item>
                </template>
              </ApolloMutation>
            </v-list>
          </v-menu>
        </v-app-bar>
        <v-navigation-drawer
          v-if="data && data.user"
          :rail="navDrawMini"
          permanent
        >
          <v-list nav>
            <v-list-item @click="navDrawMini = !navDrawMini">
              <template #prepend>
                <v-img alt="Quizzy" :src="logo" width="24" />
              </template>
              <v-list-item-title
                >{{ data.user.instance.name }}
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <template #prepend>
                <v-tooltip :disabled="!navDrawMini" location="bottom">
                  <template #activator="{ props }">
                    <v-icon v-bind="props" @click="navDrawMini = !navDrawMini"
                      >mdi-calendar</v-icon
                    >
                  </template>
                  {{ instanceStore.season?.name }}
                </v-tooltip>
              </template>
              <v-list-item-title>
                <season-selector @change="navDrawMini = true" />
              </v-list-item-title>
            </v-list-item>
            <v-divider />
            <v-list-item
              v-for="navLink in commonLinks"
              :key="navLink.link"
              :to="navLink.link"
              color="accent"
              @click="navDrawMini = true"
            >
              <template #prepend>
                <v-tooltip :disabled="!navDrawMini" location="bottom">
                  <template #activator="{ props }">
                    <v-icon v-bind="props">{{ navLink.icon }}</v-icon>
                  </template>
                  {{ navLink.title }}
                </v-tooltip>
              </template>
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
                <template #prepend>
                  <v-tooltip :disabled="!navDrawMini" location="bottom">
                    <template #activator="{ props }">
                      <v-icon v-bind="props">{{ navLink.icon }}</v-icon>
                    </template>
                    {{ navLink.title }}
                  </v-tooltip>
                </template>
                <v-list-item-title>{{ navLink.title }}</v-list-item-title>
              </v-list-item>
            </span>
          </v-list>
        </v-navigation-drawer>
        <v-main>
          <Login />
        </v-main>
      </template>
    </ApolloQuery>
  </v-app>
</template>

<script lang="ts">
import { useTheme } from "vuetify";
import Login from "@/components/Login.vue";
import SeasonSelector from "@/components/SeasonSelector.vue";
import CurrentUser from "@/graphql/CurrentUser.gql";
import Logout from "@/graphql/Logout.gql";
import logo from "@/assets/logo.png";
import { useInstanceStore } from "@/stores/instance";
import { ApiInstance } from "@/generated/types";
import moment from "moment-timezone"; //import { useTheme } from "vuetify";
//import { useTheme } from "vuetify";

//const theme = useTheme();
export default {
  name: "App",
  components: {
    Login,
    SeasonSelector,
  },
  setup() {
    const theme = useTheme();
    return { instanceStore: useInstanceStore(), theme };
  },
  data: () => ({
    navDrawMini: true,
    logo: logo,
    commonLinks: [
      { title: "Current Questions", link: "/", icon: "mdi-file-find" },
      { title: "Completed Questions", link: "/review", icon: "mdi-history" },
      { title: "Pop Quiz", link: "/quiz", icon: "mdi-auto-fix" },
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
    CurrentUser,
    Logout,
  }),
  computed: {
    now() {
      return moment().format();
    },
  },
  mounted() {
    if (window.matchMedia) {
      this.theme.global.name.value = window.matchMedia(
        "(prefers-color-scheme: dark)",
      ).matches
        ? "dark"
        : "light";
    }
  },
  methods: {
    async doLogout(mutate: () => Promise<void>) {
      await mutate();
      await this.$apollo.getClient().resetStore();
    },
    setTitle(obj: { data: { user: { instance: ApiInstance } } }) {
      if (obj.data && obj.data.user && obj.data.user.instance) {
        document.title = obj.data.user.instance.name;
        this.instanceStore.setInstance(obj.data.user.instance);
      }
    },
  },
};
</script>
