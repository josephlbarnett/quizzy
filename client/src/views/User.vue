<template>
  <div class="user">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) => {
          if (result && result.data && result.data.user) {
            this.name = result.data.user.name;
            this.setTz(result.data.user.timeZoneId);
          }
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-container v-else-if="data.user">
          <ApolloMutation
            :mutation="require('../graphql/UpdateUser.gql')"
            :variables="{
              name,
              id: data.user.id,
              instanceId: data.user.instanceId,
              email: data.user.email,
              admin: data.user.admin,
              timeZoneId: timezone,
            }"
            :refetch-queries="() => [`currentUser`]"
            :await-refetch-queries="true"
            @error="done(false)"
            @done="done(true)"
          >
            <template v-slot="{ mutate, loading }">
              <v-row>
                <v-col cols="12">
                  <v-card>
                    <v-card-title>User Settings</v-card-title>
                    <v-card-text>
                      <v-text-field
                        v-model="data.user.email"
                        label="Email"
                        disabled
                      ></v-text-field>
                      <v-text-field
                        v-model="name"
                        label="Name"
                        @keypress="(e) => key(e, mutate)"
                      ></v-text-field>
                      <v-autocomplete
                        :items="tzs"
                        label="Timezone"
                        v-model="timezone"
                        item-text="name"
                        item-value="value"
                      ></v-autocomplete>
                    </v-card-text>
                    <v-card-actions>
                      <v-btn @click="mutate()" color="accent">Save</v-btn>
                      <div v-if="loading">
                        <v-progress-circular :indeterminate="true" />
                      </div>
                    </v-card-actions>
                    <v-snackbar v-model="saveError" color="error"
                      >Could not save settings, try again.
                      <v-btn @click="saveError = false">OK</v-btn>
                    </v-snackbar>
                    <v-snackbar v-model="saveConfirm" color="accent"
                      >Settings saved.
                      <v-btn @click="saveConfirm = false">OK</v-btn>
                    </v-snackbar>
                  </v-card>
                </v-col>
              </v-row>
            </template>
          </ApolloMutation>

          <ApolloMutation
            :mutation="require('../graphql/ChangePassword.gql')"
            :variables="{
              old: oldPassword,
              new: newPassword,
            }"
            :refetch-queries="() => [`currentUser`]"
            :await-refetch-queries="true"
            @error="donePass({ data: { changePassword: false } })"
            @done="donePass"
          >
            <template v-slot="{ mutate, loading }">
              <v-row>
                <v-col cols="12">
                  <v-card>
                    <v-card-title>Change Password</v-card-title>
                    <v-card-text>
                      <v-text-field
                        v-model="oldPassword"
                        label="Current Password"
                        type="password"
                        @keypress="(e) => key(e, mutate)"
                      ></v-text-field>
                      <v-text-field
                        v-model="newPasswordOne"
                        label="New Password"
                        type="password"
                        @keypress="(e) => key(e, mutate)"
                      ></v-text-field>
                      <v-text-field
                        v-model="newPasswordTwo"
                        label="Confirm New Password"
                        type="password"
                        @keypress="(e) => key(e, mutate)"
                      ></v-text-field>
                    </v-card-text>
                    <v-card-actions>
                      <v-btn @click="mutate()" color="accent">Save</v-btn>
                      <div v-if="loading">
                        <v-progress-circular :indeterminate="true" />
                      </div>
                    </v-card-actions>
                    <v-snackbar v-model="passError" color="error"
                      >Could not change password, try again.
                      <v-btn @click="passError = false">OK</v-btn>
                    </v-snackbar>
                    <v-snackbar v-model="passConfirm" color="accent"
                      >Password changed.
                      <v-btn @click="passConfirm = false">OK </v-btn>
                    </v-snackbar>
                  </v-card>
                </v-col>
              </v-row>
            </template>
          </ApolloMutation>
        </v-container>
      </template>
    </ApolloQuery>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";

export default Vue.extend({
  name: "User",
  data: () => ({
    saveError: false,
    saveConfirm: false,
    passError: false,
    passConfirm: false,
    name: "",
    timezone: "",
    oldPassword: "",
    newPasswordOne: "",
    newPasswordTwo: "",
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
      }))
    ),
  }),
  computed: {
    newPassword: function (): string | null {
      if (this.newPasswordOne == this.newPasswordTwo) {
        return this.newPasswordOne;
      } else {
        return null;
      }
    },
  },
  methods: {
    done(success: boolean) {
      this.saveError = !success;
      this.saveConfirm = success;
    },
    donePass(ret: { data: { changePassword: boolean } }) {
      this.passError = !ret.data.changePassword;
      this.passConfirm = ret.data.changePassword;
      if (this.passConfirm) {
        this.newPasswordOne = "";
        this.newPasswordTwo = "";
        this.oldPassword = "";
      }
    },
    key({ key }: { key: string }, mutate: () => void) {
      if (key == "Enter") {
        mutate();
      }
    },
    setTz(tz: string) {
      if (this.tzs.map((x) => x.value).indexOf(tz) > -1) {
        this.timezone = tz;
      } else {
        this.timezone = "Autodetect";
      }
    },
  },
});
</script>
