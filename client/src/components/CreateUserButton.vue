<template>
  <div>
    <ApolloQuery
      :query="CurrentUser"
      @result="
        (result) => {
          if (result && result.data && result.data.user) {
            instanceId = result.data.user.instanceId;
          }
        }
      "
    >
      <template #default="{}" />
    </ApolloQuery>
    <v-dialog v-model="dialog">
      <template #activator="{ props }">
        <v-btn
          color="accent"
          v-bind="props"
          @click="(e: Event) => resetDialog(e, props)"
          >ADD</v-btn
        >
      </template>
      <v-card>
        <ApolloMutation
          :mutation="getQueryDocument()"
          :variables="getQueryVariables()"
          :refetch-queries="() => [`Users`]"
          @done="added"
          @error="added"
        >
          <template #default="{ mutate, loading /*, error*/ }">
            <v-card-title>Add User</v-card-title>
            <v-card-text>
              <v-tabs v-model="tabs">
                <v-tab key="single">Add One</v-tab>
                <v-tab key="multiple">Add Multiple</v-tab>
                <v-tabs-window>
                  <v-tabs-window-item key="single">
                    <v-text-field
                      v-model="singleName"
                      label="Name"
                      @keypress.enter="submitForm(mutate)"
                    ></v-text-field>
                    <v-text-field
                      v-model="singleEmail"
                      label="Email"
                      @keypress.enter="submitForm(mutate)"
                    ></v-text-field>
                  </v-tabs-window-item>
                  <v-tabs-window-item key="multiple">
                    <v-textarea
                      v-model="textarea"
                      :disabled="uploadedCsv != null"
                      label="Comma separated name/email pairs per line"
                    />
                    <v-file-input
                      label="Or upload a csv"
                      @change="selectFile"
                    />
                  </v-tabs-window-item>
                </v-tabs-window>
              </v-tabs>
              <v-btn @click="dialog = false">CANCEL</v-btn>
              <v-btn
                :disabled="users.length <= 0"
                color="accent"
                @click="submitForm(mutate)"
                >Create {{ users.length }} User<span v-if="users.length !== 1"
                  >s</span
                ></v-btn
              ><v-progress-circular v-if="loading" :indeterminate="true" />
            </v-card-text>
          </template>
        </ApolloMutation>
      </v-card>
    </v-dialog>
    <v-snackbar
      v-model="snackbar"
      :color="addedSuccesfully > 0 || addedWithError === 0 ? 'accent' : 'error'"
    >
      <div v-if="addedSuccesfully > 0">
        Added {{ addedSuccesfully }} new user<span v-if="addedSuccesfully > 1"
          >s</span
        >.
      </div>
      <div v-if="addedWithError > 0">
        {{ addedWithError }} new user<span v-if="addedWithError > 1">s</span>
        already exist<span v-if="addedWithError === 1">s</span>.
      </div>
      <template #actions="attrs">
        <v-btn v-bind="attrs" @click="snackbar = false">OK</v-btn></template
      >
    </v-snackbar>
  </div>
</template>
<script lang="ts">
import Papa from "papaparse";
import { MutationBaseOptions } from "@apollo/client/core/watchQueryOptions";
import { User } from "@/generated/types";
import { ExecutionResult } from "graphql";
import AddUser from "@/graphql/AddUser.gql";
import combinedQuery, { CombinedQueryBuilder } from "graphql-combine-query";
import CurrentUser from "@/graphql/CurrentUser.gql";

type AddUserInfo = {
  name: string;
  email: string;
};

export default {
  name: "CreateUserButton",
  data: function () {
    return {
      singleName: "",
      singleEmail: "",
      textarea: "",
      uploadedCsv: null as File | null,
      tabs: 0,
      users: [] as AddUserInfo[],
      dialog: false,
      snackbar: false,
      addedSuccesfully: 0,
      addedWithError: 0,
      instanceId: "",
      CurrentUser,
    };
  },
  watch: {
    async tabs() {
      this.users = await this.recalculateUsers();
    },
    async singleName() {
      this.users = await this.recalculateUsers();
    },
    async singleEmail() {
      this.users = await this.recalculateUsers();
    },
    async textarea() {
      this.users = await this.recalculateUsers();
    },
    async uploadedCsv() {
      this.users = await this.recalculateUsers();
    },
  },
  methods: {
    resetDialog(event: Event, { onClick }: { onClick: (Event) => void }) {
      this.singleName = "";
      this.singleEmail = "";
      this.textarea = "";
      this.uploadedCsv = null;
      this.tabs = 0;
      this.users = [];
      this.dialog = false;
      this.snackbar = false;
      this.addedSuccesfully = 0;
      this.addedWithError = 0;
      onClick(event);
    },
    async recalculateUsers(): Promise<AddUserInfo[]> {
      return (
        this.tabs == 1
          ? await this.parsedEmails()
          : [{ name: this.singleName, email: this.singleEmail }]
      )
        .filter((item) => {
          return item.email.indexOf("@") > 0;
        })
        .map((item) => {
          if (item.name) {
            return item;
          } else {
            return { name: item.email, email: item.email };
          }
        });
    },
    async parsedEmails(): Promise<AddUserInfo[]> {
      if (this.uploadedCsv) {
        const parsedFile = await this.parseCsv(this.uploadedCsv);
        if (parsedFile.length > 0) {
          return parsedFile;
        }
      }
      return this.parseCsv(this.textarea);
    },
    async parseCsv(toParse: string | File): Promise<AddUserInfo[]> {
      return new Promise((resolve) => {
        Papa.parse<Array<string>>(toParse, {
          complete: (r) => {
            const mapped = r.data.map((rawRow) => {
              const rowObj: AddUserInfo = { name: "", email: "" };
              rawRow.forEach((item) => {
                if (item.indexOf("@") > 0) {
                  rowObj.email = item.trim();
                } else if (!rowObj.name) {
                  rowObj.name = item.trim();
                }
              });
              return rowObj;
            });
            resolve(mapped);
          },
        });
      });
    },
    selectFile(file: File) {
      this.uploadedCsv = file;
    },
    submitForm(mutate: (options: MutationBaseOptions) => void) {
      mutate({ errorPolicy: "all" });
    },
    getQuery(): CombinedQueryBuilder {
      return combinedQuery("AddUsers").addN(
        AddUser,
        this.users.map((user) => ({
          name: user.name,
          email: user.email,
          instanceId: this.instanceId,
        })),
      );
    },
    getQueryDocument() {
      return this.getQuery().document;
    },
    getQueryVariables() {
      return this.getQuery().variables;
    },
    added(result: ExecutionResult<Record<string, User>>) {
      let success = 0;
      for (const key in result.data) {
        if (result.data[key]) {
          success++;
        }
      }
      this.addedSuccesfully = success;
      this.addedWithError = result.errors?.length || 0;
      if (this.addedWithError == 0) {
        this.dialog = false;
      }
      this.snackbar = true;
    },
  },
};
</script>
