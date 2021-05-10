<template>
  <div>
    <ApolloQuery
      :query="require('../graphql/CurrentUser.gql')"
      @result="
        (result) => {
          if (result && result.data && result.data.user) {
            this.instanceId = result.data.user.instanceId;
          }
        }
      "
    >
      <template v-slot="{}" />
    </ApolloQuery>
    <v-dialog v-model="dialog">
      <template v-slot:activator="{ on }">
        <v-btn color="accent" v-on="on" @click="resetDialog">ADD</v-btn>
      </template>
      <v-card>
        <ApolloMutation
          :mutation="getQueryDocument()"
          :variables="getQueryVariables()"
          :refetch-queries="() => [`Users`]"
          @done="added"
          @error="added"
        >
          <template v-slot="{ mutate, loading /*, error*/ }">
            <v-card-title>Add User</v-card-title>
            <v-card-text>
              <v-tabs v-model="tabs">
                <v-tab key="single">Add One</v-tab>
                <v-tab key="multiple">Add Multiple</v-tab>
                <v-tab-item key="single">
                  <v-text-field
                    @keypress.enter="submitForm(mutate)"
                    v-model="singleName"
                    label="Name"
                  ></v-text-field>
                  <v-text-field
                    @keypress.enter="submitForm(mutate)"
                    v-model="singleEmail"
                    label="Email"
                  ></v-text-field>
                </v-tab-item>
                <v-tab-item key="multiple">
                  <v-textarea
                    v-model="textarea"
                    :disabled="uploadedCsv != null"
                    label="Comma separated name/email pairs per line"
                  />
                  <v-file-input label="Or upload a csv" @change="selectFile" />
                </v-tab-item>
              </v-tabs>
              <v-btn @click="dialog = false">CANCEL</v-btn>
              <v-btn
                @click="submitForm(mutate)"
                :disabled="users.length <= 0"
                color="accent"
                >Create {{ users.length }} User<span v-if="users.length !== 1"
                  >s</span
                ></v-btn
              ><v-progress-circular :indeterminate="true" v-if="loading" />
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
      <template v-slot:action="{ attrs }">
        <v-btn v-bind="attrs" @click="snackbar = false">OK</v-btn></template
      >
    </v-snackbar>
  </div>
</template>
<script lang="ts">
import Vue from "vue";
import Papa from "papaparse";
import { MutationBaseOptions } from "apollo-client/core/watchQueryOptions";
import { User } from "@/generated/types";
import { ExecutionResult } from "graphql";
import AddUser from "@/graphql/AddUser.gql";
import combinedQuery, { CombinedQueryBuilder } from "graphql-combine-query";

type AddUserInfo = {
  name: string;
  email: string;
};

export default Vue.extend({
  name: "Login",
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
    resetDialog() {
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
            let mapped = r.data.map((rawRow) => {
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
        }))
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
});
</script>
