// frontend/careerpilot-frontend/src/services/jobService.js
import api from "./api";

export const createJob = async (jobData) => {
  const response = await api.post("/jobs", jobData);
  return response.data;
};

export const getJobs = async () => {
  const response = await api.get("/jobs");
  return response.data;
};

export const getJob = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}`);
  return response.data;
};

export const deleteJob = async (jobId) => {
  const response = await api.delete(`/jobs/${jobId}`);
  return response.data;
};

export const runJobMatch = async (jobId, resumeId) => {
  const response = await api.post(`/jobs/${jobId}/match`, { resumeId });
  return response.data;
};

export const getLatestMatch = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}/match`);
  return response.data;
};

export const getResumes = async () => {
  const response = await api.get("/resumes");
  return response.data;
};