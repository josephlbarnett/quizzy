<template>
  <div class="home">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) => {
          result &&
            result.data &&
            result.data.user &&
            setTZ(result.data.user.timeZoneId);
          this.userId =
            result && result.data && result.data.user && result.data.user.id;
        }
      "
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery
      :query="require('../graphql/CurrentQuestions.gql')"
      @result="
        (result) => {
          this.activeQuestions = result.data.activeQuestions;
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.activeQuestions">
          <v-card-title>
            Current Questions
          </v-card-title>
          <v-data-table
            :items="data.activeQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No active questions found"
            @click:row="clickRow"
          >
            <template v-slot:item.closedAt="{ value }">
              {{ renderDateTime(value) }}
            </template>
            <template v-slot:item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>

    <v-dialog v-model="responseDialog">
      <v-card v-if="clickedQuestion">
        <v-card-title
          >Question: {{ renderDate(clickedQuestion.activeAt) }} by
          {{ clickedQuestion.author.name }}</v-card-title
        >
        <ApolloMutation
          v-if="clickedQuestion"
          :mutation="require('../graphql/SaveResponse.gql')"
          :refetch-queries="() => [`currentQuestions`]"
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
          @done="responseDialog = false"
        >
          <template v-slot="{ mutate, loading }">
            <v-card-text>
              {{ clickedQuestion.body }}
              <v-textarea
                label="Response"
                v-model="clickedResponse.response"
              ></v-textarea>
              <v-textarea
                label="Rule Reference"
                v-model="clickedResponse.ruleReferences"
              ></v-textarea>
            </v-card-text>
            <v-card-actions>
              <v-btn @click="responseDialog = false">CANCEL</v-btn>
              <v-btn @click="saveResponse(mutate)" color="accent"
                >save response</v-btn
              >
              <v-progress-circular :indeterminate="true" v-if="loading"
            /></v-card-actions>
            <v-snackbar v-model="saveError" color="error">
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
import { ApiQuestion, ApiResponse } from "@/generated/types";

export default Vue.extend({
  name: "CurrentQuestions",
  data: () => ({
    userTZ: "Autodetect",
    activeQuestions: [],
    headers: [
      {
        text: "Date",
        value: "activeAt",
        sortable: false,
      },
      {
        text: "Respond By",
        value: "closedAt",
        sortable: false,
      },
      {
        text: "Question",
        value: "body",
        sortable: false,
      },
      {
        text: "Your Response",
        value: "response.response",
        sortable: false,
      },
    ],
    responseDialog: false,
    clickedQuestion: null as object | null,
    clickedResponse: null as ApiResponse | null,
    userId: "",
    saveError: false,
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
    setTZ(tz: string | null) {
      if (tz) {
        this.userTZ = tz;
      }
    },
    clickRow(item: ApiQuestion) {
      this.clickedQuestion = Object.assign({}, item);
      this.clickedResponse = Object.assign({}, item.response);
      this.responseDialog = true;
    },
    saveResponse(mutate: Function) {
      mutate();
    },
  },
});
</script>
