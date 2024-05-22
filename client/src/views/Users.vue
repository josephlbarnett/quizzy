<template>
  <div class="users">
    <!-- fetch-policy=cache-and-network so that updated grades get picked up and bypass graphql cache -->
    <ApolloQuery
      :query="Users"
      :variables="qvars"
      fetch-policy="cache-and-network"
    >
      <template #default="{ result: { error, data }, isLoading }">
        <div v-if="isLoading">
          <v-progress-circular :indeterminate="true" />
        </div>
        <div v-else-if="error" class="bg-error">An error occurred</div>
        <v-card v-if="data && data.users">
          <v-card-title>
            Users
            <create-user-button />
            <!--v-btn
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
              <template v-slot:activator="{ props }">
                <v-btn color="error" v-bind="props" :disabled="!selection"
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
                  <v-btn color="error" @click="remove" data-jest="dialogDelete"
                    >DELETE</v-btn
                  ></v-card-actions
                >
              </v-card>
            </v-dialog-->
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
            :show-select="false"
            single-select
            no-data-text="No users found"
            :search="search"
            :sort-by="[{ key: 'score', order: 'desc' }]"
            @input="selected"
            @click:row="rowToggle"
          >
            <template #item.admin="{ item }">
              <v-checkbox v-model="item.admin" disabled></v-checkbox>
            </template>
          </v-data-table>
        </v-card>
      </template>
    </ApolloQuery>
  </div>
</template>

<script lang="ts">
import { ApiUser } from "@/generated/types";
import CreateUserButton from "@/components/CreateUserButton.vue";
import { useInstanceStore } from "@/stores/instance";
import Users from "@/graphql/Users.gql";

export default {
  name: "UsersList",
  components: { CreateUserButton },
  setup() {
    return { instanceStore: useInstanceStore() };
  },
  data: () => ({
    search: "",
    headers: [
      {
        title: "Name",
        value: "name",
        sortable: false,
      },
      {
        title: "Admin",
        value: "admin",
        sortable: false,
      },
      {
        title: "Score",
        value: "score",
        sortable: false,
      },
    ],
    selection: null as ApiUser | null,
    deleteDialog: false,
    Users,
  }),
  computed: {
    qvars() {
      const season = this.instanceStore.season;
      return {
        startTime: season?.startTime,
        endTime: season?.endTime,
      };
    },
  },
  methods: {
    selected(selection: Array<ApiUser>) {
      if (selection.length > 0) {
        this.selection = selection[0];
      } else {
        this.selection = null;
      }
    },
    rowToggle() {
      // item: ApiUser,
      // {
      //   select,
      //   isSelected,
      // }: { select: (value: boolean) => void; isSelected: boolean }
      //select(!isSelected);
    },
    promote() {
      // TODO: implement
    },
    demote() {
      // TODO: implement
    },
    remove() {
      this.deleteDialog = false;
    },
  },
};
</script>
