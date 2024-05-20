<template>
  <div class="home">
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
      :query="CurrentQuestions"
      fetch-policy="cache-and-network"
      @result="
        (result) => {
          activeQuestions = result.data && result.data.activeQuestions;
        }
      "
    >
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="bg-error">An error occurred</div>
        <v-card v-if="data && data.activeQuestions">
          <v-card-title> Current Questions</v-card-title>
          <v-data-table
            :items="data.activeQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No active questions found"
            @click:row="clickRow"
          >
            <template #item.closedAt="{ value }">
              {{ renderDateTime(value) }}
            </template>
            <template #item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
            <template #item.response.response="{ item, value }">
              {{ renderResponse(item, value) }}
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>

    <v-dialog v-model="responseDialog">
      <v-card v-if="clickedQuestion">
        <v-card-title
          >Question: {{ renderDate(clickedQuestion.activeAt) }} by
          {{ clickedQuestion.author.name }}
        </v-card-title>
        <ApolloMutation
          v-if="clickedQuestion"
          :mutation="SaveResponse"
          :refetch-queries="() => [`CurrentQuestions`]"
          :await-refetch-queries="true"
          :variables="{
            questionId: clickedQuestion.id,
            response: clickedResponse ? clickedResponse.response : '',
            ruleReferences: clickedResponse
              ? clickedResponse.ruleReferences
              : '',
            id: clickedResponse ? clickedResponse.id : null,
            userId,
          }"
          @error="saveError = true"
          @done="(result) => mutated(result)"
        >
          <template #default="{ mutate, loading }">
            <v-card-text>
              <v-row>
                <v-col v-if="clickedQuestion.imageUrl" cols="12" lg="1">
                  <v-dialog v-model="imageDialog">
                    <template #activator="{ props }">
                      <v-img
                        :src="clickedQuestion.imageUrl"
                        max-height="200px"
                        max-width="200px"
                        v-bind="props"
                      ></v-img>
                    </template>
                    <v-card @click="imageDialog = false">
                      <v-img
                        cover
                        :src="clickedQuestion.imageUrl"
                        max-height="90vh"
                        max-width="90vw"
                      ></v-img>
                    </v-card>
                  </v-dialog>
                </v-col>
                <v-col align-self="center">{{ clickedQuestion.body }}</v-col>
              </v-row>
              <v-textarea
                v-if="shortAnswer()"
                v-model="clickedResponse.response"
                label="Response"
              ></v-textarea>
              <v-textarea
                v-if="shortAnswer()"
                v-model="clickedResponse.ruleReferences"
                label="Rule Reference"
              ></v-textarea>
              <v-radio-group
                v-for="value in clickedQuestion.answerChoices"
                v-else
                :key="value.letter"
                v-model="clickedResponse.response"
              >
                <v-radio
                  :key="value.letter"
                  :label="value.letter + ': ' + value.answer"
                  :value="value.letter"
                />
              </v-radio-group>
            </v-card-text>
            <v-card-actions>
              <v-btn @click="responseDialog = false">CANCEL</v-btn>
              <v-btn color="accent" @click="saveResponse(mutate)"
                >save response
              </v-btn>
              <v-progress-circular v-if="loading" :indeterminate="true" />
            </v-card-actions>
            <v-snackbar v-model="saveError" color="error">
              Couldn't save, try again.
              <template #actions="attrs">
                <v-btn v-bind="attrs" @click="saveError = false">OK </v-btn>
              </template>
            </v-snackbar>
          </template>
        </ApolloMutation>
      </v-card>
    </v-dialog>
    <graded-question-dialog
      v-model="gradeDialog"
      :question="gradedQuestion"
      :user-t-z="userTZ"
    />
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import { ApiQuestion, ApiResponse, QuestionType } from "@/generated/types.d";
import { FetchResult } from "@apollo/client/core";
import GradedQuestionDialog from "@/components/GradedQuestionDialog.vue";
import CurrentUser from "@/graphql/CurrentUser.gql";
import CurrentQuestions from "@/graphql/CurrentQuestions.gql";
import SaveResponse from "@/graphql/SaveResponse.gql";

export default {
  name: "CurrentQuestions",
  components: { GradedQuestionDialog },
  data: () => ({
    userTZ: "Autodetect",
    activeQuestions: [],
    gradedQuestion: null as ApiQuestion | null,
    headers: [
      {
        title: "Date",
        value: "activeAt",
        sortable: false,
      },
      {
        title: "Respond By",
        value: "closedAt",
        sortable: false,
      },
      {
        title: "Question",
        value: "body",
        sortable: false,
      },
      {
        title: "Your Response",
        value: "response.response",
        sortable: false,
      },
    ],
    responseDialog: false,
    imageDialog: false,
    gradeDialog: false,
    clickedQuestion: null as ApiQuestion | null,
    clickedResponse: null as ApiResponse | null,
    userId: "",
    saveError: false,
    CurrentUser,
    CurrentQuestions,
    SaveResponse,
  }),
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
    clickRow(event: Event, { item }: { item: ApiQuestion }) {
      this.clickedQuestion = Object.assign({}, item);
      this.clickedResponse = Object.assign(
        { response: "", ruleReferences: "" },
        item.response,
      );
      this.responseDialog = true;
    },
    saveResponse(mutate: () => void) {
      mutate();
    },
    mutated(result: FetchResult<Record<string, ApiResponse>>) {
      this.responseDialog = false;
      if (result.data) {
        if (result.data["response"].grade) {
          const question = result.data["response"].question;
          if (question) {
            question.response = result.data["response"];
            this.gradedQuestion = question;
            this.gradeDialog = true;
          }
        }
      }
    },
    shortAnswer(): boolean {
      return this.clickedQuestion?.type == QuestionType.ShortAnswer;
    },
    renderResponse(item: ApiQuestion, value: string) {
      if (item.type == QuestionType.MultipleChoice) {
        const selection = item.answerChoices?.find(
          (choice) => choice.letter == value,
        );
        if (selection) {
          return selection.letter + ": " + selection.answer;
        }
      }
      return value;
    },
  },
};
</script>
