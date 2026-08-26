import api from "./api";

export const getRoadmaps = async () => (await api.get("/roadmaps")).data;

export const createRoadmap = async (matchId) =>
  (await api.post("/roadmaps", { matchId })).data;

export const updateRoadmapItem = async (itemId, progress) =>
  (await api.patch(`/roadmaps/items/${itemId}`, { progress })).data;

export const deleteRoadmap = async (roadmapId) =>
  (await api.delete(`/roadmaps/${roadmapId}`)).data;
