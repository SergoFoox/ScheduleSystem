# Teacher Archiving Data Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add archiving capability to the Teacher entity by introducing a boolean `archived` field in the database and the Java model.

**Architecture:** 
- Database migration using a new SQL script.
- Update JPA entity `Teacher` with the new field and accessors.
- Update application configuration to include the new schema migration.

**Tech Stack:** Java 21, Spring Boot, JPA, SQL (PostgreSQL compatible)

---

### Task 1: Create SQL Migration

**Files:**
- Create: `src/main/resources/db/migration/V9__Add_Archived_To_Teacher.sql`

- [ ] **Step 1: Create the SQL migration file**

```sql
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
```

### Task 2: Update Teacher Entity

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`

- [ ] **Step 1: Add the archived field and accessors**

```java
    // ... after existing fields
    private boolean archived = false;

    // ... getters and setters
    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
```

### Task 3: Update Application Properties

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Add the new migration to spring.sql.init.schema-locations**

Add `,classpath:db/migration/V9__Add_Archived_To_Teacher.sql` to the existing list.

### Task 4: Verification

- [ ] **Step 1: Compile the project**

Run: `./mvnw compile`
Expected: BUILD SUCCESS
