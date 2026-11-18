-- Author: Jamius Siam
-- Since: 18/11/2026

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) UNIQUE,
    photo_path    VARCHAR(255),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE teachers (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    employee_no VARCHAR(30) NOT NULL UNIQUE,
    phone       VARCHAR(30)
);

CREATE TABLE students (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    student_no     VARCHAR(30) NOT NULL UNIQUE,
    date_of_birth  DATE,
    guardian_name  VARCHAR(100),
    guardian_phone VARCHAR(30)
);

CREATE TABLE subjects (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(20)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE school_classes (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    grade_level   INT         NOT NULL CHECK (grade_level BETWEEN 1 AND 12),
    section       VARCHAR(10) NOT NULL,
    academic_year VARCHAR(9)  NOT NULL,
    teacher_id    BIGINT      REFERENCES teachers (id) ON DELETE SET NULL,
    UNIQUE (grade_level, section, academic_year)
);

CREATE TABLE enrollments (
    id          BIGSERIAL PRIMARY KEY,
    class_id    BIGINT      NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    student_id  BIGINT      NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (class_id, student_id)
);

CREATE TABLE attendance (
    id         BIGSERIAL PRIMARY KEY,
    class_id   BIGINT      NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    student_id BIGINT      NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    date       DATE        NOT NULL,
    status     VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    marked_by  BIGINT      REFERENCES users (id) ON DELETE SET NULL,
    UNIQUE (class_id, student_id, date)
);

CREATE TABLE grades (
    id         BIGSERIAL PRIMARY KEY,
    class_id   BIGINT        NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    student_id BIGINT        NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    subject_id BIGINT        NOT NULL REFERENCES subjects (id) ON DELETE CASCADE,
    term       VARCHAR(20)   NOT NULL,
    score      NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    remarks    VARCHAR(255),
    graded_by  BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    UNIQUE (class_id, student_id, subject_id, term)
);

CREATE TABLE announcements (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    body       TEXT         NOT NULL,
    audience   VARCHAR(10)  NOT NULL CHECK (audience IN ('ALL', 'TEACHERS', 'STUDENTS')),
    class_id   BIGINT       REFERENCES school_classes (id) ON DELETE CASCADE,
    author_id  BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
