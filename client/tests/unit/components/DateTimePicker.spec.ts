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
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = initializedPicker.vm as any;
    expect(vm.date).toBe("2020-07-23");
    expect(vm.time).toBe("13:22");
    expect(vm.dateTime).toBe("2020-07-23T13:22:00Z");
    expect(vm.dateMenu).toBe(false);
    expect(vm.timeMenu).toBe(false);
  });
  it("initializes empty", () => {
    const invalidPicker = shallowMount(DateTimePicker, {
      propsData: {
        value: "Not a Timestamp!",
        timezone: "Autodetect",
      },
    });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = invalidPicker.vm as any;
    expect(vm.date).toBe("");
    expect(vm.time).toBe("");
    expect(vm.dateTime).toBe("Invalid date");
    expect(vm.dateMenu).toBe(false);
    expect(vm.timeMenu).toBe(false);
  });

  it("renders pacific date/time across UTC boundary", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = picker.vm as any;
    expect(vm.renderTime("2020-07-23 17:22:01-0700")).toBe("12:22 AM (UTC)");
    expect(vm.renderDate("2020-07-23 17:22:01-0700")).toBe("07/24/2020");
  });

  it("renders invalid datetime", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = picker.vm as any;
    expect(vm.renderTime("Not a Timestamp!")).toBe("--:-- --");
    expect(vm.renderDate("Not a Timestamp!")).toBe("mm/dd/yyyy");
  });

  it("emits update values when pickers are changed", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        value: "2020-07-23 13:22:01",
        timezone: "UTC",
      },
    });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = picker.vm as any;
    const inputEvent = jest.fn();
    vm.$on("input", inputEvent);
    vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-23T13:22:00Z");
    vm.date = "2020-07-22";
    vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-22T13:22:00Z");
    vm.time = "15:27";
    vm.onChange();
    expect(inputEvent).toHaveBeenCalledWith("2020-07-22T15:27:00Z");
  });

  it("close date opens time", () => {
    const picker = shallowMount(DateTimePicker, {
      propsData: {
        timezone: "UTC",
      },
    });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const vm = picker.vm as any;
    vm.dateClicked();
    expect(vm.dateMenu).toBeFalsy();
    expect(vm.timeMenu).toBeTruthy();
  });
});
