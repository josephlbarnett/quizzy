<template>
  <div class="futureQuestions">
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
            <v-dialog v-model="addDialog">
              <template v-slot:activator="{ on }">
                <v-btn color="accent" v-on="on" @click="resetAddDialogState"
                  >ADD QUESTION</v-btn
                >
              </template>
              <v-card>
                <v-card-title>Add Question</v-card-title>
                <v-card-text>
                  <v-row>
                    <v-col :cols="1">Date:</v-col>
                    <v-col :cols="4">
                      <v-row>
                        <v-menu
                          v-model="addDialogActiveDateMenu"
                          :close-on-click="true"
                          :close-on-content-click="false"
                        >
                          <template v-slot:activator="{ on }">
                            <v-text-field
                              readonly
                              :value="renderDialogDate(addDialogActiveDate)"
                              v-on="on"
                            />
                          </template>
                          <v-date-picker v-model="addDialogActiveDate" />
                        </v-menu>
                        <v-menu
                          v-model="addDialogActiveTimeMenu"
                          :close-on-click="true"
                          :close-on-content-click="false"
                        >
                          <template v-slot:activator="{ on }">
                            <v-text-field
                              readonly
                              :value="renderDialogTime(addDialogActiveTime)"
                              v-on="on"
                            />
                          </template>
                          <v-time-picker
                            v-model="addDialogActiveTime"
                            :allowed-minutes="(x) => x % 5 === 0"
                          />
                        </v-menu>
                      </v-row>
                    </v-col>
                    <v-col :cols="7" />
                  </v-row>
                  <v-row>
                    <v-col :cols="1">Respond By:</v-col>
                    <v-col :cols="4">
                      <v-row>
                        <v-menu
                          v-model="addDialogCloseDateMenu"
                          :close-on-click="true"
                          :close-on-content-click="false"
                        >
                          <template v-slot:activator="{ on }">
                            <v-text-field
                              readonly
                              :value="renderDialogDate(addDialogCloseDate)"
                              v-on="on"
                            />
                          </template>
                          <v-date-picker v-model="addDialogCloseDate" />
                        </v-menu>
                        <v-menu
                          v-model="addDialogCloseTimeMenu"
                          :close-on-click="true"
                          :close-on-content-click="false"
                        >
                          <template v-slot:activator="{ on }">
                            <v-text-field
                              readonly
                              :value="renderDialogTime(addDialogCloseTime)"
                              v-on="on"
                            />
                          </template>
                          <v-time-picker
                            v-model="addDialogCloseTime"
                            :allowed-minutes="(x) => x % 5 === 0"
                          />
                        </v-menu>
                      </v-row>
                    </v-col>
                    <v-col :cols="7" />
                  </v-row>
                  <v-row>
                    <v-col :cols="1">Question Author:</v-col>
                    <v-col :cols="11">
                      <ApolloQuery :query="require('../graphql/Users.gql')">
                        <template v-slot="{ result: { data } }">
                          <v-autocomplete
                            v-model="addDialogAuthor"
                            :items="data.users"
                            item-value="id"
                            item-text="name"
                          />
                        </template>
                      </ApolloQuery>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col :cols="1">Question Body:</v-col
                    ><v-col :cols="11"
                      ><v-textarea v-model="addDialogBody"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col :cols="1">Answer:</v-col
                    ><v-col :cols="11"
                      ><v-textarea v-model="addDialogAnswer"
                    /></v-col>
                  </v-row>
                  <v-row>
                    <v-col :cols="1">Rule References:</v-col
                    ><v-col :cols="11"
                      ><v-textarea v-model="addDialogRuleReferences"
                    /></v-col>
                  </v-row>
                </v-card-text>
                <v-card-actions>
                  <v-btn @click="addDialog = false">CANCEL</v-btn>
                  <ApolloMutation
                    :mutation="require('../graphql/SaveQuestion.gql')"
                    @done="addDialog = false"
                    @error="addDialogError = true"
                    :refetch-queries="() => [`futureQuestions`]"
                    :variables="{
                      activeAt: addDialogActive,
                      answer: addDialogAnswer,
                      authorId: addDialogAuthor,
                      body: addDialogBody,
                      closedAt: addDialogClose,
                      id: null,
                      ruleReferences: addDialogRuleReferences,
                    }"
                  >
                    <template v-slot="{ mutate, loading, error }">
                      <v-btn color="accent" @click="mutate()">ADD</v-btn>
                      <div v-if="loading">
                        <v-progress-circular :indeterminate="true" />
                      </div>
                      <v-snackbar :top="true" v-model="addDialogError"
                        >An error occurred {{ error }}</v-snackbar
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
export default Vue.extend({
  name: "FutureQuestions",
  data: () => ({
    addDialogError: false,
    addDialog: false,
    addDialogBody: "",
    addDialogAnswer: "",
    addDialogAuthor: "",
    addDialogActiveDate: "",
    addDialogActiveTime: "",
    addDialogActiveDateMenu: false,
    addDialogActiveTimeMenu: false,
    addDialogCloseDate: "",
    addDialogCloseTime: "",
    addDialogCloseDateMenu: false,
    addDialogCloseTimeMenu: false,
    addDialogRuleReferences: "",
    editDialogError: false,
    editDialog: false,
    editDialogBody: "",
    editDialogAnswer: "",
    editDialogAuthor: "",
    editDialogActiveDate: "",
    editDialogActiveTime: "",
    editDialogActiveDateMenu: false,
    editDialogActiveTimeMenu: false,
    editDialogCloseDate: "",
    editDialogCloseTime: "",
    editDialogCloseDateMenu: false,
    editDialogCloseTimeMenu: false,
    editDialogRuleReferences: "",
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
  computed: {
    addDialogActive: function () {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const parsed = moment.tz(
        `${this.addDialogActiveDate} ${this.addDialogActiveTime}`,
        "YYYY-MM-DD HH:mm",
        browserTZ
      );
      return parsed.format();
    },
    addDialogClose: function () {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const parsed = moment.tz(
        `${this.addDialogCloseDate} ${this.addDialogCloseTime}`,
        "YYYY-MM-DD HH:mm",
        browserTZ
      );
      return parsed.format();
    },
  },
  methods: {
    resetAddDialogState() {
      this.addDialogBody = "";
      this.addDialogAnswer = "";
      this.addDialogAuthor = "";
      this.addDialogActiveDate = "";
      this.addDialogActiveTime = "";
      this.addDialogActiveDateMenu = false;
      this.addDialogActiveTimeMenu = false;
      this.addDialogCloseDate = "";
      this.addDialogCloseTime = "";
      this.addDialogCloseDateMenu = false;
      this.addDialogCloseTimeMenu = false;
      this.addDialogRuleReferences = "";
    },
    setTZ(tz: string) {
      if (this.tzs.map((x) => x.value).indexOf(tz) > -1) {
        this.timezone = tz;
      } else {
        this.timezone = "Autodetect";
      }
    },
    renderDialogTime(date: string) {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const zonedMoment = moment.utc(date, "HH:mm");
      const formatted = `${zonedMoment.format("h:mm A")} (${moment
        .tz(browserTZ)
        .zoneName()})`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "--:-- --";
      } else {
        return formatted;
      }
    },
    renderDialogDate(date: string) {
      const zonedMoment = moment.utc(date, "YYYY-MM-DD");
      const formatted = `${zonedMoment.format("MM/DD/YYYY")}`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "mm/dd/yyyy";
      } else {
        return formatted;
      }
    },
    renderDateTime(date: string) {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, browserTZ);
      return `${zonedMoment.format("ddd, MMM D YYYY, h:mmA")} (${browserTZ})`;
    },
    renderDate(date: string) {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const zonedMoment = moment.tz(date, browserTZ);
      return zonedMoment.format("ddd, MMM D YYYY");
    },
  },
});
</script>
