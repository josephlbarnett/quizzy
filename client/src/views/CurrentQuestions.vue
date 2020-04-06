<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="result => setTZ(result.data.user.timeZoneId)"
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/CurrentQuestions.gql')"
      @result="
        result => {
          this.activeQuestions = result.data.activeQuestions;
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.activeQuestions">
          <v-card-title>
            Current Questions
          </v-card-title>
          <v-data-table
            :items="data.activeQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No active questions found"
          >
            <template v-slot:item.closedAt="{ value }">
              {{ renderDateTime(value) }}
            </template>
            <template v-slot:item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";

export default Vue.extend({
  name: "CurrentQuestions",
  data: () => ({
    userTZ: "Autodetect",
    activeQuestions: [],
    headers: [
      {
        text: "Question",
        value: "body",
        sortable: false
      },
      {
        text: "Date",
        value: "activeAt",
        sortable: false
      },
      {
        text: "Respond By",
        value: "closedAt",
        sortable: false
      },
      {
        text: "Your Response",
        value: "response.response",
        sortable: false
      }
    ]
  }),
  methods: {
    renderDateTime(date: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, browserTZ);
      return `${zonedMoment.format("ddd, MMM D YYYY, h:mmA")} (${browserTZ})`;
    },
    renderDate(date: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    setTZ(tz: string) {
      this.userTZ = tz;
    }
  }
});
</script>
