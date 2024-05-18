<template>
  <div class="user">
    <ApolloQuery
      :query="CurrentUser"
      @result="
        (result) => {
          if (result && result.data && result.data.user) {
            name = result.data.user.name;
            notifyViaEmail = result.data.user.notifyViaEmail;
            setTz(result.data.user.timeZoneId);
          }
        }
      "
    >
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="bg-error">An error occurred</div>
        <v-container v-else-if="data.user">
          <ApolloMutation
            :mutation="UpdateUser"
            :variables="{
              name,
              id: data.user.id,
              instanceId: data.user.instanceId,
              email: data.user.email,
              admin: data.user.admin,
              timeZoneId: timezone,
              notifyViaEmail: notifyViaEmail,
            }"
            :refetch-queries="() => [`CurrentUser`]"
            :await-refetch-queries="true"
            @error="done(false)"
            @done="done(true)"
          >
            <template #default="{ mutate, loading }">
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
                        v-model="timezone"
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
                      <v-btn color="accent" @click="mutate()">Save</v-btn>
                      <div v-if="loading">
                        <v-progress-circular :indeterminate="true" />
                      </div>
                    </v-card-actions>
                    <v-snackbar v-model="saveError" color="error"
                      >Could not save settings, try again.
                      <template #actions="attrs">
                        <v-btn v-bind="attrs" @click="saveError = false"
                          >OK</v-btn
                        ></template
                      >
                    </v-snackbar>
                    <v-snackbar v-model="saveConfirm" color="accent"
                      >Settings saved.
                      <template #actions="attrs">
                        <v-btn v-bind="attrs" @click="saveConfirm = false"
                          >OK</v-btn
                        ></template
                      >
                    </v-snackbar>
                  </v-card>
                </v-col>
              </v-row>
            </template>
          </ApolloMutation>

          <ApolloMutation
            :mutation="ChangePassword"
            :variables="{
              old: oldPassword,
              new: newPassword,
            }"
            :refetch-queries="() => [`CurrentUser`]"
            :await-refetch-queries="true"
            @error="donePass({ data: { changePassword: false } })"
            @done="donePass"
          >
            <template #default="{ mutate, loading }">
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
                      <v-btn color="accent" @click="mutate()">Save</v-btn>
                      <div v-if="loading">
                        <v-progress-circular :indeterminate="true" />
                      </div>
                    </v-card-actions>
                    <v-snackbar v-model="passError" color="error"
                      >Could not change password, try again.
                      <template #actions="attrs">
                        <v-btn v-bind="attrs" @click="passError = false"
                          >OK</v-btn
                        ></template
                      >
                    </v-snackbar>
                    <v-snackbar v-model="passConfirm" color="accent"
                      >Password changed.
                      <template #actions="attrs">
                        <v-btn v-bind="attrs" @click="passConfirm = false"
                          >OK
                        </v-btn></template
                      >
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
import moment from "moment-timezone";
import CurrentUser from "@/graphql/CurrentUser.gql";
import UpdateUser from "@/graphql/UpdateUser.gql";
import ChangePassword from "@/graphql/ChangePassword.gql";

export default {
  name: "UserSettings",
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
    notifyViaEmail: false,
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
    CurrentUser,
    UpdateUser,
    ChangePassword,
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
};
</script>
