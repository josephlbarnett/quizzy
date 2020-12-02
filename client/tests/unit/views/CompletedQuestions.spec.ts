import { mount, Wrapper } from "@vue/test-utils";
import CompletedQuestions from "@/views/CompletedQuestions.vue";
import { createMockClient, MockApolloClient } from "mock-apollo-client";
import VueApollo from "vue-apollo";
import moment from "moment-timezone";
import completedQuestionQuery from "@/graphql/CompletedQuestions.gql";
import currentUserQuery from "@/graphql/CurrentUser.gql";
import vuetify from "@/plugins/vuetify";
import { ApiQuestion } from "@/generated/types";
import Vue from "vue";
// silence a VDialog warning!?
document.body.setAttribute("data-app", "true");

const mockUser = {
  id: 987,
  instanceId: 111,
  instance: {
    id: 111,
    name: "instance",
    status: "ACTIVE",
  },
  email: "me@me.com",
  name: "me",
  admin: false,
  timeZoneId: "UTC",
  notifyViaEmail: false,
  score: 18,
};

const mockQuestions = [
  {
    id: 123,
    authorId: 456,
    author: mockUser,
    body: "Q1",
    activeAt: "2020-01-01T00:00:00Z",
    closedAt: "2020-01-03T00:00:00Z",
    answer: "A1",
    ruleReferences: "Ref1",
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

function mountCompletedQuestions(mockClient: MockApolloClient) {
  return mount(CompletedQuestions, {
    stubs: [],
    vuetify,
    apolloProvider: new VueApollo({
      defaultClient: mockClient,
    }),
  });
}

function assertDialogMatchesInputs<T extends Vue>(
  page: Wrapper<T>,
  question: ApiQuestion
) {
  expect(page.find(".v-dialog .v-card__title").text()).toContain(
    `Review Question: ${moment
      .tz(question.activeAt, "UTC")
      .format("ddd, MMM D YYYY")} by`
  );
  expect(page.find(".v-dialog .v-card__title").text()).toContain(
    question.author?.name || "no author!?"
  );
  expect(page.find(".v-dialog .v-card__text").text()).toMatch(
    new RegExp(`${question.body}.*`)
  );
  const inputs = page.findAll(".v-dialog .v-textarea");
  expect(
    (inputs
      .filter((x) => x.text() == "Answer Key")
      .at(0)
      .find("textarea").element as HTMLTextAreaElement).value
  ).toBe(question.answer);
  expect(
    (inputs
      .filter((x) => x.text() == "Answer Key Rule References")
      .at(0)
      .find("textarea").element as HTMLTextAreaElement).value
  ).toBe(question.ruleReferences);
  expect(
    (inputs
      .filter((x) => x.text() == "Your Response")
      .at(0)
      .find("textarea").element as HTMLTextAreaElement).value
  ).toBe(question.response?.response);
  expect(
    (inputs
      .filter((x) => x.text() == "Your Rule References")
      .at(0)
      .find("textarea").element as HTMLTextAreaElement).value
  ).toBe(question.response?.ruleReferences);
  const correctness = question.response?.grade?.correct;
  if (correctness === undefined || correctness === null) {
    expect(
      page
        .findAll(".v-dialog .v-card__text div.row")
        .filter((x) => x.text().startsWith("Correct?:"))
        .at(0)
        .text()
    ).toMatch(new RegExp(`.*Ungraded`));
    expect(
      page
        .findAll(".v-dialog .v-card__text div.row")
        .filter((x) => x.text().startsWith("Bonus:"))
        .at(0)
        .text()
    ).toBe("Bonus:");
    expect(
      page
        .findAll(".v-dialog .v-card__text div.row")
        .filter((x) => x.text().startsWith("Score:"))
        .at(0)
        .text()
    ).toBe("Score:");
  } else {
    if (correctness) {
      expect(
        page
          .findAll(".v-dialog .v-card__text div.row")
          .filter((x) => x.text().startsWith("Correct?:"))
          .at(0)
          .text()
      ).toMatch(new RegExp(`.*Yes`));
    } else {
      expect(
        page
          .findAll(".v-dialog .v-card__text div.row")
          .filter((x) => x.text().startsWith("Correct?:"))
          .at(0)
          .text()
      ).toMatch(new RegExp(`.*No`));
    }
    expect(
      page
        .findAll(".v-dialog .v-card__text div.row")
        .filter((x) => x.text().startsWith("Bonus:"))
        .at(0)
        .text()
    ).toMatch(new RegExp(`.*${question.response?.grade?.bonus || 0}`));
    expect(
      page
        .findAll(".v-dialog .v-card__text div.row")
        .filter((x) => x.text().startsWith("Score:"))
        .at(0)
        .text()
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
        })
    );
    const page = mountCompletedQuestions(mockClient);
    await page.vm.$nextTick();
    expect(page.find(".v-progress-circular").vm.$props.indeterminate).toBe(
      true
    );
  });

  it("error state", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ errors: [{ message: "Some Error" }], data: null })
    );
    const page = mountCompletedQuestions(mockClient);
    await page.vm.$nextTick();
    await page.vm.$nextTick();
    expect(page.text()).toBe("An error occurred");
  });

  it("renders grid", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } })
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const page = mountCompletedQuestions(mockClient);
    await page.vm.$nextTick();
    await page.vm.$nextTick();
    expect(page.vm.$data.userTZ).toBe("UTC");
    const rows = page.findAll("tbody tr");
    expect(rows.length).toBe(mockQuestions.length);
    for (let i = 0; i < rows.length; i++) {
      const row = rows.at(i);
      const cols = row.findAll("td");
      expect(cols.at(0).text()).toBe(
        moment.tz(mockQuestions[i].activeAt, "UTC").format("ddd, MMM D YYYY")
      );
      expect(cols.at(1).text()).toBe(mockQuestions[i].body);
      expect(cols.at(2).text()).toBe(mockQuestions[i].answer);
      expect(cols.at(3).text()).toBe(mockQuestions[i].response.response);
      if (!mockQuestions[i].response.grade) {
        expect(cols.at(4).find("i").classes()).not.toContain(
          "mdi-check-circle"
        );
        expect(cols.at(4).find("i").classes()).not.toContain(
          "mdi-close-circle"
        );
        expect(cols.at(5).text()).toBe("");
      } else {
        expect(cols.at(4).find("i").classes()).toContain(
          mockQuestions[i].response.grade?.correct
            ? "mdi-check-circle"
            : "mdi-close-circle"
        );
        expect(cols.at(5).text()).toBe(
          mockQuestions[i].response.grade?.score.toString()
        );
      }
    }
  });

  it("renders dialog", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } })
    );
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: mockUser } })
    );
    const page = mountCompletedQuestions(mockClient);
    await page.vm.$nextTick();
    await page.vm.$nextTick();
    const table = page.find(".v-data-table");

    for (let i = 0; i < mockQuestions.length; i++) {
      table.vm.$emit("click:row", mockQuestions[i]);
      await page.vm.$nextTick();
      expect(page.vm.$data.clickedQuestion).toBe(mockQuestions[i]);
      assertDialogMatchesInputs(page, mockQuestions[i]);
      await page.find(".v-dialog button").trigger("click");
    }
  });

  it("null TZ handled", async () => {
    const mockClient = createMockClient();
    mockClient.setRequestHandler(completedQuestionQuery, () =>
      Promise.resolve({ data: { closedQuestions: mockQuestions } })
    );
    const nullTZUser = JSON.parse(JSON.stringify(mockUser));
    nullTZUser.timeZoneId = null;
    mockClient.setRequestHandler(currentUserQuery, () =>
      Promise.resolve({ data: { user: nullTZUser } })
    );
    const page = mountCompletedQuestions(mockClient);
    await page.vm.$nextTick();
    expect(page.vm.$data.userTZ).toBe("Autodetect");
  });
});
