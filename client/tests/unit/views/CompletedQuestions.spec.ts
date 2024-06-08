import { mount, VueWrapper } from "@vue/test-utils";
import CompletedQuestions from "@/views/CompletedQuestions.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import moment from "moment-timezone";
import completedQuestionQuery from "@/graphql/CompletedQuestions.gql";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import vuetify from "@/plugins/vuetify";
import { ApiQuestion, ApiUser, QuestionType } from "@/generated/types.d";
import { awaitVm } from "../TestUtils";
import { createPinia } from "pinia";
import { createProvider } from "@/vue-apollo";
import VueApolloPlugin from "@vue/apollo-components";
import { describe, expect, it } from "vitest";
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
    answer: "A1",
    ruleReferences: "Ref1",
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    response: {
      id: 789,
      response: "Resp1",
      ruleReferences: "RespRef1",
      grade: {
        id: 654,
        correct: false,
        bonus: null,
        score: 0,
        responseId: 789,
      },
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
    answer: "A2",
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    ruleReferences: "Ref2",
    response: {
      id: 790,
      response: "Resp2",
      ruleReferences: "RespRef2",
      grade: {
        id: 655,
        correct: true,
        bonus: 3,
        score: 18,
        responseId: 790,
      },
      user: mockUser,
      userId: mockUser.id,
      questionId: 124,
    },
  },
  {
    id: 125,
    authorId: 456,
    author: mockUser,
    body: "Q3",
    activeAt: "2020-01-03T00:00:00Z",
    closedAt: "2020-01-07T00:00:00Z",
    answer: "A3",
    type: QuestionType.ShortAnswer,
    imageUrl: null,
    answerChoices: [],
    ruleReferences: "Ref3",
    response: {
      id: 791,
      response: "Resp3",
      ruleReferences: "RespRef3",
      grade: null,
      user: mockUser,
      userId: mockUser.id,
      questionId: 125,
    },
  },
];

