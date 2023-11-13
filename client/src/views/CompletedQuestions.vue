<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) =>
          result && result.data && result.data.user && setUser(result.data.user)
      "
    >
      <template #default="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/CompletedQuestions.gql')"
      :variables="qvars"
      fetch-policy="cache-and-network"
      @result="
        (result) => {
          completedQuestions = result.data.closedQuestions;
        }
      "
    >
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.closedQuestions">
          <v-card-title>Completed Questions</v-card-title>
          <v-data-table
            :items="data.closedQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No completed questions found"
            @click:row="clickRow"
          >
            <template #item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template #item.answer="{ item, value }">
              {{ findAnswer(item, value, item.answer) }}
            </template>
            <template #item.response.response="{ item, value }">
              {{ findAnswer(item, value, value || "&mdash;") }}
            </template>
            <template #item.response.grade.correct="{ value }">
              <v-icon v-if="value === true" color="green darken-2"
                >mdi-check-circle
              </v-icon>
              <v-icon v-else-if="value === false" color="red darken-2"
                >mdi-close-circle
              </v-icon>
              <v-icon v-else color="grey darken-2">mdi-help-circle</v-icon>
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
    <graded-question-dialog
      v-model="detailDialog"
      :question="clickedQuestion"
      :user-t-z="userTZ"
    />
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";
import { ApiQuestion, ApiUser, QuestionType } from "@/generated/types.d";
import GradedQuestionDialog from "@/components/GradedQuestionDialog.vue";
import { useInstanceStore } from "@/stores/instance";

export default Vue.extend({
  name: "CompletedQuestions",
  components: { GradedQuestionDialog },
  setup() {
    const instanceStore = useInstanceStore();
    return {
      instanceStore,
    };
  },
  data: () => ({
    userTZ: "Autodetect",
    detailDialog: false,
    clickedQuestion: null as ApiQuestion | null,
    questionType: QuestionType.ShortAnswer,
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
  computed: {
    qvars() {
      const season = this.instanceStore.season;
      return {
        startTime: season?.startTime,
        endTime: season?.endTime,
      };
    },
  },
  methods: {
    renderDate(value: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(value, moment.ISO_8601, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    setUser(user: ApiUser) {
      if (user.timeZoneId) {
        this.userTZ = user.timeZoneId;
      }
      this.questionType = user.instance.defaultQuestionType;
    },
    clickRow(item: ApiQuestion) {
      this.clickedQuestion = item;
      this.detailDialog = true;
    },
    shortAnswer(): boolean {
      return this.clickedQuestion?.type == QuestionType.ShortAnswer;
    },
    findAnswer(
      item: ApiQuestion,
      letter: string,
      defaultValue: string,
    ): string {
      if (item.type == QuestionType.MultipleChoice) {
        const correctAnswer = item.answerChoices?.find(
          (choice) => choice.letter == letter,
        );
        if (correctAnswer) {
          return correctAnswer.letter + ": " + correctAnswer.answer;
        }
      }
      return defaultValue;
    },
  },
});
</script>
