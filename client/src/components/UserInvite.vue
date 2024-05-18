<template>
  <div class="invite">
    <ApolloMutation
      :mutation="UserInvite"
      :variables="{
        inviteCode,
        password,
        user: {
          name,
          email,
          notifyViaEmail,
          timeZoneId,
          admin: false,
          instanceId: newUUID,
        },
      }"
      @done="(result) => mutated(result)"
      @error="error = true"
    >
      <template #default="{ mutate, loading /*, error*/ }">
        <v-card>
          <v-card-title>Create User</v-card-title>
          <v-card-text>
            <v-text-field v-model="email" label="Email"></v-text-field>
            <v-text-field v-model="name" label="Name"></v-text-field>
            <v-text-field
              v-model="passwordOne"
              label="Password"
              type="password"
            ></v-text-field>
            <v-text-field
              v-model="passwordTwo"
              label="Confirm Password"
              type="password"
            ></v-text-field>
            <v-autocomplete
              v-model="timeZoneId"
              :items="tzs"
              label="Timezone"
              item-title="name"
              item-value="value"
            ></v-autocomplete>
            <v-checkbox
              v-model="notifyViaEmail"
              label="Notify of new questions via email?"
            ></v-checkbox>
          </v-card-text>
          <v-card-actions>
            <v-btn color="accent" @click="save(mutate)">Save</v-btn>
            <div v-if="loading">
              <v-progress-circular :indeterminate="true" />
            </div>
          </v-card-actions>
          <v-snackbar v-model="error" color="error"
            >Could not create user, try again.
            <template #actions="attrs">
              <v-btn v-bind="attrs" @click="error = false">OK</v-btn></template
            >
          </v-snackbar>
          <v-snackbar v-model="success" color="accent"
            >Created new user, please login.
            <template #actions="attrs">
              <v-btn v-bind="attrs" @click="success = false"
                >OK</v-btn
              ></template
            >
          </v-snackbar>
        </v-card>
      </template>
    </ApolloMutation>
  </div>
</template>
<script lang="ts">
import moment from "moment-timezone";
import { FetchResult } from "@apollo/client/core";
import { User } from "@/generated/types.d";
import UserInvite from "@/graphql/UserInvite.gql";

export default {
  name: "UserInvite",
  data: () => ({
    passwordOne: "",
    passwordTwo: "",
    email: "",
    name: "",
    notifyViaEmail: false,
    timeZoneId: "Autodetect",
    error: false,
    success: false,
    tzs: [
      {
        name: `Autodetect -- ${moment.tz.guess()} (${moment
          .tz(moment.tz.guess())
          .zoneName()})`,
        value: "Autodetect",
      },
    ].concat(
      moment.tz.names().map((name) => ({
        name: `${name} (${moment.tz(name).zoneName()})`,
        value: name,
      })),
    ),
    UserInvite,
  }),
  computed: {
    inviteCode(): string {
      const path = this.$route.path;
      return path.slice(path.lastIndexOf("/") + 1);
    },
    newUUID(): string {
      return crypto.randomUUID();
    },
    password(): string | null {
      return this.passwordOne == this.passwordTwo && this.passwordOne.length > 2
        ? this.passwordOne
        : null;
    },
  },
  methods: {
    save(mutate: () => void) {
      this.error = false;
      mutate();
    },
    mutated(result: FetchResult<Record<string, User>>) {
      if (result.data && result.data["createUser"]) {
        this.$router.push("/");
      } else {
        this.error = true;
      }
    },
  },
};
</script>
