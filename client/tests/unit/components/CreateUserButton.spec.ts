import { mount } from "@vue/test-utils";
import CreateUserButton from "@/components/CreateUserButton.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import UsersQuery from "@/graphql/Users.gql";
import { awaitVm } from "../TestUtils";
import { ApiUser, QuestionType } from "@/generated/types.d";
import { describe, expect, it } from "vitest";
import Vuetify from "@/plugins/vuetify";
import VueApolloPlugin from "@vue/apollo-components";
import { createProvider } from "@/vue-apollo";

// silence a VDialog warning!?
//document.body.setAttribute("data-app", "true");

const mockUser: ApiUser = {
  id: 123,
  instanceId: 456,
  instance: {
    id: 789,
    name: "Tenant",
    defaultQuestionType: QuestionType.ShortAnswer,
    autoGrade: false,
    status: "ACTIVE",
    __typename: "ApiInstance",
    defaultScore: 15,
    seasons: [],
    supportsGroupMe: true,
  },
  email: "joe@test.com",
  name: "joe test",
  admin: false,
  timeZoneId: "America/Los_Angeles",
  notifyViaEmail: false,
  score: 15,
  __typename: "ApiUser",
};

async function mountComponent(mockClient: MockApolloClient) {
  const component = mount(CreateUserButton, {
    propsData: {
      inTest: true,
    },
    global: {
      stubs: [],
      plugins: [
        VueApolloPlugin,
        Vuetify,
        createProvider({
          defaultClient: mockClient,
        }),
      ],
    },
  });
  // watch the "Users" query to prevent warning on refetchQueries setting
  mockClient.setRequestHandler(UsersQuery, () =>
    Promise.resolve({ data: { users: [] } }),
  );
  await component.vm.$apollo.watchQuery({ query: UsersQuery });
  return component;
}

