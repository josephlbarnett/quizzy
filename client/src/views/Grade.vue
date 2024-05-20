<template>
  <div class="grade">
    <ApolloQuery
      :query="CurrentUser"
      @result="
        (result) => {
          result &&
            result.data &&
            result.data.user &&
            setTZ(result.data.user.timeZoneId);
          userId =
            result && result.data && result.data.user && result.data.user.id;
        }
      "
    >
      <template #default="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="Grader"
      fetch-policy="cache-and-network"
      :variables="{
        includeGraded: !hideGraded,
        ...qvars,
      }"
    >
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="bg-error">An error occurred</div>
        <v-card v-if="data && data.responses">
          <v-card-title>
            Responses to grade
            <v-spacer />
            <v-checkbox v-model="hideGraded" />
            Hide graded responses
          </v-card-title>
          <v-data-table
            :items="data.responses"
            :headers="headers"
            item-key="id"
            no-data-text="No responses to grade found"
            @click:row="clickRow"
          >
            <template #item.question.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template #item.response="{ item, value }">
              {{ findAnswer(item, value, item.response) }}
            </template>
            <template #item.grade.correct="{ value }">
              <v-icon v-if="value === true" color="green-darken-2"
                >mdi-check-circle
              </v-icon>
              <v-icon v-else-if="value === false" color="red-darken-2"
                >mdi-close-circle
              </v-icon>
              <v-icon v-else color="grey-darken-2">mdi-help-circle</v-icon>
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
    <v-dialog
      v-if="
        clickedResponse &&
        clickedResponse.question &&
        clickedResponse.question.author
      "
      v-model="gradeDialog"
    >
      <v-card>
        <ApolloMutation
          :refetch-queries="() => [`Grader`, `CompletedQuestions`, `Users`]"
          :mutation="Grade"
          :variables="{
            responseId: clickedResponse.id,
            bonus: clickedResponse.bonus || null,
            correct:
              clickedResponse.correct === 'correct'
                ? true
                : clickedResponse.correct === 'incorrect'
                  ? false
                  : null,
            id: clickedResponse.gradeId,
          }"
          :await-refetch-queries="true"
          @error="saveError = true"
          @done="gradeDialog = false"
        >
          <template #default="{ mutate, loading }">
            <v-card-title
              >Grade Response:
              {{ renderDate(clickedResponse.question.activeAt) }} by
              {{ clickedResponse.question.author.name }}
            </v-card-title>
            <v-card-text>
              <v-row>
                <v-col
                  v-if="clickedResponse.question?.imageUrl"
                  cols="12"
                  lg="1"
                >
                  <v-dialog v-model="imageDialog">
                    <template #activator="{ props }">
                      <v-img
                        :src="clickedResponse.question?.imageUrl"
                        max-height="200px"
                        max-width="200px"
                        v-bind="props"
                      ></v-img>
                    </template>
                    <v-card @click="imageDialog = false">
                      <v-img
                        cover
                        :src="clickedResponse.question?.imageUrl"
                        max-height="90vh"
                        max-width="90vw"
                      ></v-img>
                    </v-card>
                  </v-dialog>
                </v-col>
                <v-col align-self="center"
                  >{{ clickedResponse.question?.body }}
                </v-col>
              </v-row>
              <v-row v-if="shortAnswer()">
                <v-col cols="6">
                  <v-textarea
                    v-model="clickedResponse.response"
                    :readonly="true"
                    :label="clickedResponse.user.name + '\'s Response'"
                  />
                </v-col>
                <v-col cols="6">
                  <v-textarea
                    v-model="clickedResponse.question.answer"
                    :readonly="true"
                    label="Answer Key"
                  />
                </v-col>
              </v-row>
              <v-row v-else-if="clickedResponse.question">
                <v-col cols="12">
                  <v-radio-group
                    readonly
                    :model-value="clickedResponse.response"
                  >
                    <v-row
                      v-for="choice in clickedResponse.question.answerChoices"
                      :key="choice.letter"
                    >
                      <v-radio
                        :key="choice.letter"
                        :label="choice.letter + ': ' + choice.answer"
                        :value="choice.letter"
                        readonly
                      ></v-radio
                      >&nbsp;&nbsp;
                      <v-icon
                        v-if="
                          clickedResponse.response &&
                          choice.letter == clickedResponse.response &&
                          choice.letter == clickedResponse.question.answer
                        "
                        color="green-darken-2"
                        >mdi-check-circle
                      </v-icon>
                      <v-icon
                        v-if="
                          clickedResponse.response &&
                          choice.letter == clickedResponse.response &&
                          choice.letter != clickedResponse.question.answer
                        "
                        color="red-darken-2"
                        >mdi-close-circle
                      </v-icon>
                    </v-row>
                  </v-radio-group>
                  <v-text-field
                    v-model="correctAnswerChoice"
                    :readonly="true"
                    label="Answer Key"
                  />
                </v-col>
              </v-row>
              <v-row v-if="shortAnswer()">
                <v-col cols="6">
                  <v-textarea
                    v-model="clickedResponse.ruleReferences"
                    :readonly="true"
                    :label="clickedResponse.user.name + '\'s Rule References'"
                  />
                </v-col>
                <v-col cols="6">
                  <v-textarea
                    v-model="clickedResponse.question.ruleReferences"
                    :readonly="true"
                    label="Answer Key Rule References"
                  />
                </v-col>
              </v-row>
              <v-row v-else>
                <v-col cols="12">
                  <v-textarea
                    v-model="clickedResponse.question.ruleReferences"
                    :readonly="true"
                    label="Answer Key Rule References"
                  />
                </v-col>
              </v-row>
              <a :href="`mailto:${mailToLink(clickedResponse)}`">
                Email {{ clickedResponse.user.name }}
              </a>
              <v-radio-group v-model="clickedResponse.correct">
                <v-radio label="Ungraded" value="ungraded" />
                <v-radio label="Correct" value="correct" />
                <v-radio label="Incorrect" value="incorrect" />
              </v-radio-group>

              <v-text-field
                v-if="shortAnswer()"
                v-model.number="clickedResponse.bonus"
                type="number"
                min="0"
                max="5"
                label="Bonus points"
              />
            </v-card-text>
            <v-card-actions>
              <v-btn @click="gradeDialog = false">CANCEL</v-btn>
              <v-btn color="accent" @click="mutate()">GRADE</v-btn>
              <v-progress-circular v-if="loading" :indeterminate="true" />
            </v-card-actions>
            <v-snackbar v-model="saveError" color="error">
              Couldn't save, try again.
              <template #actions="attrs">
                <v-btn v-bind="attrs" @click="saveError = false">OK</v-btn>
              </template>
            </v-snackbar>
          </template>
        </ApolloMutation>
      </v-card>
    </v-dialog>
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import { ApiResponse, QuestionType } from "@/generated/types.d";
import { useInstanceStore } from "@/stores/instance";
import CurrentUser from "@/graphql/CurrentUser.gql";
import Grader from "@/graphql/Grader.gql";
import Grade from "@/graphql/Grade.gql";

