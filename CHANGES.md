# CareerPilot AI UI & Data Improvements

## Roadmap
- Added DELETE /api/roadmaps/{roadmapId} with ownership protection.
- Added a visible `×` delete control and confirmation dialog on each roadmap.
- Added real roadmap progress summary and milestone progress styling.

## Resume Analyzer
- Added DELETE /api/resumes/{resumeId} with ownership protection.
- Resume deletion also removes the resume's stored AI analysis, match results, and related roadmap data to keep the database consistent.
- Saved resumes are clickable and open a resume preview using the extracted resume text currently stored by the application.
- Added a professional delete confirmation dialog.

## Dashboard
- Replaced all hard-coded dashboard numbers with a real `/api/dashboard` endpoint backed by PostgreSQL.
- Dashboard values now include latest resume score, average latest job-match score, distinct skill gaps, saved jobs, resume count, match count, roadmap count, and roadmap completion.
- Added real recent-job and recent-resume lists.
- Added working navigation to Resume Analyzer, Job Match, Roadmap, Interview, and job details from dashboard cards.
- Replaced the previous fake Applications counters with real Career/Workspace data because the current backend does not contain an Application entity.
- Added a modern, responsive visual design for the dashboard, roadmap cards, document list, preview modal, and empty/loading states.

## Notes
- The current resume storage model stores extracted resume text, not the original PDF/DOCX binary. The new document preview therefore displays the extracted resume content. A true PDF/DOCX viewer/download can be added later by introducing file/object storage.
- `backend/.env` is intentionally excluded from the updated ZIP. Use `backend/.env.example` to recreate local secrets.
