import { mount } from "@vue/test-utils";
import Users from "@/views/Users.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import { createProvider } from "@/vue-apollo";
import VueApolloPlugin from "@vue/apollo-components";
import usersQuery from "@/graphql/Users.gql";
import vuetify from "@/plugins/vuetify";
import { awaitVm } from "../TestUtils";
import { ApiUser, QuestionType } from "@/generated/types.d";
import { createPinia } from "pinia";
import { describe, expect, it, vi } from "vitest";
import { VProgressCircular } from "vuetify/components/VProgressCircular";
import { VDataTable } from "vuetify/components/VDataTable";

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
    global: {
      stubs: ["v-snackbar", "create-user-button"],
      plugins: [
        vuetify,
        VueApolloPlugin,
        createProvider({
          defaultClient: mockClient,
        }),
        createPinia(),
      ],
    },
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
    expect(
      usersPage.findComponent(VProgressCircular).vm.$props.indeterminate,
    ).toBe(true);
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
      expect(tableRows[i].text()).toContain(mockUsers[i].name);
    }
  });

  it.skip("selection works", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(usersQuery, () =>
      Promise.resolve({ data: { users: mockUsers } }),
    );
    const usersPage = await mountUsers(mockClient);
    const table = usersPage.findComponent(VDataTable);
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    expect(usersPage.vm.$data.selection).toEqual(mockUsers[0]);
    table.vm.$emit("input", [mockUsers[1]]);
    await awaitVm(usersPage);
    expect(usersPage.vm.$data.selection).toEqual(mockUsers[1]);
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
    const table = usersPage.findComponent(VDataTable);
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
    const table = usersPage.findComponent(VDataTable);
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    const deleteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DELETE");
    expect(deleteButton.length).toBe(1);
    const dialogButtonPreOpen = usersPage.findAll("[data-jest=dialogDelete]");
    expect(dialogButtonPreOpen.length).toBe(0);
    await deleteButton[0].trigger("click");
    expect(usersPage.vm.$data.deleteDialog).toBeTruthy();
    const dialogButton = usersPage
      .findAll("[data-jest=dialogDelete]")
      .filter((x) => x.text() == "DELETE");
    expect(dialogButton.length).toBe(1);
    await dialogButton[0].trigger("click");
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
      .filter((x) => x.text() == "PROMOTE")[0];
    expect(promoteButtonPreSelection.element.disabled).toBe(true);
    const table = usersPage.findComponent(VDataTable);
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[1]]);
    await awaitVm(usersPage);
    const promoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "PROMOTE")[0];
    expect(promoteButton.element.disabled).toBe(false);
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
      .filter((x) => x.text() == "DEMOTE")[0];
    expect(demoteButtonPreSelection.element.disabled).toBe(true);
    const table = usersPage.findComponent(VDataTable);
    expect(usersPage.vm.$data.selection).toBeFalsy();
    table.vm.$emit("input", [mockUsers[0]]);
    await awaitVm(usersPage);
    const demoteButton = usersPage
      .findAll("button")
      .filter((x) => x.text() == "DEMOTE")[0];
    expect(demoteButton.element.disabled).toBe(false);
    await demoteButton.trigger("click");
    await awaitVm(usersPage);
    // TODO : test behavior once implemented
  });
});
