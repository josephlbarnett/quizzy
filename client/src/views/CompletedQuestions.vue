<template>
  <div class="home">
    <ApolloQuery
      :query="CurrentUser"
      @result="
        (result) =>
          result && result.data && result.data.user && setUser(result.data.user)
      "
    >
      <template #default="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="CompletedQuestions"
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
        <div v-else-if="error" class="bg-error">An error occurred</div>
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
            <template #item.percentCorrect="{ value, item }">
              <v-dialog>
                <template #activator="{ props }">
                  <v-chip v-bind="props" :color="getColor(value)"
                    >{{ value.toFixed(0) }}%</v-chip
                  >
                </template>
                <v-card>
                  <v-card-title
                    >Responses for Question: {{ renderDate(item.activeAt) }} by
                    {{ item.author?.name }}</v-card-title
                  >
                  <v-card-text>
                    <ApolloQuery
                      :query="QuestionResponses"
                      :variables="{ questionId: item.id }"
                    >
                      <template
                        #default="{
                          result: { error: responseError, data: responseData },
                          isLoading: responseIsLoading,
                        }"
                      >
                        <v-row>
                          <v-col v-if="item.imageUrl" cols="12" lg="1">
                            <v-dialog v-model="imageDialog">
                              <template #activator="{ props }">
                                <v-img
                                  :src="item.imageUrl"
                                  max-height="200px"
                                  max-width="200px"
                                  v-bind="props"
                                ></v-img>
                              </template>
                              <v-card @click="imageDialog = false">
                                <v-img
                                  cover
                                  :src="item.imageUrl"
                                  max-height="90vh"
                                  max-width="90vw"
                                ></v-img>
                              </v-card>
                            </v-dialog>
                          </v-col>
                          <v-col align-self="center">{{ item.body }}</v-col>
                        </v-row>
                        <v-row
                          v-for="choice in item.answerChoices"
                          :key="choice.letter"
                        >
                          {{ choice.letter }}: {{ choice.answer }}&nbsp;&nbsp;
                          <v-icon
                            v-if="choice.letter == item.answer"
                            color="green-darken-2"
                            >mdi-check-circle
                          </v-icon>
                        </v-row>
                        <v-data-table
                          v-if="responseData"
                          :items="responseData.questionResponses"
                          :headers="[
                            { title: 'Name', value: 'user.name' },
                            ...item.answerChoices.map((i) => ({
                              title: i.letter,
                              value: i.letter,
                            })),
                            // { title: 'Response', value: 'response' },
                            // { title: 'Correct', value: 'grade.correct' },
                          ]"
                          item-key="id"
                        >
                          <template #item.grade.correct="{ value: correct }">
                            <v-icon
                              v-if="correct === true"
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="correct === false"
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-help-circle</v-icon
                            >
                          </template>
                          <template #item.A="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'A' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'A' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                          <template #item.B="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'B' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'B' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                          <template #item.C="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'C' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'C' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                          <template #item.D="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'D' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'D' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                          <template #item.E="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'E' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'E' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                          <template #item.F="{ item: colItem }">
                            <v-icon
                              v-if="
                                colItem.response == 'F' && colItem.grade.correct
                              "
                              color="green-darken-2"
                              >mdi-check-circle
                            </v-icon>
                            <v-icon
                              v-else-if="
                                colItem.response == 'F' &&
                                !colItem.grade.correct
                              "
                              color="red-darken-2"
                              >mdi-close-circle
                            </v-icon>
                            <v-icon v-else color="grey-darken-2"
                              >mdi-minus-circle</v-icon
                            >
                          </template>
                        </v-data-table>
                        <v-progress-circular
                          v-else-if="responseIsLoading"
                          indeterminate
                        />
                        <div v-else-if="responseError" class="bg-error">
                          An error occurred
                        </div>
                      </template>
                    </ApolloQuery>
                  </v-card-text>
                </v-card>
              </v-dialog>
            </template>
            <template #item.answer="{ item, value }">
              {{ findAnswer(item, value, item.answer) }}
            </template>
            <template #item.response.response="{ item, value }">
              {{ findAnswer(item, value, value || "&mdash;") }}
            </template>
            <template #item.response.grade.correct="{ value }">
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
    <graded-question-dialog
      v-model="detailDialog"
      :question="clickedQuestion"
      :question-index="clickedQuestionIndex"
      :user-t-z="userTZ"
      :in-test="inTest"
      @next="next"
      @prev="prev"
    />
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import { ApiQuestion, ApiUser, QuestionType } from "@/generated/types.d";
import GradedQuestionDialog from "@/components/GradedQuestionDialog.vue";
import { useInstanceStore } from "@/stores/instance";
import CurrentUser from "@/graphql/CurrentUser.gql";
import CompletedQuestions from "@/graphql/CompletedQuestions.gql";
import QuestionResponses from "@/graphql/QuestionResponses.gql";

