<template>
  <div class="futureQuestions">
    <ApolloQuery
      :query="CurrentUser"
      @result="
        (result: ApolloQueryResult<Query>) => {
          setUser(result);
        }
      "
    >
      <template #default="{}" />
    </ApolloQuery>
    <ApolloQuery :query="FutureQuestions">
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="bg-error">An error occurred</div>
        <v-card v-if="data && data.futureQuestions">
          <v-card-title>
            Upcoming Questions
            <v-spacer />
            <v-dialog
              v-model="addDialog"
              @click:outside="addDialogError = false"
            >
              <template #activator="{ props }">
                <v-btn
                  color="accent"
                  v-bind="props"
                  @click="resetAddDialogState"
                >
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
                      />
                    </v-col>
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
                      <ApolloQuery :query="Users">
                        <template #default="{ result: { data: userData } }">
                          <v-autocomplete
                            v-if="userData"
                            v-model="addDialogAuthor"
                            label="Question Author:"
                            :items="userData && userData.users"
                            item-value="id"
                            item-title="name"
                          />
                        </template>
                      </ApolloQuery>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col v-if="supportsImage" cols="12">
                      <v-file-input
                        v-model="addDialogImage"
                        placeholder="Image"
                      />
                      <v-icon v-if="clickedImage" @click="clickedImage = null"
                        >mdi-close
                      </v-icon>
                      <v-dialog v-model="imageDialog">
                        <template #activator="{ props }">
                          <v-img
                            v-if="imageUrl"
                            :src="imageUrl"
                            max-height="200px"
                            max-width="200px"
                            v-bind="props"
                          ></v-img>
                        </template>
                        <v-card @click="imageDialog = false">
                          <v-img
                            v-if="imageUrl"
                            cover
                            :src="imageUrl"
                            max-height="90vh"
                            max-width="90vw"
                          ></v-img>
                        </v-card>
                      </v-dialog>
                    </v-col>
                    <v-col>
                      <v-textarea
                        v-model="addDialogBody"
                        label="Question Body:"
                      />
                    </v-col>
                  </v-row>
                  <v-row v-if="shortAnswer()">
                    <v-col>
                      <v-textarea v-model="addDialogAnswer" label="Answer:" />
                    </v-col>
                  </v-row>
                  <v-row v-else>
                    <v-col>
                      Answer Choices:
                      <v-radio-group v-model="addDialogAnswer">
                        <v-row
                          v-for="(choice, index) in addDialogAnswerChoices"
                          :key="index"
                        >
                          <v-radio :value="getLetterIndex(index)" />
                          <v-text-field
                            v-model="addDialogAnswerChoices[index]"
                            :label="'Answer ' + getLetterIndex(index)"
                          />
                          <v-icon
                            v-if="addDialogAnswerChoices.length > 1"
                            @click="addDialogAnswerChoices.splice(index, 1)"
                            >mdi-close
                          </v-icon>
                        </v-row>
                        <v-row>
                          <v-icon @click="addDialogAnswerChoices.push('')"
                            >mdi-plus
                          </v-icon>
                        </v-row>
                      </v-radio-group>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col>
                      <v-textarea
                        v-model="addDialogRuleReferences"
                        label="Rule References:"
                      />
                    </v-col>
                  </v-row>
                </v-card-text>
                <v-card-actions>
                  <ApolloMutation
                    :mutation="SaveQuestion"
                    :refetch-queries="
                      () => [`FutureQuestions`, `CurrentQuestions`]
                    "
                    @done="addDialog = false"
                    @error="addDialogError = true"
                  >
                    <template #default="{ mutate, loading }">
                      <v-btn @click="addDialog = false">CANCEL</v-btn>
                      <v-btn color="accent" @click="submitForm(mutate)"
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
                        <template #actions="attrs">
                          <v-btn v-bind="attrs" @click="addDialogError = false"
                            >OK
                          </v-btn>
                        </template>
                      </v-snackbar>
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
import moment from "moment-timezone";
import DateTimePicker from "@/components/DateTimePicker.vue";
import {
  AnswerChoice,
  Query,
  Question,
  QuestionType,
} from "@/generated/types.d";
import { ApolloQueryResult, FetchResult } from "@apollo/client/core";
import CurrentUser from "@/graphql/CurrentUser.gql";
import FutureQuestions from "@/graphql/FutureQuestions.gql";
import Users from "@/graphql/Users.gql";
import SaveQuestion from "@/graphql/SaveQuestion.gql";

