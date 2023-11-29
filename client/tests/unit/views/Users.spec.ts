import { mount } from "@vue/test-utils";
import Users from "@/views/Users.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import usersQuery from "@/graphql/Users.gql";
import vuetify from "@/plugins/vuetify";
import { awaitVm } from "../TestUtils";
import { ApiUser, QuestionType } from "@/generated/types.d";
import { createPinia } from "pinia";
// silence a VDialog warning!?
document.body.setAttribute("data-app", "true");

const mockUsers: ApiUser[] = [
  {
    id: 123,
    instanceId: 456,
    instance: {
      id: 456,
      name: "Tenant",
      defaultQuestionType: QuestionType.ShortAnswer,
      status: "ACTIVE",
      autoGrade: false,
      __typename: "ApiInstance",
      defaultScore: 15,
      seasons: [],
      supportsGroupMe: true,
    },
    email: "joe@test.com",
    name: "joe test",
    admin: true,
    timeZoneId: "America/Los_Angeles",
    notifyViaEmail: false,
    __typename: "ApiUser",
    score: 15,
  },
  {
    id: 456,
    instanceId: 456,
    instance: {
      id: 456,
      name: "Tenant",
      defaultQuestionType: QuestionType.ShortAnswer,
      status: "ACTIVE",
      autoGrade: false,
      __typename: "ApiInstance",
      defaultScore: 15,
      seasons: [],
      supportsGroupMe: true,
    },
    email: "jon@test.com",
    name: "jon test",
    admin: false,
    timeZoneId: "America/Los_Angeles",
    notifyViaEmail: false,
    __typename: "ApiUser",
    score: 14,
  },
];

async function mountUsers(mockClient: MockApolloClient) {
  const page = mount(Users, {
    stubs: ["v-snackbar", "create-user-button"],
    vuetify,
    apolloProvider: new VueApollo({
      defaultClient: mockClient,
    }),
    pinia: createPinia(),
  });
  await awaitVm(page);
  return page;
}

describe("users page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      usersQuery,
      () =>
        new Promise(() => {
          // never resolve
        }),
    );
    const usersPage = await mountUsers(mockClient);
    expect(usersPage.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true,
    );
  });

  it("rendered grid state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const tableRows = usersPage.findAll("tbody tr");
    expect(tableRows.length).toBe(mockUsers.length);
    for (let i = 0; i < mockUsers.length; i++) {
      expect(tableRows.at(i).text()).toContain(mockUsers[i].name);
    }
  });

  it("selection works", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    expect(usersPage.vm.$data.selection).toBe(mockUsers[0]);
    table.vm.$emit("input", [mockUsers[1]]);
    await awaitVm(usersPage);
    expect(usersPage.vm.$data.selection).toBe(mockUsers[1]);
    table.vm.$emit("input", []);
    await awaitVm(usersPage);
    expect(usersPage.vm.$data.selection).toBeFalsy();
  });

  it.skip("rowToggle calls selection callback", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const table = usersPage.find(".v-data-table");
    const selectionCallback = vi.fn((selection: boolean) => {
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

  it.skip("delete dialog shows up and dismisses", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    const deleteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DELETE");
    expect(deleteButton.length).toBe(1);
    const dialogButtonPreOpen = usersPage.findAll("[data-jest=dialogDelete]");
    expect(dialogButtonPreOpen.length).toBe(0);
    await deleteButton.at(0).trigger("click");
    expect(usersPage.vm.$data.deleteDialog).toBeTruthy();
    const dialogButton = usersPage
      .findAll("[data-jest=dialogDelete]")
      .filter((x) => x.text() == "DELETE");
    expect(dialogButton.length).toBe(1);
    await dialogButton.at(0).trigger("click");
    expect(usersPage.vm.$data.deleteDialog).toBeFalsy();
  });

  it.skip("promote non-admin", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const promoteButtonPreSelection = usersPage
      .findAll("button")
      .filter((x) => x.text() == "PROMOTE")
      .at(0);
    expect(promoteButtonPreSelection.props().disabled).toBe(true);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[1]]);
    await awaitVm(usersPage);
    const promoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "PROMOTE")
      .at(0);
    expect(promoteButton.props().disabled).toBe(false);
    await promoteButton.trigger("click");
    await awaitVm(usersPage);
    // TODO : test behavior once implemented
  });

  it.skip("demote admin", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const demoteButtonPreSelection = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DEMOTE")
      .at(0);
    expect(demoteButtonPreSelection.props().disabled).toBe(true);
    const table = usersPage.find(".v-data-table");
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    const demoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DEMOTE")
      .at(0);
    expect(demoteButton.props().disabled).toBe(false);
    await demoteButton.trigger("click");
    await awaitVm(usersPage);
    // TODO : test behavior once implemented
  });
});
