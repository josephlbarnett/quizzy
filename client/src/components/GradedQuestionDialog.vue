<template>
  <v-dialog v-model="detailDialog">
    <v-card v-if="clickedQuestion && clickedQuestion.author">
      <v-card-title
        >Review Question: {{ renderDate(clickedQuestion.activeAt) }} by
        {{ clickedQuestion.author.name }}</v-card-title
      >
      <v-card-text>
        <v-row>
          <v-col v-if="clickedQuestion.imageUrl" cols="12" lg="1">
            <v-img
              :src="clickedQuestion.imageUrl"
              max-height="200px"
              max-width="200px"
            ></v-img>
          </v-col>
          <v-col align-self="center">{{ clickedQuestion.body }}</v-col>
        </v-row>
        <v-row v-if="shortAnswer()">
          <v-col cols="6">
            <v-textarea
              v-if="clickedQuestion.response"
              v-model="clickedQuestion.response.response"
              :readonly="true"
              label="Your Response"
            />
            <v-textarea v-else :readonly="true" label="Your Response" />
          </v-col>
          <v-col cols="6">
            <v-textarea
              v-model="clickedQuestion.answer"
              :readonly="true"
              label="Answer Key"
            />
          </v-col>
        </v-row>
        <v-row v-else>
          <v-col cols="12">
            <v-radio-group
              readonly
              :value="
                clickedQuestion.response && clickedQuestion.response.response
              "
            >
              <v-row
                v-for="choice in clickedQuestion.answerChoices"
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
                    clickedQuestion.response &&
                    choice.letter == clickedQuestion.response.response &&
                    choice.letter == clickedQuestion.answer
                  "
                  color="green darken-2"
                  >mdi-check-circle</v-icon
                >
                <v-icon
                  v-if="
                    clickedQuestion.response &&
                    choice.letter == clickedQuestion.response.response &&
                    choice.letter != clickedQuestion.answer
                  "
                  color="red darken-2"
                  >mdi-close-circle</v-icon
                >
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
              v-if="clickedQuestion.response"
              v-model="clickedQuestion.response.ruleReferences"
              :readonly="true"
              label="Your Rule References"
            />
            <v-textarea v-else :readonly="true" label="Your Rule References" />
          </v-col>
          <v-col cols="6">
            <v-textarea
              v-model="clickedQuestion.ruleReferences"
              :readonly="true"
              label="Answer Key Rule References"
            />
          </v-col>
        </v-row>
        <v-row v-else>
          <v-col cols="12">
            <v-textarea
              v-model="clickedQuestion.ruleReferences"
              :readonly="true"
              label="Rule References"
            />
          </v-col>
        </v-row>
        <v-row v-if="shortAnswer()">
          <v-col>Correct?:</v-col>
          <v-col>
            <span
              v-if="
                clickedQuestion.response &&
                clickedQuestion.response.grade &&
                clickedQuestion.response.grade.correct === true
              "
            >
              <v-icon color="green darken-2">mdi-check-circle</v-icon>Yes</span
            >
            <span
              v-else-if="
                clickedQuestion.response &&
                clickedQuestion.response.grade &&
                clickedQuestion.response.grade.correct === false
              "
            >
              <v-icon color="red darken-2">mdi-close-circle</v-icon>No</span
            >
            <span v-else>
              <v-icon color="grey darken-2">mdi-help-circle</v-icon
              >Ungraded</span
            >
          </v-col>
        </v-row>
        <v-row v-if="shortAnswer()">
          <v-col>Bonus:</v-col>
          <v-col>{{ clickedQuestionBonus }}</v-col>
        </v-row>
        <v-row v-if="shortAnswer()">
          <v-col> Score: </v-col>
          <v-col>
            {{
              clickedQuestion.response &&
              clickedQuestion.response.grade &&
              clickedQuestion.response.grade.score
            }}
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <v-btn color="accent" @click="detailDialog = false">OK</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import Vue, { PropType } from "vue";
import { ApiQuestion, QuestionType } from "@/generated/types.d";
import moment from "moment-timezone";

export default Vue.extend({
  props: {
    question: { type: Object as PropType<ApiQuestion>, default: null },
    value: { type: Boolean, default: false },
    userTZ: { type: String, default: "Autodetect" },
  },
  computed: {
    detailDialog: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
    clickedQuestion() {
      return this.question;
    },
    correctAnswerChoice(): string {
      if (this.question.type == QuestionType.MultipleChoice) {
        const correctAnswer = this.question.answerChoices?.find(
          (choice) => choice.letter == this.question.answer
        );
        if (correctAnswer) {
          return correctAnswer.letter + ": " + correctAnswer.answer;
        }
      }
      return "";
    },
    clickedQuestionBonus() {
      if (this.question?.response?.grade?.correct) {
        return this.question.response.grade.bonus;
      } else if (this.question?.response?.grade?.correct === false) {
        return "0";
      }
      return "";
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
    shortAnswer(): boolean {
      return this.clickedQuestion?.type == QuestionType.ShortAnswer;
    },
  },
});
</script>
