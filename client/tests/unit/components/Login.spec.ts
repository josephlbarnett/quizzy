import { mount } from "@vue/test-utils";
import Login from "@/components/Login.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import loginMutation from "@/graphql/Login.gql";
import requestPasswordResetMutation from "@/graphql/RequestPasswordReset.gql";
import completePasswordResetMutation from "@/graphql/CompletePasswordReset.gql";
import { awaitVm } from "../TestUtils";
import { ApiUser, QuestionType } from "@/generated/types.d";

const mockUser: ApiUser = {
  id: 123,
  instanceId: 456,
  instance: {
    id: 789,
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
  admin: false,
  timeZoneId: "America/Los_Angeles",
  notifyViaEmail: false,
  score: 15,
  __typename: "ApiUser",
};

async function mountLogin(
  mockClient: MockApolloClient,
  mockRoute = { query: {}, path: "/" },
  mockRouter = { push: vi.fn() },
) {
  const component = mount(Login, {
    stubs: ["router-view", "v-snackbar", "router-link"],
    apolloProvider: new VueApollo({
      defaultClient: mockClient,
    }),
    mocks: {
      $route: mockRoute,
      $router: mockRouter,
    },
  });
  await awaitVm(component);
  return component;
}

describe("Login Tests", () => {
  it("shows router stub when user returned", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const login = await mountLogin(mockClient);
    expect(login.find("router-view-stub")).toBeTruthy();
  });

  it("shows loading icon", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      currentUserQuery,
      () =>
        new Promise(() => {
          // never fulfill promise
        }),
    );
    const login = await mountLogin(mockClient);
    expect(login.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true,
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ errors: [{ message: "Some Error" }], data: null }),
    );
    const login = await mountLogin(mockClient);
    expect(login.text()).toBe("An error occurred");
  });

  it("shows login screen when no user", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const login = await mountLogin(mockClient);
    const inputs = login.findAll(".v-text-field");
    expect(inputs.length).toBe(2);
    const userInput = inputs.at(0);
    const passInput = inputs.at(1);
    expect(userInput.find("input").element.getAttribute("type")).toBe("text");
    expect(passInput.find("input").element.getAttribute("type")).toBe(
      "password",
    );
    expect(login.find("button").element.getAttribute("label")).toBe("Login");
  });

  it("clicking login calls mutate and shows error", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const mutationMock = vi.fn((arg) =>
      Promise.resolve({ data: { login: false }, passthrough: arg }),
    );
    mockClient.setRequestHandler(loginMutation, mutationMock);
    const login = await mountLogin(mockClient);
    login.setData({ email: "joe@joe.com", pass: "secret" });
    await awaitVm(login);
    const button = login.find("button");
    await button.trigger("click");
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].email).toBe("joe@joe.com");
    expect(mutationMock.mock.calls[0][0].pass).toBe("secret");
    await awaitVm(login);
    expect(login.find("v-snackbar-stub").text()).toBe(
      "Couldn't login, try again.",
    );
  });

  it("pressing enter calls mutate and  shows error", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );

    const mutationMock = vi.fn(() =>
      Promise.resolve({ data: { login: false } }),
    );
    mockClient.setRequestHandler(loginMutation, mutationMock);

    const login = await mountLogin(mockClient);
    const userInput = login.findAll(".v-text-field").at(0);
    await userInput.vm.$emit("keypress", { key: "a" });
    expect(mutationMock.mock.calls.length).toBe(0);
    await userInput.vm.$emit("keypress", { key: "Enter" });
    expect(mutationMock.mock.calls.length).toBe(1);
    await awaitVm(login);
    expect(login.find("v-snackbar-stub").text()).toBe(
      "Couldn't login, try again.",
    );
  });

  it("init of password reset flow", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const mutationMock = vi.fn((arg) =>
      Promise.resolve({
        data: { requestPasswordReset: true },
        passthrough: arg,
      }),
    );
    mockClient.setRequestHandler(requestPasswordResetMutation, mutationMock);
    const pushMock = vi.fn();
    const login = await mountLogin(
      mockClient,
      { query: {}, path: "/initreset" },
      { push: pushMock },
    );
    login.setData({ email: "joe@joe.com" });
    await awaitVm(login);
    const button = login.find("button");
    await button.trigger("click");
    await awaitVm(login);
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].email).toBe("joe@joe.com");
    expect(pushMock.mock.calls.length).toBe(1);
    expect(pushMock.mock.calls[0][0]).toBe("/passreset");
  });

  it("failed init of password reset flow", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const mutationMock = vi.fn((arg) =>
      Promise.resolve({
        data: { requestPasswordReset: false },
        passthrough: arg,
      }),
    );
    mockClient.setRequestHandler(requestPasswordResetMutation, mutationMock);
    const pushMock = vi.fn();
    const login = await mountLogin(
      mockClient,
      { query: {}, path: "/initreset" },
      { push: pushMock },
    );
    login.setData({ email: "joe@joe.com" });
    await awaitVm(login);
    const button = login.find("button");
    await button.trigger("click");
    await awaitVm(login);
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].email).toBe("joe@joe.com");
    expect(pushMock.mock.calls.length).toBe(0);
  });

  it("completion of password reset flow", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const mutationMock = vi.fn((arg) =>
      Promise.resolve({
        data: { completePasswordReset: true },
        passthrough: arg,
      }),
    );
    mockClient.setRequestHandler(completePasswordResetMutation, mutationMock);
    const pushMock = vi.fn();
    const login = await mountLogin(
      mockClient,
      { query: { code: "123", email: "joe@joe.com" }, path: "/passreset" },
      { push: pushMock },
    );
    const button = login.find("button");
    const pw1Input = login
      .findAll(".v-input")
      .filter((x) => x.text() == "New Password")
      .at(0);
    const pw2Input = login
      .findAll(".v-input")
      .filter((x) => x.text() == "Confirm New Password")
      .at(0);
    await pw1Input.find("input").setValue("456");
    await button.trigger("click");
    await login.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(0);
    await pw2Input.find("input").setValue("456");
    await button.trigger("click");
    await awaitVm(login);
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].email).toBe("joe@joe.com");
    expect(mutationMock.mock.calls[0][0].code).toBe("123");
    expect(mutationMock.mock.calls[0][0].newPass).toBe("456");
    expect(pushMock.mock.calls.length).toBe(1);
    expect(pushMock.mock.calls[0][0]).toBe("/");
  });

  it("failed completion password reset flow", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } }),
    );
    const mutationMock = vi.fn((arg) =>
      Promise.resolve({
        data: { completePasswordReset: false },
        passthrough: arg,
      }),
    );
    mockClient.setRequestHandler(completePasswordResetMutation, mutationMock);
    const pushMock = vi.fn();
    const login = await mountLogin(
      mockClient,
      { query: { code: "123", email: "joe@joe.com" }, path: "/passreset" },
      { push: pushMock },
    );
    const button = login.find("button");
    const pw1Input = login
      .findAll(".v-input")
      .filter((x) => x.text() == "New Password")
      .at(0);
    const pw2Input = login
      .findAll(".v-input")
      .filter((x) => x.text() == "Confirm New Password")
      .at(0);
    await pw1Input.find("input").setValue("456");
    await pw2Input.find("input").setValue("456");
    await button.trigger("click");
    await awaitVm(login);
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].email).toBe("joe@joe.com");
    expect(mutationMock.mock.calls[0][0].code).toBe("123");
    expect(mutationMock.mock.calls[0][0].newPass).toBe("456");
    expect(pushMock.mock.calls.length).toBe(0);
    expect(login.vm.$data.failedReset).toBe(true);
  });
});
