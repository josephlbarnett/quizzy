<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="result => setTZ(result.data.user.timeZoneId)"
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/CompletedQuestions.gql')"
      @result="
        result => {
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
            Current Questions
          </v-card-title>
          <v-data-table
            :items="data.closedQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No active questions found"
          >
            <template v-slot:item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template v-slot:item.response.correct="{ value }">
              {{
                value === true ? "YES" : value === false ? "NO" : "NOT GRADED"
              }}
            </template>
            <template v-slot:item.response.bonus="{ item }">
              {{ renderScore(item) }}
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
        text: "Answer Key",
        value: "answer",
        sortable: false
      },
      {
        text: "Your Response",
        value: "response.response",
        sortable: false
      },
      {
        text: "Correct",
        value: "response.correct",
        sortable: false
      },
      {
        text: "Score",
        value: "response.bonus",
        sortable: false
      }
    ]
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
    renderScore(item: {
      response: { correct: boolean | null; bonus: number };
    }) {
      if (item.response && item.response.correct === true) {
        return 5 + item.response.bonus;
      } else if (!item.response || item.response.correct == null) {
        return "";
      } else {
        return 0;
      }
    },
    setTZ(tz: string) {
      this.userTZ = tz;
    }
  }
});
</script>
