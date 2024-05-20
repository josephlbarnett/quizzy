<template>
  <v-row>
    <v-col>
      <v-menu
        v-model="dateMenu"
        :close-on-content-click="false"
        min-width="100px"
      >
        <template #activator="{ props }">
          <v-text-field
            v-model="renderedDate"
            readonly
            :label="label"
            v-bind="props"
          />
        </template>
        <v-date-picker
          v-model="date"
          class="date-picker"
          @update:model-value="onChange(true)"
        />
        <v-btn color="accent" @click="dateClicked">OK</v-btn>
      </v-menu>
    </v-col>
    <v-col>
      <v-menu
        v-model="timeMenu"
        :close-on-content-click="false"
        min-width="100px"
      >
        <template #activator="{ props }">
          <v-text-field v-model="renderedTime" readonly v-bind="props" />
        </template>
        <v-time-picker
          v-if="timeMenu"
          v-model="time"
          class="time-picker"
          ampm-in-title
          :allowed-minutes="(x) => x % 5 === 0"
          @update:model-value="onChange(false)"
        />
        <v-btn color="accent" @click="timeMenu = false">OK</v-btn>
      </v-menu>
    </v-col>
  </v-row>
</template>
<script lang="ts">
import moment from "moment-timezone";
import { VTimePicker } from "vuetify/labs/VTimePicker";

function modelTime(date: string): string {
  const zonedMoment = moment(date, moment.ISO_8601);
  const formatted = zonedMoment.format("HH:mm");
  if (formatted.indexOf("Invalid date") >= 0) {
    return "";
  } else {
    return formatted;
  }
}
function modelDate(date: string): Date | null {
  const zonedMoment = moment(date, moment.ISO_8601);
  const formatted = zonedMoment.format("YYYY-MM-DD");
  if (formatted.indexOf("Invalid date") >= 0) {
    return null;
  } else {
    return zonedMoment.toDate();
  }
}

export default {
  components: { VTimePicker },
  props: {
    value: { type: String, default: null },
    timezone: { type: String, default: null },
    label: { type: String, required: true },
  },
  data: function () {
    return {
      time: modelTime(this.value),
      date: modelDate(this.value),
      dateMenu: false,
      timeMenu: false,
    };
  },
  computed: {
    dateTime(): string {
      const browserTZ =
        this.timezone != null && this.timezone != "Autodetect"
          ? this.timezone
          : moment.tz.guess();
      const parsed = moment.tz(
        `${this.date?.toISOString().split("T")[0]} ${this.time}`,
        "YYYY-MM-DD HH:mm",
        browserTZ,
      );
      return parsed.format();
    },
    renderedTime(): string {
      const zonedMoment = moment(this.dateTime, moment.ISO_8601).tz(this.tz);
      const formatted = `${zonedMoment.format("h:mm A")} (${moment
        .tz(this.tz)
        .zoneName()})`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "--:-- --";
      } else {
        return formatted;
      }
    },
    renderedDate(): string {
      const zonedMoment = moment(this.dateTime, moment.ISO_8601).tz(this.tz);
      const formatted = `${zonedMoment.format("MM/DD/YYYY")}`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "mm/dd/yyyy";
      } else {
        return formatted;
      }
    },
    tz(): string {
      return this.timezone != null && this.timezone != "Autodetect"
        ? this.timezone
        : moment.tz.guess();
    },
  },
  methods: {
    onChange(date: boolean) {
      this.$emit("update:modelValue", this.dateTime);
      if (date) {
        this.dateClicked();
      }
    },
    dateClicked() {
      this.dateMenu = false;
      this.timeMenu = true;
    },
  },
};
</script>
