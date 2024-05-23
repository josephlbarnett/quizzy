import { shallowMount } from "@vue/test-utils";
import DateTimePicker from "@/components/DateTimePicker.vue";
import moment from "moment";
import { describe, expect, it } from "vitest";

describe("DateTimePicker Tests", () => {
  it("initializes properly", () => {
    const initializedPicker = shallowMount(DateTimePicker, {
      props: {
        value: "2020-07-23 13:22:01",
        timezone: "UTC",
        label: "some_text",
      },
    });
    const vm = initializedPicker.vm;
    expect(moment(vm.date).format("YYYY-MM-DD")).toBe("2020-07-23");
    expect(vm.time).toBe("13:22");
    expect(vm.dateTime).toBe("2020-07-23T13:22:00Z");
    expect(vm.dateMenu).toBe(false);
    expect(vm.timeMenu).toBe(false);
  });
  it("initializes empty", () => {
    const invalidPicker = shallowMount(DateTimePicker, {
      props: {
        value: "Not a Timestamp!",
        timezone: "Autodetect",
        label: "some_text",
      },
    });
    const vm = invalidPicker.vm;
    expect(vm.date).toBe(null);
    expect(vm.time).toBe("");
    expect(vm.dateTime).toBe("Invalid date");
    expect(vm.dateMenu).toBe(false);
    expect(vm.timeMenu).toBe(false);
  });

  it("renders pacific date/time across UTC boundary", () => {
    const picker = shallowMount(DateTimePicker, {
      props: {
        timezone: "UTC",
        label: "some_text",
      },
    });
    expect(picker.vm.renderTime("2020-07-23 17:22:01-0700")).toBe(
      "12:22 AM (UTC)",
    );
    expect(picker.vm.renderDate("2020-07-23 17:22:01-0700")).toBe("07/24/2020");
  });

  it("renders invalid datetime", () => {
    const picker = shallowMount(DateTimePicker, {
      props: {
        timezone: "UTC",
        label: "some_text",
      },
    });
    const vm = picker.vm;
    expect(vm.renderTime("Not a Timestamp!")).toBe("--:-- --");
    expect(vm.renderDate("Not a Timestamp!")).toBe("mm/dd/yyyy");
  });

  it("emits update values when pickers are changed", () => {
    const picker = shallowMount(DateTimePicker, {
      props: {
        value: "2020-07-23 13:22:01",
        timezone: "UTC",
        label: "some_text",
      },
    });
    picker.vm.onChange(false);
    picker.vm.date = new Date("2020-07-22T00:00:00");
    picker.vm.onChange(false);
    picker.vm.time = "15:27";
    picker.vm.onChange(false);
    expect(picker.emitted("update:modelValue")).toHaveLength(3);
    expect(picker.emitted("update:modelValue")[0]).toEqual([
      "2020-07-23T13:22:00Z",
    ]);
    expect(picker.emitted("update:modelValue")[1]).toEqual([
      "2020-07-22T13:22:00Z",
    ]);
    expect(picker.emitted("update:modelValue")[2]).toEqual([
      "2020-07-22T15:27:00Z",
    ]);
  });

  it("close date opens time", () => {
    const picker = shallowMount(DateTimePicker, {
      props: {
        timezone: "UTC",
        label: "some_text",
      },
    });
    picker.vm.dateClicked();
    expect(picker.vm.dateMenu).toBeFalsy();
    expect(picker.vm.timeMenu).toBeTruthy();
  });
});
