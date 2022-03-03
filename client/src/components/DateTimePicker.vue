<template>
  <v-row>
    <v-col>
      <v-menu
        v-model="dateMenu"
        :close-on-click="true"
        :close-on-content-click="false"
        min-width="100px"
      >
        <template #activator="{ on }">
          <v-text-field
            readonly
            :label="label"
            :value="renderDate(dateTime)"
            v-on="on"
          />
        </template>
        <v-date-picker
          v-model="date"
          class="date-picker"
          @change="onChange"
          @click:date="dateClicked"
        >
          <!--        <v-btn @click="dateClicked" color="accent">OK</v-btn>-->
        </v-date-picker>
      </v-menu>
    </v-col>
    <v-col>
      <v-menu
        v-model="timeMenu"
        :close-on-click="true"
        :close-on-content-click="false"
        min-width="100px"
      >
        <template #activator="{ on }">
          <v-text-field readonly :value="renderTime(dateTime)" v-on="on" />
        </template>
        <v-time-picker
          v-if="timeMenu"
          v-model="time"
          class="time-picker"
          :allowed-minutes="(x) => x % 5 === 0"
          @input="onChange"
          ><v-btn color="accent" @click="timeMenu = false"
            >OK</v-btn
          ></v-time-picker
        >
      </v-menu>
    </v-col>
  </v-row>
</template>
<script lang="ts">
import Vue from "vue";
import moment from "moment-timezone";

function modelTime(date: string): string {
  const zonedMoment = moment(date, moment.ISO_8601);
  const formatted = zonedMoment.format("HH:mm");
  if (formatted.indexOf("Invalid date") >= 0) {
    return "";
  } else {
    return formatted;
  }
}
function modelDate(date: string): string {
  const zonedMoment = moment(date, moment.ISO_8601);
  const formatted = zonedMoment.format("YYYY-MM-DD");
  if (formatted.indexOf("Invalid date") >= 0) {
    return "";
  } else {
    return formatted;
  }
}

export default Vue.extend({
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
        `${this.date} ${this.time}`,
        "YYYY-MM-DD HH:mm",
        browserTZ
      );
      return parsed.format();
    },
    tz(): string {
      return this.timezone != null && this.timezone != "Autodetect"
        ? this.timezone
        : moment.tz.guess();
    },
  },
  methods: {
    onChange() {
      this.$emit("input", this.dateTime);
    },
    renderTime(date: string): string {
      const zonedMoment = moment(date, moment.ISO_8601).tz(this.tz);
      const formatted = `${zonedMoment.format("h:mm A")} (${moment
        .tz(this.tz)
        .zoneName()})`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "--:-- --";
      } else {
        return formatted;
      }
    },
    renderDate(date: string): string {
      const zonedMoment = moment(date, moment.ISO_8601).tz(this.tz);
      const formatted = `${zonedMoment.format("MM/DD/YYYY")}`;
      if (formatted.indexOf("Invalid date") >= 0) {
        return "mm/dd/yyyy";
      } else {
        return formatted;
      }
    },
    dateClicked() {
      this.dateMenu = false;
      this.timeMenu = true;
    },
  },
});
</script>