async function mountCompletedQuestions(mockClient: MockApolloClient) {
  const page = mount(CompletedQuestions, {
    props: {
      inTest: true,
    },
    global: {
      stubs: [],
      plugins: [
        VueApolloPlugin,
        vuetify,
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

function assertDialogMatchesInputs<T>(
  page: VueWrapper<T>,
  question: ApiQuestion,
) {
  expect(page.find(".v-dialog .v-card-title").text()).toContain(
    `Review Question: ${moment
      .tz(question.activeAt, "UTC")
      .format("ddd, MMM D YYYY")} by`,
  );
  expect(page.find(".v-dialog .v-card-title").text()).toContain(
    question.author?.name || "no author!?",
  );
  expect(page.find(".v-dialog .v-card-text").text()).toMatch(
    new RegExp(`${question.body}.*`),
  );
  const inputs = page.findAll(".v-dialog .v-textarea");
  expect(
    (
      inputs.filter((x) => x.text().endsWith("Answer Key"))[0].find("textarea")
        .element as HTMLTextAreaElement
    ).value,
  ).toBe(question.answer);
  expect(
    (
      inputs
        .filter((x) => x.text().endsWith("Answer Key Rule References"))[0]
        .find("textarea").element as HTMLTextAreaElement
    ).value,
  ).toBe(question.ruleReferences);
  expect(
    (
      inputs
        .filter((x) => x.text().endsWith("Your Response"))[0]
        .find("textarea").element as HTMLTextAreaElement
    ).value,
  ).toBe(question.response?.response);
  expect(
    (
      inputs
        .filter((x) => x.text().endsWith("Your Rule References"))[0]
        .find("textarea").element as HTMLTextAreaElement
    ).value,
  ).toBe(question.response?.ruleReferences);
  const correctness = question.response?.grade?.correct;
  if (correctness === undefined || correctness === null) {
    expect(
      page
        .findAll(".v-dialog .v-card-text div.v-row")
        .filter((x) => x.text().startsWith("Correct?:"))[0]
        .text(),
    ).toMatch(new RegExp(`.*Ungraded`));
    expect(
      page
        .findAll(".v-dialog .v-card-text div.v-row")
        .filter((x) => x.text().startsWith("Bonus:"))[0]
        .text(),
    ).toBe("Bonus:");
    expect(
      page
        .findAll(".v-dialog .v-card-text div.v-row")
        .filter((x) => x.text().startsWith("Score:"))[0]
        .text(),
    ).toBe("Score:");
  } else {
    if (correctness) {
      expect(
        page
          .findAll(".v-dialog .v-card-text div.v-row")
          .filter((x) => x.text().startsWith("Correct?:"))[0]
          .text(),
      ).toMatch(new RegExp(`.*Yes`));
    } else {
      expect(
        page
          .findAll(".v-dialog .v-card-text div.v-row")
          .filter((x) => x.text().startsWith("Correct?:"))[0]
          .text(),
      ).toMatch(new RegExp(`.*No`));
    }
    expect(
      page
        .findAll(".v-dialog .v-card-text div.v-row")
        .filter((x) => x.text().startsWith("Bonus:"))[0]
        .text(),
    ).toMatch(new RegExp(`.*${question.response?.grade?.bonus || 0}`));
    expect(
      page
        .findAll(".v-dialog .v-card-text div.v-row")
        .filter((x) => x.text().startsWith("Score:"))[0]
        .text(),
    ).toMatch(new RegExp(`.*${question.response?.grade?.score || 0}`));
  }
}

describe("Completed Questions page tests", () => {
  it("loading state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(
      completedQuestionQuery,
      () =>
        new Promise(() => {
          // never resolve
        }),
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const page = await mountCompletedQuestions(mockClient);
    expect(page.findComponent(VProgressCircular).vm.$props.indeterminate).toBe(
      true,
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ errors: [{ message: "Some Error" }], data: null }),
    );
    const page = await mountCompletedQuestions(mockClient);
    expect(page.text()).toBe("An error occurred");
  });

  it("renders grid", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } }),
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const page = await mountCompletedQuestions(mockClient);
    expect(page.vm.$data.userTZ).toBe("UTC");
    const rows = page.findAll("tbody tr");
    expect(rows.length).toBe(mockQuestions.length);
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const cols = row.findAll("td");
      expect(cols[0].text()).toContain(
        moment.tz(mockQuestions[i].activeAt, "UTC").format("ddd, MMM D YYYY"),
      );
      expect(cols[1].text()).toContain(mockQuestions[i].body);
      expect(cols[2].text()).toContain(mockQuestions[i].answer);
      expect(cols[3].text()).toContain(mockQuestions[i].response?.response);
      if (!mockQuestions[i].response?.grade) {
        expect(cols[4].find("i").classes()).not.toContain("mdi-check-circle");
        expect(cols[4].find("i").classes()).not.toContain("mdi-close-circle");
        expect(cols[5].text()).not.toMatch("[0-9]*");
      } else {
        expect(cols[4].find("i").classes()).toContain(
          mockQuestions[i].response?.grade?.correct
            ? "mdi-check-circle"
            : "mdi-close-circle",
        );
        expect(cols[5].text()).toContain(
          mockQuestions[i].response?.grade?.score.toString(),
        );
      }
    }
  });

  it("renders dialog", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } }),
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } }),
    );
    const page = await mountCompletedQuestions(mockClient);
    const table = page.findComponent(VDataTable);

    for (let i = 0; i < mockQuestions.length; i++) {
      table.findAllComponents(VDataTableRow)[i].vm.$emit("click");
      await awaitVm(page);
      expect(page.vm.$data.clickedQuestion?.id).toEqual(mockQuestions[i].id);
      assertDialogMatchesInputs(page, mockQuestions[i]);
      await page.find(".v-dialog button").trigger("click");
    }
  });

  it("null TZ handled", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } }),
    );
    const nullTZUser = JSON.parse(JSON.stringify(mockUser));
    nullTZUser.timeZoneId = null;
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: nullTZUser } }),
    );
    const page = await mountCompletedQuestions(mockClient);
    expect(page.vm.$data.userTZ).toBe("Autodetect");
  });
});
