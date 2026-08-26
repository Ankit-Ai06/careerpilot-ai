import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import {
  createJob,
  getJobs,
  deleteJob,
  runJobMatch,
  getLatestMatch,
  getResumes,
} from "../services/jobService";

function JobMatch() {
  const { showToast } = useToast();
  const [searchParams] = useSearchParams();

  const [jobs, setJobs] = useState([]);
  const [resumes, setResumes] = useState([]);
  const [loadingLists, setLoadingLists] = useState(true);

  const [selectedJobId, setSelectedJobId] = useState(null);
  const [selectedResumeId, setSelectedResumeId] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [savingJob, setSavingJob] = useState(false);
  const [formData, setFormData] = useState({
    title: "",
    company: "",
    jobUrl: "",
    description: "",
  });

  const [matching, setMatching] = useState(false);
  const [match, setMatch] = useState(null);
  const [matchError, setMatchError] = useState("");
  const [jobPendingDeletion, setJobPendingDeletion] = useState(null);

  const selectedJob = jobs.find((job) => job.id === selectedJobId) || null;

  const loadLists = async () => {
    setLoadingLists(true);

    try {
      const [jobsData, resumesData] = await Promise.all([
        getJobs(),
        getResumes(),
      ]);

      setJobs(jobsData);
      setResumes(resumesData);

      if (jobsData.length > 0) {
        const requestedJobId = Number(searchParams.get("jobId"));
        const requestedJob = jobsData.find((job) => job.id === requestedJobId);
        setSelectedJobId(requestedJob ? requestedJob.id : jobsData[0].id);
      }

      if (resumesData.length > 0) {
        setSelectedResumeId(resumesData[0].id);
      }
    } catch (error) {
      console.error("Failed to load jobs/resumes:", error);
      showToast(
        error.response?.data?.message ||
          "Couldn't load your jobs and resumes.",
        "error"
      );
    } finally {
      setLoadingLists(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadLists();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  // Whenever the selected job changes, try to load its most recent
  // match result (if one exists) instead of always starting blank.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMatch(null);
    setMatchError("");

    if (!selectedJobId) return;

    getLatestMatch(selectedJobId)
      .then((data) => setMatch(data))
      .catch(() => {
        // No match yet for this job - that's fine, not an error state.
      });
  }, [selectedJobId]);

  const handleFormChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSaveJob = async (e) => {
    e.preventDefault();

    if (
      !formData.title.trim() ||
      !formData.company.trim() ||
      !formData.description.trim()
    ) {
      showToast("Title, company and description are required.", "warning");
      return;
    }

    setSavingJob(true);

    try {
      const saved = await createJob(formData);

      showToast("Job saved successfully.", "success");

      setFormData({ title: "", company: "", jobUrl: "", description: "" });
      setShowForm(false);

      await loadLists();
      setSelectedJobId(saved.jobId);
    } catch (error) {
      console.error("Failed to save job:", error);
      showToast(
        error.response?.data?.message || "Couldn't save this job.",
        "error"
      );
    } finally {
      setSavingJob(false);
    }
  };

  const handleDeleteJob = async (jobId) => {
    try {
      await deleteJob(jobId);
      showToast("Job removed.", "success");

      const remaining = jobs.filter((job) => job.id !== jobId);
      setJobs(remaining);

      if (selectedJobId === jobId) {
        setSelectedJobId(remaining.length > 0 ? remaining[0].id : null);
      }
      setJobPendingDeletion(null);
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't delete this job.",
        "error"
      );
    }
  };

  const handleRunMatch = async () => {
    if (!selectedJobId) {
      showToast("Select or add a job first.", "warning");
      return;
    }

    if (!selectedResumeId) {
      showToast("Select a resume to match against.", "warning");
      return;
    }

    setMatching(true);
    setMatchError("");

    try {
      const result = await runJobMatch(selectedJobId, selectedResumeId);
      setMatch(result);
      showToast("Match complete.", "success");
    } catch (error) {
      console.error("Job match failed:", error);
      const message =
        error.response?.data?.message ||
        "Couldn't run the match. Please try again.";
      setMatchError(message);
      showToast(message, "error");
    } finally {
      setMatching(false);
    }
  };

  return (
    <div className="resume-page job-match-page">
      <div className="section-head">
        <div>
          <p className="eyebrow">RESUME ↔ JOB MATCH</p>
          <h1>Job Match</h1>
          <p>
            Save job descriptions and see exactly how well your resume
            matches each one - skills, keywords, and what to fix.
          </p>
        </div>
      </div>

      <div className="job-match-layout">
        <div className="job-list-panel">
          <div className="panel-header">
            <h2>Saved Jobs</h2>
            <button
              type="button"
              className="link-btn"
              onClick={() => setShowForm((prev) => !prev)}
            >
              {showForm ? "Cancel" : "+ Add job"}
            </button>
          </div>

          {showForm && (
            <form className="job-form" onSubmit={handleSaveJob}>
              <div className="form-group">
                <label>Job title</label>
                <input
                  name="title"
                  value={formData.title}
                  onChange={handleFormChange}
                  placeholder="e.g. Backend Engineer"
                />
              </div>

              <div className="form-group">
                <label>Company</label>
                <input
                  name="company"
                  value={formData.company}
                  onChange={handleFormChange}
                  placeholder="e.g. Acme Corp"
                />
              </div>

              <div className="form-group">
                <label>Job URL (optional)</label>
                <input
                  name="jobUrl"
                  value={formData.jobUrl}
                  onChange={handleFormChange}
                  placeholder="https://..."
                />
              </div>

              <div className="form-group">
                <label>Job description</label>
                <textarea
                  name="description"
                  rows={7}
                  value={formData.description}
                  onChange={handleFormChange}
                  placeholder="Paste the full job description here..."
                />
              </div>

              <button
                type="submit"
                className="primary-btn full-width"
                disabled={savingJob}
              >
                {savingJob ? "Saving..." : "Save Job"}
              </button>
            </form>
          )}

          {!loadingLists && jobs.length === 0 && !showForm && (
            <div className="empty-state">
              <p>No jobs saved yet.</p>
              <span>Add a job description to get started.</span>
            </div>
          )}

          <div className="job-list">
            {jobs.map((job) => (
              <div
                key={job.id}
                className={`job-list-item ${
                  selectedJobId === job.id ? "active" : ""
                }`}
                onClick={() => setSelectedJobId(job.id)}
              >
                <div>
                  <strong>{job.title}</strong>
                  <span>{job.company}</span>
                </div>

                <button
                  type="button"
                  className="job-delete-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    setJobPendingDeletion(job);
                  }}
                  aria-label={`Delete ${job.title} at ${job.company}`}
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="job-detail-panel">
          {!selectedJob && !loadingLists && (
            <div className="empty-state large">
              <p>Select a job on the left, or add a new one.</p>
            </div>
          )}

          {selectedJob && (
            <>
              <div className="job-detail-head">
                <div>
                  <h2>{selectedJob.title}</h2>
                  <p>{selectedJob.company}</p>
                  {selectedJob.jobUrl && (
                    <a
                      href={selectedJob.jobUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="job-url-link"
                    >
                      View original posting ↗
                    </a>
                  )}
                </div>
              </div>

              <div className="match-controls">
                <div className="form-group">
                  <label>Match against resume</label>
                  <select
                    value={selectedResumeId}
                    onChange={(e) => setSelectedResumeId(e.target.value)}
                    disabled={resumes.length === 0}
                  >
                    {resumes.length === 0 && (
                      <option value="">No resumes uploaded yet</option>
                    )}
                    {resumes.map((resume) => (
                      <option key={resume.id} value={resume.id}>
                        {resume.fileName}
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  type="button"
                  className="primary-btn"
                  onClick={handleRunMatch}
                  disabled={matching || resumes.length === 0}
                >
                  {matching
                    ? "Running AI Match..."
                    : match
                    ? "Re-run Match"
                    : "Run AI Match"}
                </button>
              </div>

              {resumes.length === 0 && (
                <p className="field-error">
                  Upload a resume on the Resume Analyzer page first.
                </p>
              )}

              {matchError && !match && (
                <p className="field-error">{matchError}</p>
              )}

              {match && <MatchResults match={match} />}
            </>
          )}
        </div>
      </div>

      {jobPendingDeletion && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => setJobPendingDeletion(null)}
        >
          <div
            className="confirmation-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-job-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <p className="eyebrow">REMOVE SAVED JOB</p>
            <h2 id="delete-job-title">Delete this job?</h2>
            <p>
              This removes {jobPendingDeletion.title} at {jobPendingDeletion.company}
              and its saved match results. This cannot be undone.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-btn"
                onClick={() => setJobPendingDeletion(null)}
              >
                Keep job
              </button>
              <button
                type="button"
                className="danger-btn"
                onClick={() => handleDeleteJob(jobPendingDeletion.id)}
              >
                Delete job
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function MatchResults({ match }) {
  const analysis = match.analysis || {};

  const scoreRows = [
    { label: "Skill Match", value: analysis.skillMatchScore },
    { label: "Keyword Match", value: analysis.keywordMatchScore },
    { label: "Experience Match", value: analysis.experienceMatchScore },
  ].filter((row) => typeof row.value === "number");

  return (
    <div className="resume-score-panel job-match-results">
      <div className="score-header">
        <div
          className="score-circle"
          style={{ "--score": `${match.overallScore}%` }}
        >
          <div className="score-circle-inner">
            <strong>{match.overallScore}%</strong>
            <span>Match</span>
          </div>
        </div>

        <div className="score-summary">
          <p className="eyebrow">AI ASSESSMENT</p>
          <h2>{scoreLabel(match.overallScore)}</h2>
          <p>{analysis.summary}</p>
        </div>
      </div>

      {scoreRows.length > 0 && (
        <div className="score-breakdown">
          <h3>Breakdown</h3>
          {scoreRows.map((row) => (
            <div className="score-row" key={row.label}>
              <span>{row.label}</span>
              <strong>{row.value}%</strong>
            </div>
          ))}
        </div>
      )}

      <div className="skills-grid">
        <div className="skills-column">
          <h3>Matched Skills</h3>
          <div className="chip-list">
            {(analysis.matchedSkills || []).length === 0 && (
              <span className="chip chip-muted">None detected</span>
            )}
            {(analysis.matchedSkills || []).map((skill) => (
              <span className="chip chip-success" key={skill}>
                ✓ {skill}
              </span>
            ))}
          </div>
        </div>

        <div className="skills-column">
          <h3>Missing Skills</h3>
          <div className="chip-list">
            {(analysis.missingSkills || []).length === 0 && (
              <span className="chip chip-muted">None - great coverage!</span>
            )}
            {(analysis.missingSkills || []).map((skill) => (
              <span className="chip chip-danger" key={skill}>
                {skill}
              </span>
            ))}
          </div>
        </div>
      </div>

      {(analysis.recommendations || []).length > 0 && (
        <div className="score-breakdown recommendations-block">
          <h3>Recommendations</h3>
          <ul className="recommendation-list">
            {analysis.recommendations.map((rec, index) => (
              <li key={index}>{rec}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function scoreLabel(score) {
  if (score >= 85) return "Excellent Fit";
  if (score >= 70) return "Strong Fit";
  if (score >= 50) return "Moderate Fit";
  return "Needs Work";
}

export default JobMatch;