export default {
  name: "FutureQuestions",
  components: { DateTimePicker },
  data: () => ({
    addDialogError: false,
    addDialog: false,
    addDialogBody: "",
    addDialogAnswer: "",
    addDialogAnswerChoices: ["", "", "", ""],
    addDialogAuthor: "",
    addDialogActive: "",
    addDialogClose: "",
    addDialogId: null as string | null,
    addDialogRuleReferences: "",
    addDialogImage: null as File | null,
    clickedImage: null as string | null,
    imageDialog: false,
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
      })),
    ),
    timezone: "Autodetect",
    questionType: QuestionType.ShortAnswer,
    supportsImage: false,
    CurrentUser,
    FutureQuestions,
    Users,
    SaveQuestion,
  }),
  computed: {
    imageUrl() {
      if (!this.addDialogImage) {
        return this.clickedImage;
      }
      return URL.createObjectURL(this.addDialogImage);
    },
  },
  methods: {
    resetAddDialogState() {
      this.addDialogId = null;
      this.addDialogBody = "";
      this.addDialogAnswer = "";
      this.addDialogAuthor = "";
      this.addDialogActive = "";
      this.addDialogClose = "";
      this.addDialogRuleReferences = "";
      this.addDialogAnswerChoices = ["", "", "", ""];
      this.addDialogImage = null;
      this.clickedImage = null;
    },
    clickRow(event: Event, { item }: { item: Question }) {
      this.addDialogId = item.id;
      this.addDialogBody = item.body;
      this.addDialogAnswer = item.answer;
      this.addDialogAuthor = item.authorId;
      this.addDialogActive = item.activeAt;
      this.addDialogClose = item.closedAt;
      this.addDialogRuleReferences = item.ruleReferences;
      this.addDialogAnswerChoices = item.answerChoices?.map(
        (choice) => choice.answer,
      ) || ["", "", "", ""];
      this.clickedImage = item.imageUrl || null;
      this.addDialogImage = null;
      this.addDialog = true;
    },
    setUser(result: ApolloQueryResult<Query>) {
      if (result && result.data && result.data.user) {
        const user = result.data.user;
        const tz = user.timeZoneId;
        if (tz && this.tzs.map((x) => x.value).indexOf(tz) > -1) {
          this.timezone = tz;
        } else {
          this.timezone = "Autodetect";
        }
        if (user.instance) {
          this.supportsImage = user.instance.supportsGroupMe;
          if (user.instance.defaultQuestionType) {
            this.questionType = user.instance.defaultQuestionType;
          }
        }
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
    shortAnswer(): boolean {
      return this.questionType == QuestionType.ShortAnswer;
    },
    getLetterIndex(index: number): string {
      return String.fromCharCode("A".charCodeAt(0) + index);
    },
    mutationAnswerChoices(): AnswerChoice[] {
      return this.addDialogAnswerChoices.map((value, index) => {
        return {
          letter: this.getLetterIndex(index),
          answer: value,
        };
      });
    },
    async submitForm(mutate: (options: object) => Promise<FetchResult>) {
      let imageUrl = this.clickedImage;
      if (this.addDialogImage && this.imageUrl) {
        // try to convert unsupported image type to a jpeg before uploading
        let convertedImage: Blob = this.addDialogImage;
        const image = new Image();
        image.src = this.imageUrl;
        const canvas = document.createElement("canvas");
        canvas.height = image.height;
        canvas.width = image.width;
        const context = canvas.getContext("2d");
        if (
          context &&
          !["image/jpeg", "image/gif", "image/png"].includes(
            this.addDialogImage?.type || "",
          )
        ) {
          context.drawImage(image, 0, 0);
          const convertPromise = new Promise<Blob>((resolve, reject) => {
            canvas.toBlob((b) => {
              if (b) {
                resolve(b);
              } else {
                reject();
              }
            }, "image/jpeg");
          });
          convertedImage = await convertPromise.catch();
        }
        const response = await fetch(
          `${import.meta.env.VITE_REST_HTTP}/image/upload`,
          {
            method: "POST",
            credentials: "include",
            body: convertedImage,
          },
        );
        if (response.ok) {
          imageUrl = await response.text();
        }
      }
      const options = {
        id: this.addDialogId,
        activeAt: this.addDialogActive,
        answer: this.addDialogAnswer,
        authorId: this.addDialogAuthor,
        body: this.addDialogBody,
        closedAt: this.addDialogClose,
        ruleReferences: this.addDialogRuleReferences,
        type: this.questionType,
        answerChoices: this.mutationAnswerChoices(),
        imageUrl: imageUrl,
      };
      await mutate({ variables: options });
    },
  },
};
</script>
