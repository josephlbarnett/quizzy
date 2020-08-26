<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) =>
          result &&
          result.data &&
          result.data.user &&
          setTZ(result.data.user.timeZoneId)
      "
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
          <v-card-title> Completed Questions </v-card-title>
          <v-data-table
            :items="data.closedQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No completed questions found"
            @click:row="clickRow"
          >
            <template v-slot:item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template v-slot:item.response.grade.correct="{ value }">
              <v-icon v-if="value === true" color="green darken-2"
                >mdi-check-circle</v-icon
              >
              <v-icon v-else-if="value === false" color="red darken-2"
                >mdi-close-circle</v-icon
              >
              <v-icon v-else color="grey darken-2">mdi-help-circle</v-icon>
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
    <v-dialog v-model="detailDialog">
      <v-card v-if="clickedQuestion && clickedQuestion.author">
        <v-card-title
          >Review Question: {{ renderDate(clickedQuestion.activeAt) }} by
          {{ clickedQuestion.author.name }}</v-card-title
        >
        <v-card-text>
          <div>{{ clickedQuestion.body }}</div>
          <v-row>
            <v-col cols="6">
              <v-textarea
                :readonly="true"
                v-model="clickedQuestion.response.response"
                v-if="clickedQuestion.response"
                label="Your Response"
              />
              <v-textarea v-else :readonly="true" label="Your Response" />
            </v-col>
            <v-col cols="6">
              <v-textarea
                :readonly="true"
                v-model="clickedQuestion.answer"
                label="Answer Key"
              />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-textarea
                :readonly="true"
                v-model="clickedQuestion.response.ruleReferences"
                v-if="clickedQuestion.response"
                label="Your Rule References"
              />
              <v-textarea
                v-else
                :readonly="true"
                label="Your Rule References"
              />
            </v-col>
            <v-col cols="6">
              <v-textarea
                :readonly="true"
                v-model="clickedQuestion.ruleReferences"
                label="Answer Key Rule References"
              />
            </v-col>
          </v-row>
          <v-row>
            Correct?:
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
          </v-row>
          <v-row> Bonus: {{ clickedQuestionBonus }} </v-row>
          <v-row>
            Score:
            {{
              clickedQuestion.response &&
              clickedQuestion.response.grade &&
              clickedQuestion.response.grade.score
            }}
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn @click="detailDialog = false" color="accent">OK</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";
import { ApiQuestion } from "@/generated/types";

export default Vue.extend({
  name: "CompletedQuestions",
  data: () => ({
    userTZ: "Autodetect",
    detailDialog: false,
    clickedQuestion: null as ApiQuestion | null,
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
    clickedQuestionBonus() {
      if (this.clickedQuestion?.response?.grade?.correct) {
        return this.clickedQuestion.response.grade.bonus;
      } else if (this.clickedQuestion?.response?.grade?.correct === false) {
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
      const zonedMoment = moment.tz(value, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
    setTZ(tz: string | null) {
      if (tz) {
        this.userTZ = tz;
      }
    },
    clickRow(item: ApiQuestion) {
      this.clickedQuestion = item;
      this.detailDialog = true;
    },
  },
});
</script>
