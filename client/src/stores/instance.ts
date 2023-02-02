import { defineStore } from "pinia";
import { ApiInstance } from "@/generated/types";

export const useInstanceStore = defineStore("instance", {
  state: () => ({
    instance: null as ApiInstance | null,
    seasonIndex: 0,
  }),
  getters: {
    seasons: (state) => state.instance?.seasons || [],
    season: (state) => {
      if (state.seasonIndex < (state.instance?.seasons?.length || 0)) {
        return state.instance?.seasons[state.seasonIndex];
      }
      return null;
    },
  },
  actions: {
    setInstance(inst: ApiInstance) {
      this.instance = inst;
      if (inst.seasons.length > 0) {
        this.seasonIndex = inst.seasons.length - 1;
      } else {
        this.seasonIndex = 0;
      }
    },
    setCurrentSeason(index: number) {
      if ((this.instance?.seasons?.length || 0) > index) {
        this.seasonIndex = index;
      } else {
        this.seasonIndex = 0;
      }
    },
  },
});
