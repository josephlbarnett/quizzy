<template>
  <v-select
    v-if="instanceStore.instance"
    v-model="currentSeason"
    :items="instanceStore.seasons"
    label="Season"
    item-title="name"
    @update:model-value="change"
  />
</template>
<script lang="ts">
import { useInstanceStore } from "@/stores/instance";

export default {
  name: "UserInvite",
  setup() {
    const instanceStore = useInstanceStore();
    return { instanceStore };
  },
  data: () => ({}),
  computed: {
    currentSeason: {
      get: function () {
        return this.instanceStore.season;
      },
      set: function (season: string) {
        let index = this.instanceStore.seasons
          .map((s) => s.name)
          .indexOf(season);
        if (index < 0) {
          index = 0;
        }
        this.instanceStore.setCurrentSeason(index);
      },
    },
  },
  methods: {
    change(event: string) {
      this.$emit("change", event);
    },
  },
};
</script>
