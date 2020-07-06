import { mount } from "@vue/test-utils";
import Users from "@/views/Users.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import usersQuery from "@/graphql/Users.gql";
import vuetify from "@/plugins/vuetify";

const mockUsers = [
  {
    id: 123,
    instanceId: 456,
    instance: {
      id: 456,
      name: "Tenant",
      __typename: "Instance",
    },
    email: "joe@test.com",
    name: "joe test",
    admin: true,
    timeZoneId: "America/Los_Angeles",
    __typename: "ApiUser",
    score: 15,
  },
  {
    id: 456,
    instanceId: 456,
    instance: {
      id: 456,
      name: "Tenant",
      __typename: "Instance",
    },
    email: "jon@test.com",
    name: "jon test",
    admin: false,
    timeZoneId: "America/Los_Angeles",
    __typename: "ApiUser",
    score: 16,
  },
];

function mountUsers(mockClient: MockApolloClient) {
  return mount(Users, {
    stubs: ["v-snackbar"],
    vuetify,
    apolloProvider: new VueApollo({
      defaultClient: mockClient,
    }),
  });
}

describe("users page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      usersQuery,
      () =>
        new Promise(() => {
          // never resolve
        })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    expect(usersPage.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true
    );
  });

  it("rendered grid state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const tableRows = usersPage.findAll("tbody tr");
    expect(tableRows.length).toBe(mockUsers.length);
    for (let i = 0; i < mockUsers.length; i++) {
      expect(tableRows.at(i).text()).toContain(mockUsers[i].name);
    }
  });

  it("selection works", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await usersPage.vm.$nextTick();
    expect(usersPage.vm.$data.selection).toBe(mockUsers[0]);
    table.vm.$emit("input", [mockUsers[1]]);
    await usersPage.vm.$nextTick();
    expect(usersPage.vm.$data.selection).toBe(mockUsers[1]);
    table.vm.$emit("input", []);
    await usersPage.vm.$nextTick();
    expect(usersPage.vm.$data.selection).toBeFalsy();
  });

  it("rowToggle calls selection callback", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const table = usersPage.find(".v-data-table");
    const selectionCallback = jest.fn((selection: boolean) => {
      if (selection) {
        // do nothing
      }
    });
    table.vm.$emit("click:row", mockUsers[0], {
      select: selectionCallback,
      isSelected: false,
    });
    expect(selectionCallback.mock.calls.length).toBe(1);
    expect(selectionCallback.mock.calls[0][0]).toBe(true);
    table.vm.$emit("click:row", mockUsers[0], {
      select: selectionCallback,
      isSelected: true,
    });
    expect(selectionCallback.mock.calls.length).toBe(2);
    expect(selectionCallback.mock.calls[1][0]).toBe(false);
  });

  it("delete dialog shows up and dismisses", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await usersPage.vm.$nextTick();
    const deleteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DELETE");
    expect(deleteButton.length).toBe(1);
    const dialogButtonPreOpen = usersPage.findAll(
      ".v-dialog__container button"
    );
    expect(dialogButtonPreOpen.length).toBe(0);
    await deleteButton.at(0).trigger("click");
    expect(usersPage.vm.$data.deleteDialog).toBeTruthy();
    const dialogButton = usersPage
      .findAll(".v-dialog__container button")
      .filter((x) => x.text() == "DELETE");
    expect(dialogButton.length).toBe(1);
    await dialogButton.at(0).trigger("click");
    expect(usersPage.vm.$data.deleteDialog).toBeFalsy();
  });

  it("promote non-admin", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const promoteButtonPreSelection = usersPage
      .findAll("button")
      .filter((x) => x.text() == "PROMOTE")
      .at(0);
    expect(promoteButtonPreSelection.props().disabled).toBe(true);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[1]]);
    await usersPage.vm.$nextTick();
    const promoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "PROMOTE")
      .at(0);
    expect(promoteButton.props().disabled).toBe(false);
    await promoteButton.trigger("click");
    await usersPage.vm.$nextTick();
    // TODO : test behavior once implemented
  });

  it("demote admin", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } })
    );
    const usersPage = mountUsers(mockClient);
    await usersPage.vm.$nextTick();
    const demoteButtonPreSelection = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DEMOTE")
      .at(0);
    expect(demoteButtonPreSelection.props().disabled).toBe(true);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await usersPage.vm.$nextTick();
    const demoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DEMOTE")
      .at(0);
    expect(demoteButton.props().disabled).toBe(false);
    await demoteButton.trigger("click");
    await usersPage.vm.$nextTick();
    // TODO : test behavior once implemented
  });
});
