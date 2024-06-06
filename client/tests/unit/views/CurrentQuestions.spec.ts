import { mount } from "@vue/test-utils";
import CurrentQuestions from "@/views/CurrentQuestions.vue";
import currentQuestionsQuery from "@/graphql/CurrentQuestions.gql";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import saveResponseMutation from "@/graphql/SaveResponse.gql";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import vuetify from "@/plugins/vuetify";
import moment from "moment-timezone";
import completedQuestionQuery from "@/graphql/CompletedQuestions.gql";
import { awaitVm } from "../TestUtils";
import { ApiQuestion, ApiUser, QuestionType } from "@/generated/types.d";
import { describe, expect, it, vi } from "vitest";
import { createProvider } from "@/vue-apollo";
import ApolloVuePlugin from "@vue/apollo-components";
import { VProgressCircular } from "vuetify/components/VProgressCircular";
import { VDataTable, VDataTableRow } from "vuetify/components/VDataTable";

const mockUser: ApiUser = {
  id: 987,
  instanceId: 111,
  instance: {
    id: 111,
    name: "instance",
    defaultQuestionType: QuestionType.ShortAnswer,
    autoGrade: false,
    status: "ACTIVE",
    defaultScore: 15,
    seasons: [],
    supportsGroupMe: true,
  },
  email: "me@me.com",
  name: "me",
  admin: false,
  timeZoneId: "UTC",
  notifyViaEmail: false,
  score: 18,
};

const mockQuestions: ApiQuestion[] = [
  {
    id: 123,
    authorId: 456,
    author: mockUser,
    body: "Q1",
    activeAt: "2020-01-01T00:00:00Z",
    closedAt: "2020-01-03T00:00:00Z",
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    answer: "A1",
    ruleReferences: "Ref1",
    response: {
      id: 789,
      response: "Resp1",
      ruleReferences: "RespRef1",
      grade: null,
      user: mockUser,
      userId: mockUser.id,
      questionId: 123,
    },
  },
  {
    id: 124,
    authorId: 456,
    author: mockUser,
    body: "Q2",
    activeAt: "2020-01-03T00:00:00Z",
    closedAt: "2020-01-07T00:00:00Z",
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    answer: "A2",
    ruleReferences: "Ref2",
    response: null,
  },
];

async function mountCurrentQuestions(mockClient: MockApolloClient) {
  const page = mount(CurrentQuestions, {
    props: {
      inTest: true,
    },
    global: {
      stubs: [],
      plugins: [
        ApolloVuePlugin,
        vuetify,
        createProvider({
          defaultClient: mockClient,
        }),
      ],
    },
  });
  await awaitVm(page);
  return page;
}

describe("Current questions page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      currentQuestionsQuery,
      () =>
        new Promise(() => {
          // never resolve
        }),
    );
    const page = await mountCurrentQuestions(mockClient);
    expect(page.findComponent(VProgressCircular).vm.$props.indeterminate).toBe(
      true,
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentQuestionsQuery, () =>
      Promise.resolve({ data: null, errors: [{ message: "some error" }] }),
    );
    const page = await mountCurrentQuestions(mockClient);
    expect(page.text()).toBe("An error occurred");
  });

  it("loads grid", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentQuestionsQuery, () =>
      Promise.resolve({ data: { activeQuestions: mockQuestions } }),
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const page = await mountCurrentQuestions(mockClient);
    const rows = page.findAll("tbody tr");
    expect(rows.length).toBe(mockQuestions.length);
    for (let i = 0; i < rows.length; i++) {
      const cols = rows[i].findAll("td");
      expect(cols[0].text()).toBe(
        moment.tz(mockQuestions[i].activeAt, "UTC").format("ddd, MMM D YYYY"),
      );
      expect(cols[1].text()).toBe(
        `${moment
          .tz(mockQuestions[i].closedAt, "UTC")
          .format("ddd, MMM D YYYY, h:mmA")} (UTC)`,
      );
      expect(cols[2].text()).toBe(mockQuestions[i].body);
      expect(cols[3].text()).toBe(mockQuestions[i].response?.response || "");
    }
  });

  it("loads and saves dialog", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentQuestionsQuery, () =>
      Promise.resolve({ data: { activeQuestions: mockQuestions } }),
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const mockMutation = vi.fn();
    mockClient.setRequestHandler(saveResponseMutation, mockMutation);
    const page = await mountCurrentQuestions(mockClient);
    const table = page.findComponent(VDataTable);
    for (let i = 0; i < mockQuestions.length; i++) {
      await table.findAllComponents(VDataTableRow)[i].trigger("click");
      await awaitVm(page);
      expect(page.find(".v-dialog .v-card-title").text()).toContain(
        mockQuestions[i].author?.name,
      );
      expect(page.vm.$data.clickedQuestion?.id).toBe(mockQuestions[i].id);
      expect(page.vm.$data.clickedResponse?.id).toBe(
        mockQuestions[i].response?.id,
      );
      const answerInput = page
        .findAll(".v-dialog .v-textarea")
        .filter((x) => x.text().startsWith("Response"))[0]
        .find("textarea");
      expect((answerInput.element as HTMLTextAreaElement).value).toBe(
        mockQuestions[i].response?.response || "",
      );
      await answerInput.setValue(`answer${i}`);
      const referenceInput = page
        .findAll(".v-dialog .v-textarea")
        .filter((x) => x.text().startsWith("Rule Reference"))[0]
        .find("textarea");
      expect((referenceInput.element as HTMLTextAreaElement).value).toBe(
        mockQuestions[i].response?.ruleReferences || "",
      );
      await referenceInput.setValue(`reference${i}`);
      await page
        .findAll(".v-dialog button")
        .filter((x) => x.text() == "save response")[0]
        .trigger("click");
      expect(mockMutation.mock.calls.length).toBe(i + 1);
      expect(mockMutation.mock.calls[i][0].response).toBe(`answer${i}`);
      expect(mockMutation.mock.calls[i][0].ruleReferences).toBe(
        `reference${i}`,
      );
      expect(mockMutation.mock.calls[i][0].id).toBe(
        mockQuestions[i].response?.id,
      );
      expect(mockMutation.mock.calls[i][0].questionId).toBe(
        mockQuestions[i].id,
      );
    }
  });

  it("null TZ handled", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { activeQuestions: mockQuestions } }),
    );
    const nullTZUser = JSON.parse(JSON.stringify(mockUser));
    nullTZUser.timeZoneId = null;
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: nullTZUser } }),
    );
    const page = await mountCurrentQuestions(mockClient);
    expect(page.vm.$data.userTZ).toBe("Autodetect");
  });
});
