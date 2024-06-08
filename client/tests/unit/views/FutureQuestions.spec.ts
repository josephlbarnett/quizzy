import { mount } from "@vue/test-utils";
import FutureQuestions from "@/views/FutureQuestions.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import vuetify from "@/plugins/vuetify";
import VueApolloPlugin from "@vue/apollo-components";
import futureQuestionsQuery from "@/graphql/FutureQuestions.gql";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import saveQuestionMutation from "@/graphql/SaveQuestion.gql";
import moment from "moment-timezone";
import { awaitVm } from "../TestUtils";
import { ApiQuestion, ApiUser, QuestionType } from "@/generated/types.d";
import { describe, expect, it, vi } from "vitest";
import { createProvider } from "@/vue-apollo";
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
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    body: "Q1",
    activeAt: "2020-01-01T00:00:00Z",
    closedAt: "2020-01-03T00:00:00Z",
    answer: "A1",
    ruleReferences: "Ref1",
  },
  {
    id: 124,
    authorId: 456,
    author: mockUser,
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    body: "Q2",
    activeAt: "2020-01-03T00:00:00Z",
    closedAt: "2020-01-07T00:00:00Z",
    answer: "A2",
    ruleReferences: "Ref2",
  },
];

async function mountFutureQuestions(mockClient: MockApolloClient) {
  const page = mount(FutureQuestions, {
    props: {
      inTest: true,
    },
    global: {
      stubs: ["date-time-picker"],
      plugins: [
        vuetify,
        VueApolloPlugin,
        createProvider({
          defaultClient: mockClient,
        }),
      ],
    },
  });
  await awaitVm(page);
  return page;
}

describe("Future Questions page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      futureQuestionsQuery,
      () =>
        new Promise(() => {
          // never resolve
        }),
    );
    const page = await mountFutureQuestions(mockClient);
    expect(page.findComponent(VProgressCircular).vm.$props.indeterminate).toBe(
      true,
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(futureQuestionsQuery, () =>
      Promise.resolve({ data: null, errors: [{ message: "Error" }] }),
    );
    const page = await mountFutureQuestions(mockClient);
    expect(page.text()).toBe("An error occurred");
  });

  it("null TZ handled", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(futureQuestionsQuery, () =>
      Promise.resolve({ data: { futureQuestions: mockQuestions } }),
    );
    const nullTZUser = JSON.parse(JSON.stringify(mockUser));
    nullTZUser.timeZoneId = null;
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: nullTZUser } }),
    );
    const page = await mountFutureQuestions(mockClient);
    expect(page.vm.$data.timezone).toBe("Autodetect");
  });

  it("renders grid", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    mockClient.setRequestHandler(futureQuestionsQuery, () =>
      Promise.resolve({ data: { futureQuestions: mockQuestions } }),
    );
    const page = await mountFutureQuestions(mockClient);
    expect(page.vm.$data.timezone).toBe("UTC");
    const rows = page.findAll("tbody tr");
    expect(rows.length).toBe(mockQuestions.length);
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const cols = row.findAll("td");
      expect(cols[0].text()).toContain(
        moment.tz(mockQuestions[i].activeAt, "UTC").format("ddd, MMM D YYYY"),
      );
      expect(cols[1].text()).toContain(
        `${moment
          .tz(mockQuestions[i].closedAt, "UTC")
          .format("ddd, MMM D YYYY, h:mmA")} (UTC)`,
      );
      expect(cols[2].text()).toContain(mockQuestions[i].body);
    }
  });

  it("renders dialog", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    mockClient.setRequestHandler(futureQuestionsQuery, () =>
      Promise.resolve({ data: { futureQuestions: mockQuestions } }),
    );
    const page = await mountFutureQuestions(mockClient);
    const table = page.findComponent(VDataTable);

    for (let i = 0; i < mockQuestions.length; i++) {
      await table.findAllComponents(VDataTableRow)[i].trigger("click");
      expect(page.vm.$data.addDialog).toBe(true);
      expect(page.vm.$data.addDialogId).toBe(mockQuestions[i].id);
      expect(page.vm.$data.addDialogBody).toBe(mockQuestions[i].body);
      expect(page.vm.$data.addDialogAnswer).toBe(mockQuestions[i].answer);
      expect(page.vm.$data.addDialogAuthor).toBe(mockQuestions[i].authorId);
      expect(page.vm.$data.addDialogActive).toBe(mockQuestions[i].activeAt);
      expect(page.vm.$data.addDialogClose).toBe(mockQuestions[i].closedAt);
      expect(page.vm.$data.addDialogRuleReferences).toBe(
        mockQuestions[i].ruleReferences,
      );
      await page
        .findAll(".v-dialog button")
        .filter((x) => x.text() == "CANCEL")[0]
        .trigger("click");
      expect(page.vm.$data.addDialog).toBe(false);
    }
    await page
      .findAll("button")
      .filter((x) => x.text() == "ADD QUESTION")[0]
      .trigger("click");
    expect(page.vm.$data.addDialog).toBe(true);
    expect(page.vm.$data.addDialogId).toBe(null);
    expect(page.vm.$data.addDialogBody).toBe("");
    expect(page.vm.$data.addDialogAnswer).toBe("");
    expect(page.vm.$data.addDialogAuthor).toBe("");
    expect(page.vm.$data.addDialogActive).toBe("");
    expect(page.vm.$data.addDialogClose).toBe("");
    expect(page.vm.$data.addDialogRuleReferences).toBe("");
  });

  it("submits form", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    mockClient.setRequestHandler(futureQuestionsQuery, () =>
      Promise.resolve({ data: { futureQuestions: mockQuestions } }),
    );
    const mockMutation = vi.fn();
    mockClient.setRequestHandler(saveQuestionMutation, mockMutation);
    const page = await mountFutureQuestions(mockClient);
    const table = page.findComponent(VDataTable);
    await table.findComponent(VDataTableRow).trigger("click");
    await awaitVm(page);
    const bodyInput = page.findAll(".v-dialog .v-textarea")[0].find("textarea");
    expect((bodyInput.element as HTMLTextAreaElement).value).toBe(
      mockQuestions[0].body,
    );
    await bodyInput.setValue(`Q1V2`);
    const answerInput = page
      .findAll(".v-dialog .v-textarea")[1]
      .find("textarea");
    expect((answerInput.element as HTMLTextAreaElement).value).toBe(
      mockQuestions[0].answer,
    );
    await answerInput.setValue(`A1V2`);
    const ruleRefInput = page
      .findAll(".v-dialog .v-textarea")[2]
      .find("textarea");
    expect((ruleRefInput.element as HTMLTextAreaElement).value).toBe(
      mockQuestions[0].ruleReferences,
    );
    await ruleRefInput.setValue(`Ref1V2`);
    await page
      .findAll(".v-dialog button")
      .filter((x) => x.text() == "SAVE")[0]
      .trigger("click");
    expect(mockMutation.mock.calls.length).toBe(1);
    expect(mockMutation.mock.calls[0][0].id).toBe(mockQuestions[0].id);
    expect(mockMutation.mock.calls[0][0].body).toBe("Q1V2");
    expect(mockMutation.mock.calls[0][0].answer).toBe("A1V2");
    expect(mockMutation.mock.calls[0][0].ruleReferences).toBe("Ref1V2");
  });
});
