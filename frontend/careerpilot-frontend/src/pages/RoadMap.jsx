import { useEffect, useState } from "react";
import { useToast } from "../context/ToastContext";
import { getJobs, getLatestMatch } from "../services/jobService";
import {
  createRoadmap,
  deleteRoadmap,
  getRoadmaps,
  updateRoadmapItem,
} from "../services/roadmapService";

function Roadmap() {
  const { showToast } = useToast();
  const [roadmaps, setRoadmaps] = useState([]);
  const [matches, setMatches] = useState([]);
  const [matchId, setMatchId] = useState("");
  const [creating, setCreating] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const load = async () => {
    try {
      const [saved, jobs] = await Promise.all([getRoadmaps(), getJobs()]);
      const found = (
        await Promise.allSettled(
          jobs.map(async (job) => ({
            job,
            match: await getLatestMatch(job.id),
          }))
        )
      )
        .filter((result) => result.status === "fulfilled")
        .map((result) => result.value);

      setRoadmaps(saved);
      setMatches(found);

      if (!matchId && found.length) {
        setMatchId(String(found[0].match.matchId));
      }
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't load roadmaps.",
        "error"
      );
    }
  };

  useEffect(() => {
    load();
  }, []);

  const selectedRoadmap = roadmaps.find(
    (roadmap) => String(roadmap.matchId) === matchId
  );

  const generate = async () => {
    if (!matchId) {
      showToast("Choose a completed job match first.", "warning");
      return;
    }

    setCreating(true);

    try {
      const roadmap = await createRoadmap(Number(matchId));
      setRoadmaps((current) =>
        current.some((existing) => existing.id === roadmap.id)
          ? current
          : [roadmap, ...current]
      );
      showToast(
        selectedRoadmap
          ? "This roadmap already exists."
          : "Learning roadmap created.",
        "success"
      );
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't create roadmap.",
        "error"
      );
    } finally {
      setCreating(false);
    }
  };

  const saveProgress = async (roadmapId, itemId, progress) => {
    try {
      const updated = await updateRoadmapItem(itemId, Number(progress));
      setRoadmaps((current) =>
        current.map((roadmap) =>
          roadmap.id === roadmapId
            ? {
                ...roadmap,
                items: roadmap.items.map((item) =>
                  item.id === itemId ? updated : item
                ),
              }
            : roadmap
        )
      );
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't save progress.",
        "error"
      );
    }
  };

  const removeRoadmap = async (roadmap) => {
    setDeletingId(roadmap.id);

    try {
      await deleteRoadmap(roadmap.id);
      setRoadmaps((current) =>
        current.filter((item) => item.id !== roadmap.id)
      );
      showToast("Roadmap deleted successfully.", "success");
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't delete this roadmap.",
        "error"
      );
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <main className="resume-page roadmap-page">
      <div className="section-head roadmap-page-header">
        <div>
          <p className="eyebrow">PERSONALIZED LEARNING</p>
          <h1>Skill Roadmap</h1>
          <p>
            Turn your actual job-match skill gaps into a practical,
            trackable learning plan.
          </p>
        </div>
        <div className="roadmap-header-badge">
          {roadmaps.length} {roadmaps.length === 1 ? "roadmap" : "roadmaps"}
        </div>
      </div>

      <section className="panel roadmap-create-panel">
        <div className="panel-header">
          <div>
            <p className="eyebrow">BUILD YOUR PLAN</p>
            <h2>Create an AI roadmap</h2>
          </div>
          <button
            className="primary-btn"
            onClick={generate}
            disabled={!matchId || creating}
          >
            {creating ? "Creating your plan..." : "Generate roadmap"}
          </button>
        </div>

        <select
          className="roadmap-select"
          value={matchId}
          onChange={(event) => setMatchId(event.target.value)}
          disabled={!matches.length}
        >
          {!matches.length && (
            <option>No completed job matches yet</option>
          )}
          {matches.map(({ job, match }) => (
            <option key={match.matchId} value={match.matchId}>
              {job.title} at {job.company} · {match.overallScore}% match
            </option>
          ))}
        </select>
      </section>

      <div className="roadmap-list">
        {roadmaps.map((roadmap) => {
          const completed = roadmap.items.filter((item) => item.completed).length;
          const progress = roadmap.items.length
            ? Math.round((completed / roadmap.items.length) * 100)
            : 0;

          return (
            <section className="panel roadmap-card" key={roadmap.id}>
              <div className="roadmap-card-top">
                <div>
                  <p className="eyebrow">ROADMAP</p>
                  <h2>{roadmap.title}</h2>
                  <span className="roadmap-created">
                    {roadmap.createdAt
                      ? new Date(roadmap.createdAt).toLocaleDateString()
                      : "Created recently"}
                  </span>
                </div>

                <button
                  type="button"
                  className="icon-danger-btn"
                  onClick={() => removeRoadmap(roadmap)}
                  disabled={deletingId === roadmap.id}
                  aria-label={`Delete ${roadmap.title}`}
                  title="Delete roadmap"
                >
                  ×
                </button>
              </div>

              <div className="roadmap-progress-summary">
                <div>
                  <strong>{progress}%</strong>
                  <span>
                    {completed} of {roadmap.items.length} milestones completed
                  </span>
                </div>
                <div className="roadmap-progress-track">
                  <div style={{ width: `${progress}%` }} />
                </div>
              </div>

              {roadmap.items.map((item) => (
                <div className="roadmap-milestone" key={item.id}>
                  <div className="roadmap-milestone-marker">
                    <span>{item.completed ? "✓" : item.position}</span>
                  </div>
                  <div className="roadmap-milestone-content">
                    <div className="roadmap-milestone-meta">
                      <span className="eyebrow">
                        STEP {item.position}
                      </span>
                      <span className={`priority-pill priority-${item.priority?.toLowerCase()}`}>
                        {item.priority} priority
                      </span>
                    </div>
                    <h3>{item.skill}</h3>
                    <p>{item.learningOutcome}</p>
                    <div className="roadmap-item-progress">
                      <div className="progress">
                        <div style={{ width: `${item.progress ?? 0}%` }} />
                      </div>
                      <strong>{item.progress ?? 0}%</strong>
                    </div>
                  </div>
                  <button
                    className={item.completed ? "secondary-btn" : "primary-btn"}
                    onClick={() =>
                      saveProgress(
                        roadmap.id,
                        item.id,
                        item.completed ? 0 : 100
                      )
                    }
                  >
                    {item.completed ? "Undo" : "Complete"}
                  </button>
                </div>
              ))}
            </section>
          );
        })}
      </div>

      {!roadmaps.length && (
        <div className="empty-state large roadmap-empty-state">
          <div className="empty-state-icon">✦</div>
          <p>No roadmaps yet</p>
          <span>Run a job match, then generate a roadmap from your real skill gaps.</span>
        </div>
      )}
    </main>
  );
}

export default Roadmap;
