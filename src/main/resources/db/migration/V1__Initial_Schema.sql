-- V1: Initial Schema for ASMS V3
-- Supports H2 (PostgreSQL Mode) and PostgreSQL

CREATE TABLE IF NOT EXISTS subject (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    abbreviation VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    building VARCHAR(255) NOT NULL,
    equipment TEXT,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS teacher (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    specialization VARCHAR(255),
    position_type VARCHAR(50) NOT NULL,
    weekly_hour_limit INTEGER DEFAULT 40,
    max_working_days_per_week INTEGER,
    assigned_room_id BIGINT REFERENCES room(id),
    archived_at TIMESTAMP,
    archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS student_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size INTEGER NOT NULL,
    course INTEGER NOT NULL,
    department VARCHAR(255) NOT NULL,
    curator_id BIGINT
);

CREATE TABLE IF NOT EXISTS course_plan (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES subject(id),
    teacher_id BIGINT REFERENCES teacher(id),
    second_teacher_id BIGINT REFERENCES teacher(id),
    group_id BIGINT NOT NULL REFERENCES student_group(id),
    total_hours INTEGER NOT NULL,
    lecture_hours INTEGER NOT NULL,
    practice_hours INTEGER NOT NULL,
    lab_hours INTEGER NOT NULL,
    lecture_sessions_per_week INTEGER NOT NULL,
    practice_sessions_per_week INTEGER NOT NULL,
    lab_sessions_per_week INTEGER NOT NULL,
    lecture_periodicity VARCHAR(50) NOT NULL DEFAULT 'WEEKLY',
    practice_periodicity VARCHAR(50) NOT NULL DEFAULT 'WEEKLY',
    lab_periodicity VARCHAR(50) NOT NULL DEFAULT 'WEEKLY',
    executed_hours INTEGER NOT NULL DEFAULT 0,
    required_room_type VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS timeslot (
    id BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    week_parity VARCHAR(50) NOT NULL DEFAULT 'WEEKLY',
    lesson_number INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS schedule_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS schedule_profile (
    id BIGSERIAL PRIMARY KEY,
    academic_year VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    default_template BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    kind VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    copied_from_id BIGINT REFERENCES schedule_profile(id)
);

CREATE TABLE IF NOT EXISTS lesson (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES subject(id),
    lesson_type VARCHAR(50) NOT NULL,
    teacher_id BIGINT NOT NULL REFERENCES teacher(id),
    group_id BIGINT NOT NULL REFERENCES student_group(id),
    course_plan_id BIGINT NOT NULL REFERENCES course_plan(id),
    timeslot_id BIGINT REFERENCES timeslot(id),
    room_id BIGINT REFERENCES room(id),
    periodicity VARCHAR(50) NOT NULL DEFAULT 'WEEKLY',
    subgroup INTEGER DEFAULT 0,
    split_group_index INTEGER DEFAULT 0,
    schedule_profile_id BIGINT REFERENCES schedule_profile(id),
    template_id BIGINT REFERENCES schedule_template(id)
);

CREATE TABLE IF NOT EXISTS teacher_competence_matrix (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teacher(id),
    subject_id BIGINT NOT NULL REFERENCES subject(id),
    lesson_type VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_schedule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    full_template BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    snapshot_json TEXT
);

CREATE TABLE IF NOT EXISTS saved_schedule_lesson (
    id BIGSERIAL PRIMARY KEY,
    saved_schedule_id BIGINT NOT NULL REFERENCES saved_schedule(id),
    lesson_id BIGINT NOT NULL,
    course_plan_id BIGINT,
    group_id BIGINT,
    subject_id BIGINT,
    teacher_id BIGINT,
    timeslot_id BIGINT,
    room_id BIGINT,
    lesson_type VARCHAR(50) NOT NULL,
    periodicity VARCHAR(50) NOT NULL,
    subgroup INTEGER,
    split_group_index INTEGER
);

ALTER TABLE saved_schedule ADD COLUMN IF NOT EXISTS full_template BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saved_schedule ADD COLUMN IF NOT EXISTS snapshot_json TEXT;
ALTER TABLE saved_schedule ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;
