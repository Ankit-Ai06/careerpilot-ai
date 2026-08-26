import { useEffect, useMemo, useState } from "react";
import api from "../services/api";
import { useToast } from "../context/ToastContext";

function Interview() {
  const { showToast } = useToast();

  const [resumes, setResumes] = useState([]);
  const [loadingResumes, setLoadingResumes] = useState(true);

  const [resumeId, setResumeId] = useState("");
  const [jobRole, setJobRole] = useState("Java Developer");
  const [interviewType, setInterviewType] = useState("Technical");
  const [difficulty, setDifficulty] = useState("Medium");
  const [numberOfQuestions, setNumberOfQuestions] = useState(5);

  const [interview, setInterview] = useState(null);
  const [questions, setQuestions] = useState([]);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answer, setAnswer] = useState("");

  const [evaluations, setEvaluations] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [starting, setStarting] = useState(false);
  const [completing, setCompleting] = useState(false);

  const [result, setResult] = useState(null);

  useEffect(() => {
    loadResumes();
  }, []);

  const loadResumes = async () => {
    try {
      const response = await api.get("/resumes");
      setResumes(response.data || []);
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Unable to load your resumes.",
        "error"
      );
    } finally {
      setLoadingResumes(false);
    }
  };

  const currentQuestion = questions[currentIndex];

  const currentEvaluation = currentQuestion
    ? evaluations[currentQuestion.id]
    : null;

  const progress = questions.length
    ? ((currentIndex + 1) / questions.length) * 100
    : 0;

  const startInterview = async (event) => {
    event.preventDefault();

    if (!resumeId) {
      showToast("Please select a resume.", "warning");
      return;
    }

    setStarting(true);

    try {
      const response = await api.post("/interviews/start", null, {
        params: {
          resumeId,
          jobRole,
          interviewType,
          difficulty,
          numberOfQuestions,
        },
      });

      setInterview(response.data);
      setQuestions(response.data.questions || []);
      setCurrentIndex(0);
      setAnswer("");
      setEvaluations({});
      setResult(null);

      showToast("Interview started successfully!", "success");
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Unable to start interview.",
        "error"
      );
    } finally {
      setStarting(false);
    }
  };

  const submitAnswer = async () => {
    if (!currentQuestion) return;

    if (!answer.trim()) {
      showToast("Please write an answer first.", "warning");
      return;
    }

    setSubmitting(true);

    try {
      const response = await api.post(
        `/interviews/${interview.interviewId}/questions/${currentQuestion.id}/answer`,
        {
          answer: answer.trim(),
        }
      );

      const evaluation = response.data;

      setEvaluations((previous) => ({
        ...previous,
        [currentQuestion.id]: evaluation,
      }));

      setQuestions((previous) =>
        previous.map((question) =>
          question.id === currentQuestion.id
            ? {
                ...question,
                userAnswer: answer.trim(),
                score: evaluation.score,
                aiFeedback: evaluation.aiFeedback,
                strengths: evaluation.strengths,
                improvements: evaluation.improvements,
                modelAnswer: evaluation.modelAnswer,
              }
            : question
        )
      );

      showToast("Answer evaluated successfully.", "success");
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Unable to evaluate your answer.",
        "error"
      );
    } finally {
      setSubmitting(false);
    }
  };

  const goToNextQuestion = () => {
    if (currentIndex < questions.length - 1) {
      const nextIndex = currentIndex + 1;
      setCurrentIndex(nextIndex);

      const nextQuestion = questions[nextIndex];

      setAnswer(
        nextQuestion.userAnswer || ""
      );

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    }
  };

  const completeInterview = async () => {
    if (!interview) return;

    setCompleting(true);

    try {
      const response = await api.post(
        `/interviews/${interview.interviewId}/complete`
      );

      setResult(response.data);

      showToast(
        "Interview completed successfully!",
        "success"
      );
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Unable to complete interview.",
        "error"
      );
    } finally {
      setCompleting(false);
    }
  };

  const resetInterview = () => {
    setInterview(null);
    setQuestions([]);
    setCurrentIndex(0);
    setAnswer("");
    setEvaluations({});
    setResult(null);
  };

  const parseList = (value) => {
    if (!value) return [];

    if (Array.isArray(value)) {
      return value;
    }

    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed : [String(value)];
    } catch {
      return [String(value)];
    }
  };

  const scoreLabel = useMemo(() => {
    if (!currentEvaluation?.score && currentEvaluation?.score !== 0) {
      return "";
    }

    const score = currentEvaluation.score;

    if (score >= 80) return "Excellent";
    if (score >= 60) return "Good";
    if (score >= 40) return "Needs Improvement";

    return "Needs More Practice";
  }, [currentEvaluation]);

  if (result) {
    return (
      <main className="interview-page">
        <section className="interview-result-card">
          <div className="result-icon">✓</div>

          <p className="eyebrow">INTERVIEW COMPLETE</p>

          <h1>Great work! Interview completed.</h1>

          <div className="final-score">
            <strong>{result.overallScore}</strong>
            <span>/ 100</span>
          </div>

          <h3>Overall Feedback</h3>

          <p className="result-feedback">
            {result.overallFeedback}
          </p>

          <button
            className="primary-btn"
            onClick={resetInterview}
          >
            Start New Interview
          </button>
        </section>
      </main>
    );
  }

  if (!interview) {
    return (
      <main className="interview-page">
        <section className="interview-setup-card">
          <div className="setup-header">
            <div>
              <p className="eyebrow">AI INTERVIEW COACH</p>
              <h1>Practice smarter. Interview better.</h1>
              <p>
                Select your resume and configure a personalized
                AI-powered interview.
              </p>
            </div>
          </div>

          <form
            className="interview-form"
            onSubmit={startInterview}
          >
            <div className="form-group full-width">
              <label>Select Resume</label>

              <select
                value={resumeId}
                onChange={(event) =>
                  setResumeId(event.target.value)
                }
                disabled={loadingResumes}
              >
                <option value="">
                  {loadingResumes
                    ? "Loading resumes..."
                    : "Choose a resume"}
                </option>

                {resumes.map((resume) => (
                  <option
                    key={resume.id}
                    value={resume.id}
                  >
                    {resume.fileName}
                  </option>
                ))}
              </select>

              {!loadingResumes && resumes.length === 0 && (
                <small className="form-help">
                  Upload a resume first from Resume Analyzer.
                </small>
              )}
            </div>

            <div className="form-group">
              <label>Job Role</label>

              <select
                value={jobRole}
                onChange={(event) =>
                  setJobRole(event.target.value)
                }
              >
                <option>Java Developer</option>
                <option>Software Developer</option>
                <option>Frontend Developer</option>
                <option>Backend Developer</option>
                <option>Full Stack Developer</option>
                <option>Python Developer</option>
                <option>Data Analyst</option>
              </select>
            </div>

            <div className="form-group">
              <label>Interview Type</label>

              <select
                value={interviewType}
                onChange={(event) =>
                  setInterviewType(event.target.value)
                }
              >
                <option>Technical</option>
                <option>HR</option>
                <option>Behavioral</option>
                <option>Mixed</option>
              </select>
            </div>

            <div className="form-group">
              <label>Difficulty</label>

              <select
                value={difficulty}
                onChange={(event) =>
                  setDifficulty(event.target.value)
                }
              >
                <option>Easy</option>
                <option>Medium</option>
                <option>Hard</option>
              </select>
            </div>

            <div className="form-group">
              <label>Questions</label>

              <select
                value={numberOfQuestions}
                onChange={(event) =>
                  setNumberOfQuestions(
                    Number(event.target.value)
                  )
                }
              >
                <option value={3}>3 Questions</option>
                <option value={5}>5 Questions</option>
                <option value={10}>10 Questions</option>
                <option value={15}>15 Questions</option>
                <option value={20}>20 Questions</option>
              </select>
            </div>

            <div className="form-actions full-width">
              <button
                className="primary-btn"
                type="submit"
                disabled={starting || !resumeId}
              >
                {starting
                  ? "Creating Interview..."
                  : "Start AI Interview →"}
              </button>
            </div>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="interview-page">
      <div className="interview-topbar">
        <div>
          <p className="eyebrow">AI INTERVIEW COACH</p>

          <h1>{interview.jobRole}</h1>

          <p className="interview-meta">
            {interview.interviewType} Interview
            <span>•</span>
            {interview.difficulty}
          </p>
        </div>

        <button
          className="secondary-btn"
          onClick={resetInterview}
        >
          + New Interview
        </button>
      </div>

      <div className="interview-progress-card">
        <div className="progress-info">
          <span>
            Question {currentIndex + 1} of{" "}
            {questions.length}
          </span>

          <strong>
            {Math.round(progress)}%
          </strong>
        </div>

        <div className="progress-track">
          <div
            className="progress-fill"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="interview-layout">
        <section className="interview-main">
          <div className="question-card">
            <span className="question-badge">
              QUESTION {currentQuestion?.questionNumber}
            </span>

            <h2>{currentQuestion?.question}</h2>
          </div>

          <div className="answer-card">
            <div className="card-heading">
              <div>
                <p className="eyebrow">YOUR ANSWER</p>
                <h3>
                  {currentEvaluation
                    ? "Submitted Answer"
                    : "Write your answer"}
                </h3>
              </div>

              {currentEvaluation && (
                <span className="submitted-badge">
                  ✓ Evaluated
                </span>
              )}
            </div>

            <textarea
              className="interview-answer"
              value={answer}
              onChange={(event) =>
                setAnswer(event.target.value)
              }
              disabled={!!currentEvaluation || submitting}
              placeholder="Explain your answer clearly. Include examples, logic, time complexity, or technical details where relevant..."
            />

            {!currentEvaluation && (
              <button
                className="primary-btn submit-answer-btn"
                onClick={submitAnswer}
                disabled={submitting}
              >
                {submitting
                  ? "AI is evaluating..."
                  : "Submit Answer →"}
              </button>
            )}
          </div>

          {currentEvaluation && (
            <div className="question-navigation">
              {currentIndex < questions.length - 1 ? (
                <button
                  className="primary-btn"
                  onClick={goToNextQuestion}
                >
                  Next Question →
                </button>
              ) : (
                <button
                  className="primary-btn"
                  onClick={completeInterview}
                  disabled={completing}
                >
                  {completing
                    ? "Calculating Result..."
                    : "Complete Interview ✓"}
                </button>
              )}
            </div>
          )}
        </section>

        <aside className="evaluation-panel">
          {!currentEvaluation ? (
            <div className="evaluation-empty">
              <div className="empty-icon">✦</div>

              <h3>AI Evaluation</h3>

              <p>
                Submit your answer and your AI interviewer
                will evaluate your technical accuracy,
                completeness and clarity.
              </p>
            </div>
          ) : (
            <>
              <div className="evaluation-header">
                <p className="eyebrow">AI EVALUATION</p>

                <div className="score-display">
                  <div className="score-number">
                    {currentEvaluation.score}
                    <span>/100</span>
                  </div>

                  <div>
                    <strong>{scoreLabel}</strong>

                    <p>
                      Your answer has been evaluated by
                      your AI interview coach.
                    </p>
                  </div>
                </div>
              </div>

              <div className="evaluation-card feedback-card">
                <h3>💬 AI Feedback</h3>

                <p>
                  {currentEvaluation.aiFeedback}
                </p>
              </div>

              <div className="evaluation-card strengths-card">
                <h3>✓ Your Strengths</h3>

                <ul>
                  {parseList(
                    currentEvaluation.strengths
                  ).map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>

              <div className="evaluation-card improvements-card">
                <h3>↗ How to Improve</h3>

                <ul>
                  {parseList(
                    currentEvaluation.improvements
                  ).map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>

              <div className="evaluation-card model-answer-card">
                <div className="model-answer-heading">
                  <span>★</span>

                  <div>
                    <p className="eyebrow">
                      IDEAL RESPONSE
                    </p>

                    <h3>Perfect / Model Answer</h3>
                  </div>
                </div>

                <p>
                  {currentEvaluation.modelAnswer ||
                    "No model answer was generated."}
                </p>
              </div>
            </>
          )}
        </aside>
      </div>
    </main>
  );
}

export default Interview;