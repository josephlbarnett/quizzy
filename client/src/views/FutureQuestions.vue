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
          this.userId =
            result && result.data && result.data.user && result.data.user.id;
        }
      "
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <ApolloQuery :query="require('../graphql/FutureQuestions.gql')">
      <template v-slot="{ result: { error, data }, isLoading }">
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
              <template v-slot:activator="{ on }">
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
                        label="Date:"
                        v-model="addDialogActive"
                        :timezone="timezone"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col>
                      <date-time-picker
                        label="Respond By:"
                        v-model="addDialogClose"
                        :timezone="timezone"
                      />
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col>
                      <ApolloQuery :query="require('../graphql/Users.gql')">
                        <template v-slot="{ result: { data } }">
                          <v-autocomplete
                            label="Question Author:"
                            v-if="data"
                            v-model="addDialogAuthor"
                            :items="data && data.users"
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
                        label="Question Body:"
                        v-model="addDialogBody"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col
                      ><v-textarea label="Answer:" v-model="addDialogAnswer"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col
                      ><v-textarea
                        label="Rule References:"
                        v-model="addDialogRuleReferences"
                    /></v-col>
                  </v-row>
                </v-card-text>
                <v-card-actions>
                  <ApolloMutation
                    :mutation="require('../graphql/SaveQuestion.gql')"
                    @done="addDialog = false"
                    @error="addDialogError = true"
                    :refetch-queries="() => [`futureQuestions`]"
                    :variables="{
                      id: addDialogId,
                      activeAt: addDialogActive,
                      answer: addDialogAnswer,
                      authorId: addDialogAuthor,
                      body: addDialogBody,
                      closedAt: addDialogClose,
                      ruleReferences: addDialogRuleReferences,
                    }"
                  >
                    <template v-slot="{ mutate, loading }">
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
                        <template v-slot:action="{ attrs }">
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
