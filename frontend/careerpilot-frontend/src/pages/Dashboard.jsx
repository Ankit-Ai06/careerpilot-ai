import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import { getDashboard } from "../services/dashboardService";

function Dashboard() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const result = await getDashboard();
      setData(result);
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't load your dashboard.",
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  if (loading) {
    return (
      <main className="dashboard">
        <div className="dashboard-loading">
          <div className="spinner" />
          <p>Loading your career snapshot...</p>
        </div>
      </main>
    );
  }

  if (!data) return null;

  const stats = data.stats || {};
  const resumeScore = stats.resumeScore;

  return (
    <main className="dashboard">
      <section className="dashboard-hero">
        <div>
          <p className="eyebrow">YOUR CAREER COMMAND CENTER</p>
          <h1>Welcome, {data.name?.split(" ")[0] || "there"}.</h1>
          <p>
            Analyze. Improve. Prepare. Get one step closer to your dream career.
          </p>
        </div>

        <div className="dashboard-hero-actions">
          <button className="secondary-btn dashboard-action" onClick={() => navigate("/resume")}>
            Upload resume
          </button>
          <button className="primary-btn dashboard-action" onClick={() => navigate("/job-match")}>
            Analyze a job ↗
          </button>
        </div>
      </section>

      <section className="stats-grid dashboard-stats-grid">
        <DashboardStat
          label="Latest Resume Score"
          value={resumeScore == null ? "—" : `${resumeScore}%`}
          detail={resumeScore == null ? "Upload and analyze a resume" : "Based on your latest resume"}
          tone="purple"
          to="/resume"
        />
        <DashboardStat
          label="Average Job Match"
          value={stats.averageJobMatch ? `${stats.averageJobMatch}%` : "—"}
          detail={stats.matches ? `${stats.matches} matched job${stats.matches === 1 ? "" : "s"}` : "No job matches yet"}
          tone="blue"
          to="/job-match"
        />
        <DashboardStat
          label="Skill Gaps"
          value={stats.skillGaps ?? 0}
          detail="Distinct missing skills from latest matches"
          tone="orange"
          to="/roadmap"
        />
        <DashboardStat
          label="Saved Jobs"
          value={stats.savedJobs ?? 0}
          detail="Job descriptions in your workspace"
          tone="green"
          to="/job-match"
        />
      </section>

      <section className="dashboard-main-grid">
        <div className="panel dashboard-panel dashboard-gap-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">REAL-TIME INSIGHT</p>
              <h2>Top skill gaps</h2>
            </div>
            <Link to="/roadmap">Open roadmap ↗</Link>
          </div>

          {data.topSkillGaps?.length ? (
            <div className="dashboard-gap-list">
              {data.topSkillGaps.map((gap) => (
                <div className="dashboard-gap-row" key={gap.skill}>
                  <div>
                    <strong>{gap.skill}</strong>
                    <span>Missing in {gap.jobCount} {gap.jobCount === 1 ? "job" : "jobs"}</span>
                  </div>
                  <div className="gap-bar">
                    <div style={{ width: `${Math.min(100, gap.jobCount * 25)}%` }} />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyPanel
              title="No skill gaps yet"
              text="Run an AI job match to discover what to improve."
              action="Match a job"
              to="/job-match"
            />
          )}
        </div>

        <div className="panel dashboard-panel roadmap-health-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">LEARNING PROGRESS</p>
              <h2>Roadmap health</h2>
            </div>
            <Link to="/roadmap">View roadmaps ↗</Link>
          </div>

          <div className="roadmap-health-ring" style={{ "--progress": `${stats.roadmapProgress || 0}%` }}>
            <div>
              <strong>{stats.roadmapProgress || 0}%</strong>
              <span>complete</span>
            </div>
          </div>

          <div className="roadmap-health-copy">
            <strong>{stats.roadmaps || 0} active roadmap{stats.roadmaps === 1 ? "" : "s"}</strong>
            <span>{stats.roadmaps ? "Keep completing milestones to turn skill gaps into job-ready skills." : "Create a roadmap from a completed job match."}</span>
          </div>
        </div>
      </section>

      <section className="dashboard-bottom-grid">
        <div className="panel dashboard-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">RECENT JOB ACTIVITY</p>
              <h2>Latest saved jobs</h2>
            </div>
            <Link to="/job-match">View all ↗</Link>
          </div>

          {data.recentJobs?.length ? (
            <div className="recent-list">
              {data.recentJobs.map((job) => (
                <button
                  type="button"
                  className="recent-list-item"
                  key={job.id}
                  onClick={() => navigate(`/job-match?jobId=${job.id}`)}
                >
                  <div className="recent-list-icon">↗</div>
                  <div className="recent-list-content">
                    <strong>{job.title}</strong>
                    <span>{job.company}</span>
                  </div>
                  <div className="recent-list-score">
                    {job.matchScore == null ? "Not matched" : `${job.matchScore}% match`}
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <EmptyPanel
              title="No saved jobs"
              text="Add a job description to start matching your resume."
              action="Add a job"
              to="/job-match"
            />
          )}
        </div>

        <div className="panel dashboard-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">YOUR DOCUMENTS</p>
              <h2>Recent resumes</h2>
            </div>
            <Link to="/resume">Open analyzer ↗</Link>
          </div>

          {data.recentResumes?.length ? (
            <div className="recent-list">
              {data.recentResumes.map((resume) => (
                <button
                  type="button"
                  className="recent-list-item"
                  key={resume.id}
                  onClick={() => navigate("/resume")}
                >
                  <div className="recent-list-icon file-type-icon">{resume.fileType}</div>
                  <div className="recent-list-content">
                    <strong>{resume.fileName}</strong>
                    <span>{new Date(resume.uploadedAt).toLocaleDateString()}</span>
                  </div>
                  <div className="recent-list-score">Open</div>
                </button>
              ))}
            </div>
          ) : (
            <EmptyPanel
              title="No resumes uploaded"
              text="Upload your first resume to unlock scoring and matching."
              action="Upload resume"
              to="/resume"
            />
          )}
        </div>
      </section>

      <section className="dashboard-quick-actions">
        <button type="button" onClick={() => navigate("/resume")}>
          <span>01</span>
          <strong>Improve your resume</strong>
          <small>Upload, preview, score, and analyze.</small>
        </button>
        <button type="button" onClick={() => navigate("/job-match")}>
          <span>02</span>
          <strong>Match a target role</strong>
          <small>Compare your resume with a real job description.</small>
        </button>
        <button type="button" onClick={() => navigate("/roadmap")}>
          <span>03</span>
          <strong>Close your skill gaps</strong>
          <small>Turn missing skills into a practical roadmap.</small>
        </button>
        <button type="button" onClick={() => navigate("/interview")}>
          <span>04</span>
          <strong>Practice interviews</strong>
          <small>Continue your preparation in the AI interview workspace.</small>
        </button>
      </section>
    </main>
  );
}

function DashboardStat({ label, value, detail, tone, to }) {
  return (
    <Link className={`stat-card stat-card-${tone}`} to={to}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
      <em>Open ↗</em>
    </Link>
  );
}

function EmptyPanel({ title, text, action, to }) {
  return (
    <div className="dashboard-empty">
      <div className="empty-state-icon">✦</div>
      <strong>{title}</strong>
      <span>{text}</span>
      <Link to={to}>{action}</Link>
    </div>
  );
}

export default Dashboard;
