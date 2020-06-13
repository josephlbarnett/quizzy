import { shallowMount } from "@vue/test-utils";
import DateTimePicker from "@/components/DateTimePicker.vue";

describe("DateTimePicker Tests", () => {
  it("initializes properly", () => {
    const initializedPicker = shallowMount(DateTimePicker, {
      propsData: {
        value: "2020-07-23 13:22:01",
        timezone: "UTC",
      },
    });
    expect(initializedPicker.vm.date).toBe("2020-07-23");
    expect(initializedPicker.vm.time).toBe("13:22");
    expect(initializedPicker.vm.dateTime).toBe("2020-07-23T13:22:00Z");
    expect(initializedPicker.vm.dateMenu).toBe(false);
    expect(initializedPicker.vm.timeMenu).toBe(false);
  });
  it("initializes empty", () => {
    const invalidPicker = shallowMount(DateTimePicker, {
      propsData: {
        value: "Not a Timestamp!",
        timezone: "Autodetect",
      },
    });
    expect(invalidPicker.vm.date).toBe("");
    expect(invalidPicker.vm.time).toBe("");
    expect(invalidPicker.vm.dateTime).toBe("Invalid date");
    expect(invalidPicker.vm.dateMenu).toBe(false);
    expect(invalidPicker.vm.timeMenu).toBe(false);
  });

  it("renders pacific date/time across UTC boundary", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    expect(picker.vm.renderTime("2020-07-23 17:22:01-0700")).toBe(
      "12:22 AM (UTC)"
    );
    expect(picker.vm.renderDate("2020-07-23 17:22:01-0700")).toBe("07/24/2020");
  });

  it("renders invalid datetime", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    expect(picker.vm.renderTime("Not a Timestamp!")).toBe("--:-- --");
    expect(picker.vm.renderDate("Not a Timestamp!")).toBe("mm/dd/yyyy");
  });

  it("emits update values when pickers are changed", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        value: "2020-07-23 13:22:01",
        timezone: "UTC",
      },
    });
    const inputEvent = jest.fn();
    picker.vm.$on("input", inputEvent);
    picker.vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-23T13:22:00Z");
    picker.vm.date = "2020-07-22";
    picker.vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-22T13:22:00Z");
    picker.vm.time = "15:27";
    picker.vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-22T15:27:00Z");
  });

  it("close date opens time", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    picker.vm.dateClicked();
    expect(picker.vm.dateMenu).toBeFalsy();
    expect(picker.vm.timeMenu).toBeTruthy();
  });
});
