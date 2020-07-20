import { mount } from "@vue/test-utils";
import User from "@/views/User.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import updateUserMutation from "@/graphql/UpdateUser.gql";
import changePasswordMutation from "@/graphql/ChangePassword.gql";

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
  notifyViaEmail: false,
  __typename: "ApiUser",
};

function mountUser(mockClient: MockApolloClient) {
  return mount(User, {
    stubs: ["v-autocomplete", "v-snackbar"],
    apolloProvider: new VueApollo({
      defaultClient: mockClient,
    }),
  });
}

describe("user page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      currentUserQuery,
      () =>
        new Promise(() => {
          // never resolve
        })
    );
    const userPage = mountUser(mockClient);
    expect(userPage.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ errors: [{ message: "Some Error" }], data: null })
    );
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    expect(userPage.text()).toBe("An error occurred");
  });

  it("loads user data properly", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    expect(userPage.vm.$data.timezone).toBe(mockUser.timeZoneId);
    expect(userPage.vm.$data.name).toBe(mockUser.name);
    expect(
      userPage.vm.$data.tzs.map((x: { name: string; value: string }) => x.value)
    ).toContain("Autodetect");
    expect(
      userPage.vm.$data.tzs.map((x: { name: string; value: string }) => x.value)
    ).toContain("America/New_York");
  });

  it("loads bad timezone properly", async () => {
    const userWithBadTimezone = JSON.parse(JSON.stringify(mockUser));
    userWithBadTimezone.timeZoneId = "abacadaba";
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: userWithBadTimezone } })
    );
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    expect(userPage.vm.$data.timezone).toBe("Autodetect");
  });

  it("user form success", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const mutationMock = jest.fn(() =>
      Promise.resolve({ data: { user: mockUser } })
    );
    mockClient.setRequestHandler(updateUserMutation, mutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const button = userPage.findAll("button").at(0);
    await button.trigger("click");
    await userPage.vm.$nextTick();
    await userPage.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(userPage.vm.$data.saveConfirm).toBe(true);
    expect(userPage.vm.$data.saveError).toBe(false);
  });

  it("user form failure", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const mutationMock = jest.fn(() => Promise.reject("Error saving"));
    mockClient.setRequestHandler(updateUserMutation, mutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const button = userPage.findAll("button").at(0);
    await button.trigger("click");
    await userPage.vm.$nextTick();
    await userPage.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(userPage.vm.$data.saveConfirm).toBe(false);
    expect(userPage.vm.$data.saveError).toBe(true);
  });

  it("pass form success", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const mutationMock = jest.fn(() =>
      Promise.resolve({ data: { changePassword: true } })
    );
    mockClient.setRequestHandler(changePasswordMutation, mutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const button = userPage.findAll("button").at(1);
    await button.trigger("click");
    await userPage.vm.$nextTick();
    await userPage.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(userPage.vm.$data.passConfirm).toBe(true);
    expect(userPage.vm.$data.passError).toBe(false);
  });

  it("pass form failure", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const mutationMock = jest.fn((request: { old: string; new: string }) =>
      Promise.resolve({ data: { changePassword: false }, rawInput: request })
    );
    mockClient.setRequestHandler(changePasswordMutation, mutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const oldPass = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "Current Password")
      .at(0);
    const pass1 = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "New Password")
      .at(0);
    const pass2 = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "Confirm New Password")
      .at(0);
    oldPass.find("input").setValue("abcd");
    pass1.find("input").setValue("defg");
    pass2.find("input").setValue("defg");
    await userPage.vm.$nextTick();
    const button = userPage.findAll("button").at(1);
    await button.trigger("click");
    await userPage.vm.$nextTick();
    await userPage.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(mutationMock.mock.calls[0][0].old).toBe("abcd");
    expect(mutationMock.mock.calls[0][0].new).toBe("defg");
    expect(userPage.vm.$data.passConfirm).toBe(false);
    expect(userPage.vm.$data.passError).toBe(true);
  });

  it("pass form mismatch", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const mutationMock = jest.fn((request: { old: string; new: string }) =>
      Promise.resolve({ data: { changePassword: true }, rawInput: request })
    );
    mockClient.setRequestHandler(changePasswordMutation, mutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const oldPass = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "Current Password")
      .at(0);
    const pass1 = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "New Password")
      .at(0);
    const pass2 = userPage
      .findAll(".v-text-field")
      .filter((x) => x.text() == "Confirm New Password")
      .at(0);
    oldPass.find("input").setValue("abcd");
    pass1.find("input").setValue("defg");
    pass2.find("input").setValue("hijkl");
    await userPage.vm.$nextTick();
    const button = userPage.findAll("button").at(1);
    await button.trigger("click");
    await userPage.vm.$nextTick();
    await userPage.vm.$nextTick();
    expect(mutationMock.mock.calls.length).toBe(1);
    expect(userPage.vm.$data.passConfirm).toBe(true);
    expect(userPage.vm.$data.passError).toBe(false);
    expect(mutationMock.mock.calls[0][0].old).toBe("abcd");
    expect(mutationMock.mock.calls[0][0].new).toBe(null);
  });

  it("Enter key submits correct form", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const passMutationMock = jest.fn(() =>
      Promise.resolve({ data: { changePassword: true } })
    );
    mockClient.setRequestHandler(changePasswordMutation, passMutationMock);
    const userMutationMock = jest.fn(() =>
      Promise.resolve({ data: { user: mockUser } })
    );
    mockClient.setRequestHandler(updateUserMutation, userMutationMock);
    const userPage = mountUser(mockClient);
    await userPage.vm.$nextTick();
    const inputs = userPage.findAll(".v-text-field");
    const nameInput = inputs.filter((x) => x.text() == "Name").at(0);
    await nameInput.vm.$emit("keypress", { key: "a" });
    expect(userMutationMock.mock.calls.length).toBe(0);
    await nameInput.vm.$emit("keypress", { key: "Enter" });
    expect(userMutationMock.mock.calls.length).toBe(1);

    const currentPassInput = inputs
      .filter((x) => x.text() == "Current Password")
      .at(0);
    await currentPassInput.vm.$emit("keypress", { key: "a" });
    expect(passMutationMock.mock.calls.length).toBe(0);
    await currentPassInput.vm.$emit("keypress", { key: "Enter" });
    expect(passMutationMock.mock.calls.length).toBe(1);

    const newPassInput = inputs.filter((x) => x.text() == "New Password").at(0);
    await newPassInput.vm.$emit("keypress", { key: "a" });
    expect(passMutationMock.mock.calls.length).toBe(1);
    await newPassInput.vm.$emit("keypress", { key: "Enter" });
    expect(passMutationMock.mock.calls.length).toBe(2);

    const confNewPassInput = inputs
      .filter((x) => x.text() == "Confirm New Password")
      .at(0);
    await confNewPassInput.vm.$emit("keypress", { key: "a" });
    expect(passMutationMock.mock.calls.length).toBe(2);
    await confNewPassInput.vm.$emit("keypress", { key: "Enter" });
    expect(passMutationMock.mock.calls.length).toBe(3);
  });
});
