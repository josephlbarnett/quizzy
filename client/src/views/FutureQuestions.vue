<template>
  <div class="futureQuestions">
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
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
    <ApolloQuery :query="require('../graphql/FutureQuestions.gql')">
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.futureQuestions">
          <v-card-title>
            Upcoming Questions
            <v-spacer />
            <v-dialog
              v-model="addDialog"
              @click:outside="addDialogError = false"
            >
              <template #activator="{ on }">
                <v-btn color="accent" v-on="on" @click="resetAddDialogState">
                  ADD QUESTION
                </v-btn>
              </template>
              <v-card v-if="addDialog">
                <v-card-title
                  ><span v-if="addDialogId">Edit Question</span
                  ><span v-else>Add Question</span></v-card-title
                >
                <v-card-text>
                  <v-row>
                    <v-col>
                      <date-time-picker
                        v-model="addDialogActive"
                        label="Date:"
                        :timezone="timezone"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col>
                      <date-time-picker
                        v-model="addDialogClose"
                        label="Respond By:"
                        :timezone="timezone"
                      />
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col>
                      <ApolloQuery :query="require('../graphql/Users.gql')">
                        <template #default="{ result: { data: userData } }">
                          <v-autocomplete
                            v-if="userData"
                            v-model="addDialogAuthor"
                            label="Question Author:"
                            :items="userData && userData.users"
                            item-value="id"
                            item-text="name"
                          />
                        </template>
                      </ApolloQuery>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col
                      ><v-textarea
                        v-model="addDialogBody"
                        label="Question Body:"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col
                      ><v-textarea v-model="addDialogAnswer" label="Answer:"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col
                      ><v-textarea
                        v-model="addDialogRuleReferences"
                        label="Rule References:"
                    /></v-col>
                  </v-row>
                </v-card-text>
                <v-card-actions>
                  <ApolloMutation
                    :mutation="require('../graphql/SaveQuestion.gql')"
                    :refetch-queries="() => [`FutureQuestions`]"
                    :variables="{
                      id: addDialogId,
                      activeAt: addDialogActive,
                      answer: addDialogAnswer,
                      authorId: addDialogAuthor,
                      body: addDialogBody,
                      closedAt: addDialogClose,
                      ruleReferences: addDialogRuleReferences,
                    }"
                    @done="addDialog = false"
                    @error="addDialogError = true"
                  >
                    <template #default="{ mutate, loading }">
                      <v-btn @click="addDialog = false">CANCEL</v-btn>
                      <v-btn color="accent" @click="mutate()"
                        ><span v-if="addDialogId">SAVE</span
                        ><span v-else>ADD</span></v-btn
                      >
                      <v-progress-circular
                        v-if="loading"
                        :indeterminate="true"
                      />
                      <v-snackbar v-model="addDialogError" color="error"
                        >Could not <span v-if="addDialogId">edit</span
                        ><span v-else>add</span> question, try again.
                        <template #action="{ attrs }">
                          <v-btn v-bind="attrs" @click="addDialogError = false"
                            >OK</v-btn
                          ></template
                        ></v-snackbar
                      >
                    </template>
                  </ApolloMutation>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card-title>
          <v-data-table
            :items="data.futureQuestions"
            :headers="headers"
            item-key="id"
            no-data-text="No upcoming questions found"
            @click:row="clickRow"
          >
            <template #item.closedAt="{ value }">
              {{ renderDateTime(value) }}
            </template>
            <template #item.activeAt="{ value }">
              {{ renderDate(value) }}
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";
import DateTimePicker from "@/components/DateTimePicker.vue";
import { Question } from "@/generated/types";

export default Vue.extend({
  name: "FutureQuestions",
  components: { DateTimePicker },
  data: () => ({
    addDialogError: false,
    addDialog: false,
    addDialogBody: "",
    addDialogAnswer: "",
    addDialogAuthor: "",
    addDialogActive: "",
    addDialogClose: "",
    addDialogId: null as string | null,
    addDialogRuleReferences: "",
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
    ],
    tzs: [
      {
        name: `Autodetect -- ${moment.tz.guess()} (${moment
          .tz(moment.tz.guess())
          .zoneName()})`,
        value: "Autodetect",
      },
    ].concat(
      moment.tz.names().map((name) => ({
        name: `${name} (${moment.tz(name).zoneName()})`,
        value: name,
      }))
    ),
    timezone: "Autodetect",
  }),
  methods: {
    resetAddDialogState() {
      this.addDialogId = null;
      this.addDialogBody = "";
      this.addDialogAnswer = "";
      this.addDialogAuthor = "";
      this.addDialogActive = "";
      this.addDialogClose = "";
      this.addDialogRuleReferences = "";
    },
    clickRow(item: Question) {
      this.addDialogId = item.id;
      this.addDialogBody = item.body;
      this.addDialogAnswer = item.answer;
      this.addDialogAuthor = item.authorId;
      this.addDialogActive = item.activeAt;
      this.addDialogClose = item.closedAt;
      this.addDialogRuleReferences = item.ruleReferences;
      this.addDialog = true;
    },
    setTZ(tz: string | null) {
      if (tz && this.tzs.map((x) => x.value).indexOf(tz) > -1) {
        this.timezone = tz;
      } else {
        this.timezone = "Autodetect";
      }
    },
    renderDateTime(date: string) {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, moment.ISO_8601, browserTZ);
      return `${zonedMoment.format("ddd, MMM D YYYY, h:mmA")} (${browserTZ})`;
    },
    renderDate(date: string) {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, moment.ISO_8601, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
  },
});
</script>
