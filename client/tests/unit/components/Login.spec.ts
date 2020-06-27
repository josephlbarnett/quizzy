import { mount } from "@vue/test-utils";
import Login from "@/components/Login.vue";
import { createMockClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import loginMutation from "@/graphql/Login.gql";
const mockUser = {
  id: 123,
  instanceId: 456,
  instance: {
    id: 789,
    name: "Tenant",
    __typename: "Instance",
  },
  email: "joe@test.com",
  name: "joe test",
  admin: false,
  timeZoneId: "America/Los_Angeles",
  __typename: "ApiUser",
};

describe("Login Tests", () => {
  it("shows router stub when user returned", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const login = mount(Login, {
      stubs: ["router-view"],
      created() {
        this.$apolloProvider = new VueApollo({
          defaultClient: mockClient,
        });
      },
    });
    expect(login.find("router-view-stub")).toBeTruthy();
  });

  it("shows loading icon", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      currentUserQuery,
      () =>
        new Promise(() => {
          // never fulfill promise
        })
    );
    const login = mount(Login, {
      stubs: ["router-view"],
      created() {
        this.$apolloProvider = new VueApollo({
          defaultClient: mockClient,
        });
      },
    });
    expect(login.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true
    );
  });

  it("shows login screen when no user", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } })
    );
    const login = mount(Login, {
      stubs: ["router-view", "v-snackbar"],
      created() {
        this.$apolloProvider = new VueApollo({
          defaultClient: mockClient,
        });
      },
    });
    await login.vm.$nextTick();
    const inputs = login.findAll(".v-text-field");
    expect(inputs.length).toBe(2);
    const userInput = inputs.at(0);
    const passInput = inputs.at(1);
    expect(userInput.find("input").element.getAttribute("type")).toBe("text");
    expect(passInput.find("input").element.getAttribute("type")).toBe(
      "password"
    );
    expect(login.find("button").element.getAttribute("label")).toBe("Login");
  });

  it("clicking login calls mutate and shows error", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } })
    );
    const mutationMock = jest.fn(() =>
      Promise.resolve({ data: { login: false } })
    );
    mockClient.setRequestHandler(loginMutation, mutationMock);
    const login = mount(Login, {
      stubs: ["router-view", "v-snackbar"],
      created() {
        this.$apolloProvider = new VueApollo({
          defaultClient: mockClient,
        });
      },
    });
    await login.vm.$nextTick();
    const button = login.find("button");
    await button.trigger("click");
    expect(mutationMock.mock.calls.length).toBe(1);
    await login.vm.$nextTick();
    await login.vm.$nextTick();
    expect(login.find("v-snackbar-stub").text()).toBe(
      "Couldn't log in, try again."
    );
  });

  it("pressing enter calls mutate and  shows error", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: null } })
    );

    const mutationMock = jest.fn(() =>
      Promise.resolve({ data: { login: false } })
    );
    mockClient.setRequestHandler(loginMutation, mutationMock);

    const login = mount(Login, {
      stubs: ["router-view", "v-snackbar"],
      created() {
        this.$apolloProvider = new VueApollo({
          defaultClient: mockClient,
        });
      },
    });
    await login.vm.$nextTick();
    const userInput = login.findAll(".v-text-field").at(0);
    await userInput.vm.$emit("keypress", { key: "a" });
    expect(mutationMock.mock.calls.length).toBe(0);
    await userInput.vm.$emit("keypress", { key: "Enter" });
    expect(mutationMock.mock.calls.length).toBe(1);
    await login.vm.$nextTick();
    await login.vm.$nextTick();
    expect(login.find("v-snackbar-stub").text()).toBe(
      "Couldn't log in, try again."
    );
  });
});