export default {
  name: "ResponseGrader",
  setup() {
    return { instanceStore: useInstanceStore() };
  },
  data: () => ({
    userTZ: "Autodetect",
    headers: [
      {
        title: "Date",
        value: "question.activeAt",
        sortable: false,
      },
      {
        title: "Question",
        value: "question.body",
        sortable: false,
      },
      {
        title: "User",
        value: "user.name",
        sortable: false,
      },
      {
        title: "Response",
        value: "response",
        sortable: false,
      },
      {
        title: "Correct",
        value: "grade.correct",
        sortable: false,
      },
      {
        title: "Score",
        value: "grade.score",
        sortable: false,
      },
    ],
    userId: "",
    saveError: false,
    hideGraded: true,
    gradeDialog: false,
    imageDialog: false,
    clickedResponse: null as ApiResponse | null,
    CurrentUser,
    Grader,
    Grade,
  }),
  computed: {
    correctAnswerChoice(): string {
      if (this.clickedResponse?.question?.type == QuestionType.MultipleChoice) {
        const correctAnswer =
          this.clickedResponse?.question?.answerChoices?.find(
            (choice) => choice.letter == this.clickedResponse?.question?.answer,
          );
        if (correctAnswer) {
          return correctAnswer.letter + ": " + correctAnswer.answer;
        }
      }
      return "";
    },
    qvars() {
      const season = this.instanceStore.season;
      return {
        startTime: season?.startTime,
        endTime: season?.endTime,
      };
    },
  },
  methods: {
    renderDateTime(date: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, moment.ISO_8601, browserTZ);
      return `${zonedMoment.format("ddd, MMM D YYYY, h:mmA")} (${browserTZ})`;
    },
    renderDate(date: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, moment.ISO_8601, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    setTZ(tz: string | null) {
      if (tz) {
        this.userTZ = tz;
      }
    },
    clickRow(event: Event, { item }: { item: ApiResponse }) {
      const response = {
        bonus: item.grade && item.grade.bonus,
        correct:
          item.grade === null || item.grade?.correct === null
            ? "ungraded"
            : item.grade?.correct
              ? "correct"
              : "incorrect",
        question: Object.assign({}, item.question),
        questionId: item.questionId,
        user: Object.assign({}, item.user),
        userId: item.userId,
        id: item.id,
        response: item.response,
        ruleReferences: item.ruleReferences,
        gradeId: item.grade && item.grade.id,
      };
      this.clickedResponse = response;
      this.gradeDialog = true;
    },
    saveResponse(mutate: () => void) {
      mutate();
      this.gradeDialog = false;
    },
    color(value: boolean) {
      alert(value);
      return value ? "green" : "red";
    },
    mailToLink(clickedResponse: ApiResponse) {
      const subject = encodeURIComponent(
        `Re: ${this.renderDate(clickedResponse.question?.activeAt)} Question`,
      );
      const response = this.findAnswer(
        clickedResponse,
        clickedResponse.response,
        clickedResponse.response,
      );
      const quotedResponse = "> " + response.replace(/(?:\r\n|\r|\n)/g, "\n> ");
      const quotedRuleRefs =
        "> " +
        clickedResponse.ruleReferences.replace(/(?:\r\n|\r|\n)/g, "\n> ");
      const body = encodeURIComponent(`${quotedResponse}\n` + quotedRuleRefs);
      return `${clickedResponse.user?.email}?subject=${subject}&body=${body}`;
    },
    shortAnswer(): boolean {
      return this.clickedResponse?.question?.type == QuestionType.ShortAnswer;
    },
    findAnswer(
      item: ApiResponse,
      letter: string,
      defaultValue: string,
    ): string {
      if (item.question?.type == QuestionType.MultipleChoice) {
        const chosenAnswer = item.question?.answerChoices?.find(
          (choice) => choice.letter == letter,
        );
        if (chosenAnswer) {
          return chosenAnswer.letter + ": " + chosenAnswer.answer;
        }
      }
      return defaultValue;
    },
  },
};
</script>