describe("CreateUserButton tests", () => {
  it("renders on single add tab", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    const prom = component.find("button").trigger("click");
    await prom;
    expect(component.findAll(".v-text-field").length).toBe(2);
    expect(
      component.findAll(".v-text-field .v-field-label--floating")[0].text(),
    ).toBe("Name");
    expect(
      component.findAll(".v-text-field .v-field-label--floating")[1].text(),
    ).toBe("Email");
    expect(component.find(".v-tab--selected").text()).toBe("Add One");
  });

  it("renders multiple add tab", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    expect(component.find(".v-tab--selected").text()).toBe("Add Multiple");
    expect(component.findAll(".v-textarea").length).toBe(1);
    expect(component.findAll(".v-file-input").length).toBe(1);
  });

  it("single add tab entry", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component
      .findAll(".v-text-field")[0]
      .find("input")
      .setValue("A name");
    expect(
      component
        .findAll("button")
        .filter((b) => b.text().indexOf("Create") == 0)[0]
        .text(),
    ).toContain("Create 0");
    expect(
      (
        component
          .findAll("button")
          .filter((b) => b.text().indexOf("Create") == 0)[0]
          .element as HTMLButtonElement
      ).disabled,
    ).toBeTruthy();
    await component
      .findAll(".v-text-field")[1]
      .find("input")
      .setValue("a@b.com");
    await awaitVm(component);
    expect(
      component
        .findAll("button")
        .filter((b) => b.text().indexOf("Create") == 0)[0]
        .text(),
    ).toContain("Create 1");
    expect(
      (
        component
          .findAll("button")
          .filter((b) => b.text().indexOf("Create") == 0)[0]
          .element as HTMLButtonElement
      ).disabled,
    ).toBeFalsy();
    expect(component.vm.$data.singleName).toBe("A name");
    expect(component.vm.$data.singleEmail).toBe("a@b.com");
    expect(component.vm.$data.users).toEqual([
      { name: "A name", email: "a@b.com" },
    ]);
  });

  it("multi add tab textarea entry", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    await component
      .find(".v-textarea")
      .find("textarea")
      .setValue("name1,a@b.com\nname2,c@d.com\ne@f.com\ng");
    await awaitVm(component);
    expect(component.vm.$data.textarea).toBe(
      "name1,a@b.com\nname2,c@d.com\ne@f.com\ng",
    );
    expect(component.vm.$data.users).toEqual([
      { name: "name1", email: "a@b.com" },
      { name: "name2", email: "c@d.com" },
      { name: "e@f.com", email: "e@f.com" },
    ]);
  });

  it("multi add tab fileinput entry", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    const input = component.find(".v-file-input").find("input");
    Object.defineProperty(input.element, "files", {
      get: () => [
        new File(["name1,a@b.com\nname2,c@d.com\ne@f.com\ng"], "filename.txt", {
          type: "text/plain",
        }),
      ],
    });
    await input.trigger("change");
    await awaitVm(component);
    await awaitVm(component);
    expect(component.vm.$data.uploadedCsv).toBeTruthy();
    expect(component.vm.$data.users).toEqual([
      { name: "name1", email: "a@b.com" },
      { name: "name2", email: "c@d.com" },
      { name: "e@f.com", email: "e@f.com" },
    ]);
  });

  it("submit form successfully", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    await component
      .find(".v-textarea")
      .find("textarea")
      .setValue("name1,a@b.com\nname2,c@d.com\ne@f.com\ng");
    await awaitVm(component);
    mockClient.setRequestHandler(component.vm.getQueryDocument(), () =>
      Promise.resolve({
        data: { user_0: mockUser, user_1: mockUser, user_2: mockUser },
      }),
    );
    await component
      .findAll("button")
      .filter((b) => b.text().indexOf("Create") == 0)[0]
      .trigger("click");
    await awaitVm(component);
    expect(component.vm.$data.addedSuccesfully).toBe(3);
    expect(component.find(".v-snackbar").text()).toContain(
      "Added 3 new users.",
    );
    expect(component.vm.$data.addedWithError).toBe(0);
    expect(component.vm.$data.dialog).toBeFalsy();
  });

  it("submit form partial success", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    await component
      .find(".v-textarea")
      .find("textarea")
      .setValue("name1,a@b.com\nname2,c@d.com\ne@f.com\ng");
    await awaitVm(component);
    mockClient.setRequestHandler(component.vm.getQueryDocument(), () =>
      Promise.resolve({
        data: { user_0: mockUser, user_1: null, user_2: mockUser },
        errors: [{ message: "e" }],
      }),
    );
    await component
      .findAll("button")
      .filter((b) => b.text().indexOf("Create") == 0)[0]
      .trigger("click");
    await awaitVm(component);
    expect(component.vm.$data.addedSuccesfully).toBe(2);
    expect(component.find(".v-snackbar").text()).toContain(
      "Added 2 new users.",
    );
    expect(component.vm.$data.addedWithError).toBe(1);
    expect(component.find(".v-snackbar").text()).toMatch(
      /1 new user\s+already exists./,
    );
    expect(component.vm.$data.dialog).toBeTruthy();
  });

  it("submit form all error", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const component = await mountComponent(mockClient);
    await component.find("button").trigger("click");
    await component.findAll(".v-tab")[1].trigger("click");
    await awaitVm(component);
    await component
      .find(".v-textarea")
      .find("textarea")
      .setValue("name1,a@b.com\nname2,c@d.com\ne@f.com\ng");
    await awaitVm(component);
    mockClient.setRequestHandler(component.vm.getQueryDocument(), () =>
      Promise.resolve({
        data: { user_0: null, user_1: null, user_2: null },
        errors: [{ message: "e" }, { message: "e" }, { message: "e" }],
      }),
    );
    await component
      .findAll("button")
      .filter((b) => b.text().indexOf("Create") == 0)[0]
      .trigger("click");
    await awaitVm(component);
    expect(component.vm.$data.addedSuccesfully).toBe(0);
    expect(component.find(".v-snackbar").text()).not.toMatch(
      /Added \d+ new users./,
    );
    expect(component.vm.$data.addedWithError).toBe(3);
    expect(component.find(".v-snackbar").text()).toMatch(
      /3 new users\s+already exist./,
    );
    expect(component.vm.$data.dialog).toBeTruthy();
  });
});
