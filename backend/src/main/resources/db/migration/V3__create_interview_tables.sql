CREATE TABLE IF NOT EXISTS interviews (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    resume_id BIGINT,

    job_role VARCHAR(255) NOT NULL,
    interview_type VARCHAR(100) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,

    total_questions INTEGER NOT NULL,
    overall_score INTEGER,

    overall_feedback TEXT,

    status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_interview_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_interview_resume
        FOREIGN KEY (resume_id)
        REFERENCES resumes(id)
);


CREATE TABLE IF NOT EXISTS interview_questions (
    id BIGSERIAL PRIMARY KEY,

    interview_id BIGINT NOT NULL,

    question_number INTEGER NOT NULL,
    question TEXT NOT NULL,

    user_answer TEXT,

    score INTEGER,

    ai_feedback TEXT,
    strengths TEXT,
    improvements TEXT,

    CONSTRAINT fk_interview_question_interview
        FOREIGN KEY (interview_id)
        REFERENCES interviews(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_interview_question_number
        UNIQUE (interview_id, question_number)
);