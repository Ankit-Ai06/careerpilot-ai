import { useEffect, useRef, useState } from "react";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

function ResumeAnalyzer() {
  const fileInputRef = useRef(null);
  const { showToast } = useToast();

  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [resume, setResume] = useState(null);
  const [score, setScore] = useState(null);
  const [savedResumes, setSavedResumes] = useState([]);
  const [previewResume, setPreviewResume] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [resumePendingDeletion, setResumePendingDeletion] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const MAX_FILE_SIZE = 5 * 1024 * 1024;

  const loadSavedResumes = async () => {
    try {
      const response = await api.get("/resumes");
      setSavedResumes(response.data);
    } catch (error) {
      console.error("Couldn't load saved resumes:", error);
    }
  };

  useEffect(() => {
    loadSavedResumes();
  }, []);

  const validateFile = (file) => {
    if (!file) return "Please select a file.";

    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith(".pdf") && !fileName.endsWith(".docx")) {
      return "Only PDF and DOCX files are supported.";
    }

    if (file.size > MAX_FILE_SIZE) {
      return "Resume must be smaller than 5 MB.";
    }

    return null;
  };

  const handleFileSelect = (file) => {
    const error = validateFile(file);
    if (error) {
      showToast(error, "error");
      return;
    }

    setSelectedFile(file);
    setResume(null);
    setScore(null);
  };

  const handleInputChange = (event) => {
    const file = event.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);

    const file = event.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  };

  const removeSelectedFile = () => {
    setSelectedFile(null);
    setResume(null);
    setScore(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const uploadResume = async () => {
    if (!selectedFile) {
      showToast("Please select a resume first.", "warning");
      return;
    }

    const formData = new FormData();
    formData.append("file", selectedFile);
    setUploading(true);

    try {
      const response = await api.post("/resumes/upload", formData);
      setResume(response.data);
      setScore(null);
      await loadSavedResumes();
      showToast("Resume uploaded and parsed successfully.", "success");
    } catch (error) {
      showToast(
        error.response?.data?.message || "Resume upload failed. Please try again.",
        "error"
      );
    } finally {
      setUploading(false);
    }
  };

  const analyzeResume = async () => {
    if (!resume?.resumeId) {
      showToast("Please upload a resume first.", "warning");
      return;
    }

    setAnalyzing(true);
    setScore(null);

    try {
      await api.post(`/resumes/${resume.resumeId}/analyze`);
      const scoreResponse = await api.get(`/resumes/${resume.resumeId}/score`);
      setScore(scoreResponse.data);
      showToast("Resume analyzed successfully.", "success");
    } catch (error) {
      console.error("Resume analysis error:", error);
      showToast(
        error.response?.data?.message || "Resume analysis failed. Please try again.",
        "error"
      );
    } finally {
      setAnalyzing(false);
    }
  };

  const openResume = async (resumeId) => {
    setLoadingPreview(true);
    try {
      const response = await api.get(`/resumes/${resumeId}`);
      setPreviewResume(response.data);
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't open this resume.",
        "error"
      );
    } finally {
      setLoadingPreview(false);
    }
  };

  const deleteResume = async (resumeToDelete) => {
    setDeleting(true);
    try {
      await api.delete(`/resumes/${resumeToDelete.id}`);
      setSavedResumes((current) =>
        current.filter((item) => item.id !== resumeToDelete.id)
      );

      if (resume?.resumeId === resumeToDelete.id) {
        setResume(null);
        setScore(null);
        setSelectedFile(null);
      }

      if (previewResume?.id === resumeToDelete.id) {
        setPreviewResume(null);
      }

      showToast("Resume deleted successfully.", "success");
      setResumePendingDeletion(null);
    } catch (error) {
      showToast(
        error.response?.data?.message || "Couldn't delete this resume.",
        "error"
      );
    } finally {
      setDeleting(false);
    }
  };

  const formatCategoryName = (category) =>
    category
      .replace(/([A-Z])/g, " $1")
      .replace(/^./, (str) => str.toUpperCase());

  return (
    <main className="resume-page">
      <div className="section-head">
        <div>
          <p className="eyebrow">RESUME INTELLIGENCE</p>
          <h1>Analyze your resume</h1>
          <p>
            Upload a resume, review the extracted content, and run your AI
            analysis whenever you're ready.
          </p>
        </div>
      </div>

      <div
        className={`resume-upload-area ${isDragging ? "dragging" : ""}`}
        onDragOver={(event) => {
          event.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <div className="upload-icon">↑</div>
        <h2>Drop your resume here</h2>
        <p>or click anywhere in this area to choose a file</p>
        <span>PDF or DOCX • Maximum 5 MB</span>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.docx"
          onChange={handleInputChange}
          hidden
        />
      </div>

      {selectedFile && (
        <div className="resume-file-card">
          <div className="file-info">
            <div className="file-icon">
              {selectedFile.name.toLowerCase().endsWith(".pdf") ? "PDF" : "DOCX"}
            </div>
            <div>
              <strong>{selectedFile.name}</strong>
              <span>{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</span>
            </div>
          </div>
          <button
            type="button"
            className="remove-file"
            onClick={removeSelectedFile}
            aria-label="Remove selected file"
          >
            ×
          </button>
        </div>
      )}

      <div className="resume-actions">
        <button
          className="primary-btn"
          type="button"
          onClick={uploadResume}
          disabled={!selectedFile || uploading || analyzing}
        >
          {uploading ? "Uploading..." : "Upload Resume"}
        </button>
      </div>

      {resume && (
        <div className="resume-success-panel">
          <div>
            <span className="success-badge">✓ Parsed successfully</span>
            <h2>{resume.fileName}</h2>
            <p>Your resume is ready for AI analysis and job matching.</p>
          </div>

          <div className="resume-meta">
            <span>Type: {resume.fileType}</span>
            <span>Resume ID: {resume.resumeId}</span>
          </div>

          <button
            className="primary-btn"
            type="button"
            onClick={analyzeResume}
            disabled={analyzing}
          >
            {analyzing ? "Analyzing Resume..." : "Analyze Resume with AI"}
          </button>
        </div>
      )}

      {score && (
        <div className="resume-score-panel">
          <div className="score-header">
            <div
              className="score-circle"
              style={{ "--score": `${score.score * 3.6}deg` }}
            >
              <div className="score-circle-inner">
                <strong>{score.score}</strong>
                <span>/ 100</span>
              </div>
            </div>

            <div className="score-summary">
              <p className="eyebrow">RESUME SCORE</p>
              <h2>{score.rating}</h2>
              <p>
                Your score reflects your current resume structure, skills,
                projects, experience, keywords, and readability.
              </p>
            </div>
          </div>

          {score.breakdown && (
            <div className="score-breakdown">
              <h3>Score Breakdown</h3>
              {Object.entries(score.breakdown).map(([category, points]) => (
                <div className="score-row" key={category}>
                  <span>{formatCategoryName(category)}</span>
                  <strong>{points}</strong>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <section className="saved-resumes">
        <div className="panel-header">
          <div>
            <p className="eyebrow">YOUR DOCUMENTS</p>
            <h2>Saved resumes</h2>
          </div>
          <span>{savedResumes.length} uploaded</span>
        </div>

        {savedResumes.length === 0 ? (
          <div className="saved-resumes-empty">
            <p>No resumes yet.</p>
            <span>Your uploaded documents will appear here.</span>
          </div>
        ) : (
          <div className="saved-resume-list">
            {savedResumes.map((savedResume) => (
              <div
                className="saved-resume-item clickable"
                key={savedResume.id}
                onClick={() => openResume(savedResume.id)}
                role="button"
                tabIndex={0}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    openResume(savedResume.id);
                  }
                }}
              >
                <div className="file-info">
                  <div className="file-icon">{savedResume.fileType}</div>
                  <div>
                    <strong>{savedResume.fileName}</strong>
                    <span>
                      Uploaded {new Date(savedResume.uploadedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="saved-resume-actions">
                  <span className="success-badge">View</span>
                  <button
                    type="button"
                    className="icon-danger-btn"
                    onClick={(event) => {
                      event.stopPropagation();
                      setResumePendingDeletion(savedResume);
                    }}
                    aria-label={`Delete ${savedResume.fileName}`}
                    title="Delete resume"
                  >
                    ×
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {loadingPreview && (
        <div className="modal-backdrop">
          <div className="resume-preview-modal loading-preview">
            <div className="spinner" />
            <p>Opening your resume...</p>
          </div>
        </div>
      )}

      {previewResume && !loadingPreview && (
        <div
          className="modal-backdrop"
          onMouseDown={() => setPreviewResume(null)}
        >
          <div
            className="resume-preview-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="resume-preview-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="preview-head">
              <div>
                <p className="eyebrow">RESUME PREVIEW</p>
                <h2 id="resume-preview-title">{previewResume.fileName}</h2>
                <span>{previewResume.fileType}</span>
              </div>
              <button
                type="button"
                className="icon-close-btn"
                onClick={() => setPreviewResume(null)}
                aria-label="Close preview"
              >
                ×
              </button>
            </div>

            <div className="resume-preview-body">
              {previewResume.extractedText ? (
                <pre>{previewResume.extractedText}</pre>
              ) : (
                <div className="empty-state">
                  <p>No extracted text is available.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {resumePendingDeletion && (
        <div
          className="modal-backdrop"
          onMouseDown={() => setResumePendingDeletion(null)}
        >
          <div
            className="confirmation-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-resume-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <p className="eyebrow">REMOVE DOCUMENT</p>
            <h2 id="delete-resume-title">Delete this resume?</h2>
            <p>
              This removes the resume, its stored analysis, match results, and
              related roadmap data. This cannot be undone.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-btn"
                onClick={() => setResumePendingDeletion(null)}
              >
                Keep resume
              </button>
              <button
                type="button"
                className="danger-btn"
                onClick={() => deleteResume(resumePendingDeletion)}
                disabled={deleting}
              >
                {deleting ? "Deleting..." : "Delete resume"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

export default ResumeAnalyzer;
