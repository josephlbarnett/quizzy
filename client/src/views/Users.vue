<template>
  <div class="users">
    <ApolloQuery
      :query="require('../graphql/Users.gql')"
      @result="
        (result) => {
          this.users = result.users;
        }
      "
    >
      <template v-slot="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="error">An error occurred</div>
        <v-card v-if="data && data.users">
          <v-card-title>
            Users
            <v-btn
              color="accent"
              outlined
              @click="promote"
              :disabled="!selection || selection.admin"
              >PROMOTE</v-btn
            >
            <v-btn
              color="error"
              outlined
              @click="demote"
              :disabled="!selection || !selection.admin"
              >DEMOTE</v-btn
            >
            <v-dialog v-model="deleteDialog">
              <template v-slot:activator="{ on }">
                <v-btn color="error" v-on="on" :disabled="!selection"
                  >DELETE</v-btn
                >
              </template>
              <v-card>
                <v-card-title v-if="selection"
                  >Delete {{ selection.name }}</v-card-title
                >
                <v-card-text
                  >Are you sure? This action cannot be undone.</v-card-text
                >
                <v-card-actions>
                  <v-btn @click="deleteDialog = false">CANCEL</v-btn>
                  <v-btn color="error" @click="remove"
                    >DELETE</v-btn
                  ></v-card-actions
                >
              </v-card>
            </v-dialog>
            <v-spacer />
            <v-text-field
              v-model="search"
              hide-details
              append-icon="mdi-magnify"
              label="Search"
            />
          </v-card-title>
          <v-data-table
            :items="data.users"
            :headers="headers"
            item-key="id"
            show-select
            single-select
            no-data-text="No users found"
            :search="search"
            @input="selected"
            @click:row="rowToggle"
          >
            <template v-slot:item.admin="{ item }">
              <v-simple-checkbox
                v-model="item.admin"
                disabled
              ></v-simple-checkbox>
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
  </div>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({
  name: "Users",
  data: () => ({
    users: [],
    search: "",
    headers: [
      {
        text: "Name",
        value: "name",
        sortable: false,
      },
      {
        text: "Admin",
        value: "admin",
        sortable: false,
      },
    ],
    selection: null as object | null,
    deleteDialog: false,
  }),
  methods: {
    selected(selection: Array<object>) {
      if (selection.length > 0) {
        this.selection = selection[0];
      } else {
        this.selection = null;
      }
    },
    rowToggle(
      item: object,
      {
        select,
        isSelected,
      }: { select: (value: boolean) => void; isSelected: boolean }
    ) {
      select(!isSelected);
    },
    promote() {
      alert(JSON.stringify(this.selection));
    },
    demote() {
      alert(JSON.stringify(this.selection));
    },
    remove() {
      console.log(JSON.stringify(this.selection));
      this.deleteDialog = false;
    },
  },
});
</script>
