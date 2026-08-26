import api from "./api";

export const getDashboard = async () => (await api.get("/dashboard")).data;
