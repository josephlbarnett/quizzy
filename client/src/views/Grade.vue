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
            <template v-slot:item.grade.correct="{ value }">
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
    <v-dialog v-model="gradeDialog" v-if="clickedResponse">
      <v-card>
        <ApolloMutation
          :refetch-queries="() => [`Grader`, `CompletedQuestions`, `Users`]"
          :mutation="require('../graphql/Grade.gql')"
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
          <template v-slot="{ mutate, loading }">
            <v-card-title
              >Grade Response:
              {{ renderDate(clickedResponse.question.activeAt) }}</v-card-title
            >
            <v-card-text>
              <div>{{ clickedResponse.question.body }}</div>
              <v-row>
                <v-col cols="6">
                  <v-textarea
                    :readonly="true"
                    v-model="clickedResponse.response"
                    :label="clickedResponse.user.name + '\'s Response'"
                  />
                </v-col>
                <v-col cols="6">
                  <v-textarea
                    :disabled="true"
                    v-model="clickedResponse.question.answer"
                    label="Answer Key"
                  />
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="6">
                  <v-textarea
                    :readonly="true"
                    v-model="clickedResponse.ruleReferences"
                    :label="clickedResponse.user.name + '\'s Rule References'"
                  />
                </v-col>
                <v-col cols="6">
                  <v-textarea
                    :disabled="true"
                    v-model="clickedResponse.question.ruleReferences"
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
                type="number"
                v-model.number="clickedResponse.bonus"
                label="Bonus points"
              />
            </v-card-text>
            <v-card-actions>
              <v-btn @click="gradeDialog = false">CANCEL</v-btn>
              <v-btn color="accent" @click="mutate()">GRADE</v-btn>
              <v-progress-circular :indeterminate="true" v-if="loading" />
            </v-card-actions>
            <v-snackbar :top="true" v-model="saveError">
              Couldn't save, try again.
              <v-btn @click="saveError = false">OK</v-btn>
            </v-snackbar>
          </template>
        </ApolloMutation>
      </v-card>
    </v-dialog>
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
        value: "grade.correct",
        sortable: false,
      },
      {
        text: "Score",
        value: "grade.score",
        sortable: false,
      },
    ],
    userId: "",
    saveError: false,
    hideGraded: true,
    gradeDialog: false,
    clickedResponse: null as {} | null,
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
    clickRow(item: {
      grade: {
        id: string | null;
        correct: boolean | null;
        bonus: number | null;
      } | null;
      question: {
        body: string;
        activeAt: string;
        answer: string;
        ruleReferences: string;
      };
      user: { name: string; email: string };
      response: string;
      ruleReferences: string;
      id: string;
    }) {
      const clickedResponse = {
        bonus: item.grade && item.grade.bonus,
        correct:
          item.grade === null || item.grade.correct === null
            ? "ungraded"
            : item.grade.correct
            ? "correct"
            : "incorrect",
        question: Object.assign({}, item.question),
        user: Object.assign({}, item.user),
        id: item.id,
        response: item.response,
        ruleReferences: item.ruleReferences,
        gradeId: item.grade && item.grade.id,
      };
      this.clickedResponse = clickedResponse;
      this.gradeDialog = true;
    },
    saveResponse(mutate: Function) {
      mutate();
      this.gradeDialog = false;
    },
    color(value: boolean) {
      alert(value);
      return value ? "green" : "red";
    },
    mailToLink(clickedResponse: {
      user: { name: string; email: string };
      question: {
        body: string;
        activeAt: string;
        answer: string;
        ruleReferences: string;
      };
      response: string;
      ruleReferences: string;
    }) {
      const subject = encodeURIComponent(
        `Re: ${this.renderDate(clickedResponse.question.activeAt)} Question`
      );
      const quotedResponse =
        "> " + clickedResponse.response.replace(/(?:\r\n|\r|\n)/g, "\n> ");
      const quotedRuleRefs =
        "> " +
        clickedResponse.ruleReferences.replace(/(?:\r\n|\r|\n)/g, "\n> ");
      const body = encodeURIComponent(`${quotedResponse}\n` + quotedRuleRefs);
      return `${clickedResponse.user.email}?subject=${subject}&body=${body}`;
    },
  },
});
</script>
