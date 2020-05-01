<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="(result) => setTZ(result.data.user.timeZoneId)"
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/CompletedQuestions.gql')"
      @result="
        (result) => {
          this.completedQuestions = result.data.closedQuestions;
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.closedQuestions">
          <v-card-title>
            Completed Questions
          </v-card-title>
          <v-data-table
            :items="data.closedQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No completed questions found"
          >
            <template v-slot:item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template v-slot:item.response.grade.correct="{ value }">
              <v-icon v-if="value === true" color="green darken-2"
                >mdi-check-circle</v-icon
              >
              <v-icon v-if="value === false" color="red darken-2"
                >mdi-close-circle</v-icon
              >
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
  name: "CompletedQuestions",
  data: () => ({
    userTZ: "Autodetect",
    completedQuestions: [],
    headers: [
      {
        text: "Date",
        value: "activeAt",
        sortable: false,
      },
      {
        text: "Question",
        value: "body",
        sortable: false,
      },
      {
        text: "Answer Key",
        value: "answer",
        sortable: false,
      },
      {
        text: "Your Response",
        value: "response.response",
        sortable: false,
      },
      {
        text: "Correct",
        value: "response.grade.correct",
        sortable: false,
      },
      {
        text: "Score",
        value: "response.grade.score",
        sortable: false,
      },
    ],
  }),
  methods: {
    renderDate(value: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(value, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    setTZ(tz: string) {
      this.userTZ = tz;
    },
  },
});
</script>
