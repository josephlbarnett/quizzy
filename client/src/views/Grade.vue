<template>
  <div class="grade">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) => {
          setTZ(result.data.user.timeZoneId);
          this.userId = result.data.user.id;
        }
      "
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/Grader.gql')"
      :variables="{
        includeGraded: !hideGraded,
      }"
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <v-card v-if="data && data.responses">
          <v-card-title>
            Responses to grade
            <v-spacer />
            <v-checkbox v-model="hideGraded" />Hide graded responses
          </v-card-title>
          <div v-if="isLoading">
            <v-progress-circular :indeterminate="true" />
          </div>
          <div v-else-if="error" class="error">An error occurred</div>
          <v-data-table
            :items="data.responses"
            :headers="headers"
            item-key="id"
            no-data-text="No responses  to grade found"
            @click:row="clickRow"
          >
            <template v-slot:item.question.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template v-slot:item.correct="{ value }">
              {{
                value === true ? "YES" : value === false ? "NO" : "NOT GRADED"
              }}
            </template>
            <template v-slot:item.bonus="{ item }">
              {{ renderScore(item) }}
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>

    <!--    <v-dialog v-model="responseDialog">-->
    <!--      <v-card>-->
    <!--        <v-card-title>Question </v-card-title>-->
    <!--        <ApolloMutation-->
    <!--          v-if="clickedQuestion"-->
    <!--          :mutation="require('../graphql/SaveResponse.gql')"-->
    <!--          :refetch-queries="() => [`currentQuestions`]"-->
    <!--          :await-refetch-queries="true"-->
    <!--          :variables="{-->
    <!--            questionId: clickedQuestion.id,-->
    <!--            response: clickedResponse ? clickedResponse.response : '',-->
    <!--            ruleReferences: clickedResponse-->
    <!--              ? clickedResponse.ruleReferences-->
    <!--              : '',-->
    <!--            id: clickedResponse ? clickedResponse.id : null,-->
    <!--            userId,-->
    <!--          }"-->
    <!--          @error="saveError = true"-->
    <!--        >-->
    <!--          <template v-slot="{ mutate, loading }">-->
    <!--            <v-card-text>-->
    <!--              {{ clickedQuestion.body }}-->
    <!--              <v-text-field-->
    <!--                label="Response"-->
    <!--                v-model="clickedResponse.response"-->
    <!--              ></v-text-field>-->
    <!--              <v-text-field-->
    <!--                label="Rule Reference"-->
    <!--                v-model="clickedResponse.ruleReferences"-->
    <!--              ></v-text-field>-->
    <!--            </v-card-text>-->
    <!--            <v-card-actions>-->
    <!--              <v-btn @click="responseDialog = false">CANCEL</v-btn>-->
    <!--              <v-btn @click="saveResponse(mutate)" color="accent"-->
    <!--                >save response</v-btn-->
    <!--              >-->
    <!--              <v-progress-circular :indeterminate="true" v-if="loading"-->
    <!--            /></v-card-actions>-->
    <!--            <v-snackbar :top="true" v-model="saveError">-->
    <!--              Couldn't save, try again.-->
    <!--              <v-btn @click="saveError = false">OK</v-btn>-->
    <!--            </v-snackbar>-->
    <!--          </template>-->
    <!--        </ApolloMutation>-->
    <!--      </v-card>-->
    <!--    </v-dialog>-->
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";

export default Vue.extend({
  name: "Grader",
  data: () => ({
    userTZ: "Autodetect",
    headers: [
      {
        text: "Question",
        value: "question.body",
        sortable: false,
      },
      {
        text: "Date",
        value: "question.activeAt",
        sortable: false,
      },
      {
        text: "User",
        value: "user.name",
        sortable: false,
      },
      {
        text: "Response",
        value: "response",
        sortable: false,
      },
      {
        text: "Correct",
        value: "correct",
        sortable: false,
      },
      {
        text: "Score",
        value: "bonus",
        sortable: false,
      },
    ],
    gradeDialog: false,
    clickedQuestion: null as object | null,
    clickedResponse: {},
    userId: "",
    saveError: false,
    hideGraded: true,
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
    },
    clickRow(item: { response: { response: string; ruleReferences: string } }) {
      this.clickedQuestion = item;
      this.clickedResponse = item.response || {};
      this.gradeDialog = true;
    },
    saveResponse(mutate: Function) {
      mutate();
      this.gradeDialog = false;
    },
    renderScore(item: { correct: boolean | null; bonus: number }) {
      if (item && item.correct === true) {
        return 15 + item.bonus;
      } else if (!item || item.correct == null) {
        return "";
      } else {
        return 0;
      }
    },
  },
});
</script>
