<template>
  <v-card>
    <v-card-title>
      Pop Quiz
      <div v-if="!summary && created && quizQuestions.length > 0">
        : Question {{ questionIndex + 1 }} of {{ quizQuestions.length }}, by
        {{ currentQuestion.author?.name }} on
        {{ renderDate(currentQuestion.activeAt) }}
      </div>
    </v-card-title>
    <div v-if="summary">
      <v-card-text>
        Quiz score: {{ score }} of {{ quizQuestions.length }}
        <graded-question-dialog
          v-model="reviewDialog"
          :question="reviewQuestion"
          :question-index="reviewIndex"
          :user-t-z="userTZ"
          @next="next"
          @prev="prev"
        />
      </v-card-text>
      <v-card-actions>
        <v-btn @click="restart">NEW POP QUIZ</v-btn>
        <v-btn color="accent" @click="reviewDialog = true">REVIEW</v-btn>
      </v-card-actions>
    </div>
    <div v-else-if="created">
      <ApolloQuery
        :query="require('../graphql/CurrentUser.gql')"
        @result="
          (result) => {
            result &&
              result.data &&
              result.data.user &&
              setTZ(result.data.user.timeZoneId);
          }
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
            createQuiz();
          }
        "
      >
        <template #default="{ result: { error }, isLoading }">
          <div v-if="isLoading">
            <v-progress-circular :indeterminate="true" />
          </div>
          <div v-else-if="error" class="error">An error occurred</div>
          <div v-else>
            <v-card-text>
              <v-row>
                <v-col v-if="currentQuestion.imageUrl" cols="12" lg="1">
                  <v-dialog v-model="imageDialog">
                    <template #activator="{ on }">
                      <v-img
                        :src="currentQuestion.imageUrl"
                        max-height="200px"
                        max-width="200px"
                        v-on="on"
                      ></v-img>
                    </template>
                    <v-card @click="imageDialog = false">
                      <v-img
                        contain
                        :src="currentQuestion.imageUrl"
                        max-height="90vh"
                        max-width="90vw"
                      ></v-img>
                    </v-card>
                  </v-dialog>
                </v-col>
                <v-col align-self="center">{{
                  currentQuestion.body
                }}</v-col> </v-row
              ><v-textarea
                v-if="shortAnswer()"
                v-model="currentResponse.response"
                label="Response"
              ></v-textarea>
              <v-textarea
                v-if="shortAnswer()"
                v-model="currentResponse.ruleReferences"
                label="Rule Reference"
              ></v-textarea>
              <v-radio-group
                v-for="value in currentQuestion.answerChoices"
                v-else
                :key="value.letter"
                v-model="currentResponse.response"
              >
                <v-radio
                  :key="value.letter"
                  :label="value.letter + ': ' + value.answer"
                  :value="value.letter"
                />
              </v-radio-group>
            </v-card-text>
            <v-card-actions>
              <v-btn @click="restart">NEW POP QUIZ</v-btn>
              <v-btn color="accent" @click="submitAnswer"
                >SUBMIT RESPONSE</v-btn
              >
            </v-card-actions>
            <graded-question-dialog
              v-model="gradeDialog"
              :question="gradedQuestion"
              :user-t-z="userTZ"
            />
          </div>
        </template>
      </ApolloQuery>
    </div>
    <div v-else>
      <v-card-text>
        <v-text-field
          v-model.number="numQuestions"
          label="How many questions?"
          type="number"
        ></v-text-field>
      </v-card-text>
      <v-card-actions>
        <v-btn color="accent" @click="create">CREATE POP QUIZ</v-btn>
      </v-card-actions>
    </div>
  </v-card>
</template>
<script lang="ts">
import Vue from "vue";
import { useInstanceStore } from "@/stores/instance";
import { ApiQuestion, ApiResponse, QuestionType } from "@/generated/types.d";
import moment from "moment-timezone";

export default Vue.extend({
  name: "PopQuiz",
  setup() {
    const instanceStore = useInstanceStore();
    return {
      instanceStore,
    };
  },
  data: () => ({
    created: false,
    userTZ: "Autodetect",
    numQuestions: 10,
    completedQuestions: [],
    responses: [] as Array<ApiResponse>,
    quizQuestions: [] as Array<ApiQuestion>,
    questionIndex: 0,
    reviewIndex: 0,
    answers: [] as Array<string>,
    imageDialog: false,
    currentResponse: { response: "", ruleReferences: "" } as ApiResponse,
    gradeDialog: false,
    reviewDialog: false,
    score: 0,
    summary: false,
  }),
  computed: {
    qvars() {
      const season = this.instanceStore.season;
      return {
        startTime: season?.startTime,
        endTime: season?.endTime,
      };
    },
    currentQuestion(): ApiQuestion {
      return this.quizQuestions[this.questionIndex];
    },
    gradedQuestion(): ApiQuestion {
      const q = Object.assign({}, this.currentQuestion);
      q.response = this.currentResponse;
      return q;
    },
    reviewQuestion(): ApiQuestion {
      const q = Object.assign({}, this.quizQuestions[this.reviewIndex]);
      q.response = this.responses[this.reviewIndex];
      return q;
    },
  },
  watch: {
    gradeDialog(newvalue: boolean, oldvalue: boolean) {
      if (oldvalue && !newvalue) {
        if (this.currentResponse.response == this.currentQuestion.answer) {
          this.score++;
        }
        this.responses.push(this.currentResponse);
        if (this.questionIndex >= this.quizQuestions.length - 1) {
          this.summary = true;
        } else {
          this.questionIndex++;
          this.currentResponse = {
            response: "",
            ruleReferences: "",
          } as ApiResponse;
        }
      }
    },
  },
  methods: {
    setTZ(tz: string | null) {
      if (tz) {
        this.userTZ = tz;
      }
    },
    create: function () {
      if (this.numQuestions != null && this.numQuestions > 0) {
        this.created = true;
      }
    },
    restart: function () {
      this.created = false;
      this.gradeDialog = false;
      this.quizQuestions = [];
      this.summary = false;
      this.responses = [];
      this.currentResponse = {
        response: "",
        ruleReferences: "",
      } as ApiResponse;
    },
    createQuiz: function () {
      this.quizQuestions = this.completedQuestions.slice(0);
      for (let i = this.quizQuestions.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [this.quizQuestions[i], this.quizQuestions[j]] = [
          this.quizQuestions[j],
          this.quizQuestions[i],
        ];
      }
      if (
        this.numQuestions != null &&
        this.numQuestions < this.quizQuestions.length
      ) {
        this.quizQuestions = this.quizQuestions.slice(0, this.numQuestions);
      }
      this.responses = [];
      this.questionIndex = 0;
      this.score = 0;
      this.gradeDialog = false;
      this.summary = false;
      this.currentResponse = {
        response: "",
        ruleReferences: "",
      } as ApiResponse;
    },
    shortAnswer(): boolean {
      return this.currentQuestion?.type == QuestionType.ShortAnswer;
    },
    submitAnswer: function () {
      this.gradeDialog = true;
    },
    renderDate(value: string) {
      const browserTZ =
        this.userTZ != null && this.userTZ != "Autodetect"
          ? this.userTZ
          : moment.tz.guess();
      const zonedMoment = moment.tz(value, moment.ISO_8601, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    next() {
      this.reviewIndex++;
      if (this.reviewIndex >= this.quizQuestions.length) {
        this.reviewIndex = 0;
      }
    },
    prev() {
      this.reviewIndex--;
      if (this.reviewIndex < 0) {
        this.reviewIndex = this.quizQuestions.length - 1;
      }
    },
  },
});
</script>
