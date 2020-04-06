<template>
  <div class="user">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        result => {
          this.name = result.data.user.name;
          this.setTz(result.data.user.timeZoneId);
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-container v-else-if="data.user">
          <v-text-field
            v-model="data.user.email"
            label="Email"
            disabled
          ></v-text-field>
          <ApolloMutation
            :mutation="require('../graphql/UpdateUser.gql')"
            :variables="{
              name,
              id: data.user.id,
              instanceId: data.user.instanceId,
              email: data.user.email,
              admin: data.user.admin,
              timeZoneId: timezone
            }"
            :refetch-queries="() => [`currentUser`]"
            :await-refetch-queries="true"
            @error="done(false)"
            @done="done(true)"
          >
            <template v-slot="{ mutate, loading, error }">
              <v-text-field
                v-model="name"
                label="Name"
                @keypress="e => key(e, mutate)"
              ></v-text-field>
              <v-autocomplete
                :items="tzs"
                label="Timezone"
                v-model="timezone"
                item-text="name"
                item-value="value"
              ></v-autocomplete>
              <v-btn @click="mutate()" color="accent">Save</v-btn>
              <div v-if="loading">
                <v-progress-circular :indeterminate="true" />
              </div>
              <v-snackbar v-model="saveError"
                >An error occurred {{ error }}</v-snackbar
              >
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
    name: "",
    timezone: "",
    tzs: [
      {
        name: `Autodetect -- ${moment.tz.guess()} (${moment
          .tz(moment.tz.guess())
          .zoneName()})`,
        value: "Autodetect"
      }
    ].concat(
      moment.tz.names().map(name => ({
        name: `${name} (${moment.tz(name).zoneName()})`,
        value: name
      }))
    )
  }),
  methods: {
    done(success: boolean) {
      this.saveError = !success;
    },
    key({ key }: { key: string }, mutate: Function) {
      if (key == "Enter") {
        mutate();
      }
    },
    setTz(tz: string) {
      if (this.tzs.map(x => x.value).indexOf(tz) > -1) {
        this.timezone = tz;
      } else {
        this.timezone = "Autodetect";
      }
    }
  }
});
</script>