export default {
  name: "CompletedQuestions",
  components: { GradedQuestionDialog },
  props: {
    inTest: { type: Boolean, default: false },
  },
  setup() {
    const instanceStore = useInstanceStore();
    return {
      instanceStore,
    };
  },
  data: () => ({
    userTZ: "Autodetect",
    detailDialog: false,
    admin: false,
    clickedQuestion: null as ApiQuestion | null,
    clickedQuestionIndex: null as number | null,
    imageDialog: false,
    questionType: QuestionType.ShortAnswer,
    completedQuestions: [] as Array<ApiQuestion>,
    baseheaders: [
      {
        title: "Date",
        value: "activeAt",
        sortable: false,
      },
      {
        title: "Question",
        value: "body",
        sortable: false,
      },
      {
        title: "Answer Key",
        value: "answer",
        sortable: false,
      },
      {
        title: "Your Response",
        value: "response.response",
        sortable: false,
      },
      {
        title: "Correct",
        value: "response.grade.correct",
        sortable: false,
      },
      {
        title: "Score",
        value: "response.grade.score",
        sortable: false,
      },
    ],
    adminheaders: [
      {
        title: "Results",
        value: "percentCorrect",
        sortable: false,
      },
    ],
    CurrentUser,
    CompletedQuestions,
    QuestionResponses,
  }),
  computed: {
    qvars() {
      const season = this.instanceStore.season;
      return {
        startTime: season?.startTime,
        endTime: season?.endTime,
      };
    },
    headers() {
      if (this.admin) {
        return this.baseheaders.concat(this.adminheaders);
      } else {
        return this.baseheaders;
      }
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
      this.admin = user.admin;
    },
    clickRow(event: Event, { item }: { item: ApiQuestion }) {
      this.clickedQuestion = item;
      this.clickedQuestionIndex = this.completedQuestions.findIndex(
        (x) => x.id == item.id,
      );
      if (this.clickedQuestionIndex < 0) {
        this.clickedQuestionIndex = null;
      }
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
    prev() {
      if (
        this.clickedQuestionIndex != null &&
        this.completedQuestions.length > 1
      ) {
        this.clickedQuestionIndex--;
        if (this.clickedQuestionIndex < 0) {
          this.clickedQuestionIndex = this.completedQuestions.length - 1;
        }
        this.clickedQuestion =
          this.completedQuestions[this.clickedQuestionIndex];
      }
    },
    next() {
      if (
        this.clickedQuestionIndex != null &&
        this.completedQuestions.length > 1
      ) {
        this.clickedQuestionIndex++;
        if (this.clickedQuestionIndex >= this.completedQuestions.length) {
          this.clickedQuestionIndex = 0;
        }
        this.clickedQuestion =
          this.completedQuestions[this.clickedQuestionIndex];
      }
    },
    getColor(value: number) {
      if (value > 80) {
        return "green";
      } else if (value > 60) {
        return "orange";
      } else {
        return "red";
      }
    },
  },
};
</script>
