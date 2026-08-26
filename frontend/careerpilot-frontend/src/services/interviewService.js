import api from "./api";

const interviewService = {

  startInterview: async ({
    resumeId,
    jobRole,
    interviewType,
    difficulty,
    numberOfQuestions,
  }) => {
    const response = await api.post("/interviews/start", null, {
      params: {
        resumeId,
        jobRole,
        interviewType,
        difficulty,
        numberOfQuestions,
      },
    });

    return response.data;
  },

  getInterviewQuestions: async (interviewId) => {
    const response = await api.get(
      `/interviews/${interviewId}/questions`
    );

    return response.data;
  },

  submitAnswer: async (
    interviewId,
    questionId,
    answer
  ) => {
    const response = await api.post(
      `/interviews/${interviewId}/questions/${questionId}/answer`,
      {
        answer,
      }
    );

    return response.data;
  },

  completeInterview: async (interviewId) => {
    const response = await api.post(
      `/interviews/${interviewId}/complete`
    );

    return response.data;
  },

  getInterview: async (interviewId) => {
    const response = await api.get(
      `/interviews/${interviewId}`
    );

    return response.data;
  },

  getUserInterviews: async () => {
    const response = await api.get("/interviews");

    return response.data;
  },
};

export default interviewService;