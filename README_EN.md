# 📚 Electronic Diary — School Management System

> Enterprise-grade SaaS platform for automating administrative, educational, and communication workflows in schools. Built with Java 17, Spring Boot 3.5.7, and powered by an AI assistant based on GPT-4o-mini.

**Stack:** Java 17 · Spring Boot 3.5.7 · Spring Security + JWT · PostgreSQL · Flyway · Thymeleaf · Docker · OpenAI GPT-4o-mini

---

## 📋 Table of Contents

- [Features](#-features)
- [AI Assistant](#-ai-assistant)
- [Requirements](#-requirements)
- [Quick Start](#-quick-start)
- [Environment Variables](#-environment-variables)
- [Running the Database](#-running-the-database)
- [Running the Application](#-running-the-application)
- [First School Setup](#-first-school-setup)
- [Project Structure](#-project-structure)
- [Roles & Access](#-roles--access)
- [API Documentation](#-api-documentation)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Features

| Module | Description |
|---|---|
| Multi-Tenancy | Multiple schools in one database, full data isolation |
| JWT Authentication | Stateless auth — no sessions, token-based security |
| AI Assistant | Natural language management of school data via GPT-4o-mini |
| Grades | Teachers set marks, students and parents get instant notifications |
| Schedule | Weekly timetable with auto-generation via AI |
| Attendance | Per-lesson attendance tracking with status and comments |
| Polls | Role-filtered surveys for students, teachers, parents |
| News | School news published by admins and teachers |
| Complaints | Anonymous or named complaints visible to the director |
| Notifications | Real-time in-app notifications on grade changes, events |
| Canteen Menu | Daily menu per class group with image upload |
| Courses | Optional elective courses with student enrollment |
| Exams | Exam/test/quiz scheduling per discipline and group |
| Excel Import | Bulk student import via `.xlsx` files |
| Grading Settings | Per-school configuration: quarters, semesters, or annual |

---

## 🤖 AI Assistant

The AI assistant is one of the most powerful features of Electronic Diary. It allows admins and directors to **manage the entire school database through natural language conversation** — no manual form-filling required.

### The problem it solves

Setting up a school from scratch is painful. An administrator needs to:
1. Create 10–20 class groups one by one
2. Add hundreds of students with names, emails, passwords, group assignments
3. Create 15–20 teachers and link each to their subjects
4. Build a weekly schedule manually — matching teachers, subjects, classrooms, time slots
5. Repeat all of this every academic year

With a traditional UI, this takes **hours of clicking through forms**. One mistake means going back and fixing records one by one.

### How the AI assistant solves it

The assistant uses **OpenAI Function Calling (Tool Use)** — a mechanism where GPT doesn't just reply with text, but decides which database operation to run and calls it directly.

**Example conversations:**

```
Admin: "Create classes 9A, 9B, 10A, 10B with 25 students each"
AI: ✅ Group 9A: 25 students
    ✅ Group 9B: 25 students
    ✅ Group 10A: 25 students
    ✅ Group 10B: 25 students
```

```
Admin: "Add 5 math and physics teachers"
AI: ✅ Teachers (5) created: Smirnov Ivan, Petrov Alexey... Subjects: Math, Physics
```

```
Admin: "Generate a schedule for group 9A starting Monday with 6 lessons per day"
AI: ✅ Schedule for 9A created: 30 lessons (Mon–Fri from 2025-01-20)
```

```
Admin: "Which students are failing? Show me averages by class"
AI: Performance analysis:
    Average by class: 9A: 3.8, 9B: 4.1, 10A: 3.5
    By subject: Math: 4.2, History: 3.6
    Struggling students: Ivanov Ivan (3.1), Kozlov Petr (3.0)
```

### AI Tools available

| Tool | Who can use | What it does |
|---|---|---|
| `get_groups` | All | List all groups with student count |
| `get_students` | All | List students, filterable by group |
| `get_teachers` | All | List teachers with their subjects |
| `get_disciplines` | All | List all school subjects |
| `get_student_marks` | All | Get a student's grades by subject |
| `get_stats` | All | School-wide stats: counts of students, teachers, etc |
| `get_schedule` | ADMIN/DIRECTOR | Get a group's upcoming schedule |
| `add_student` | ADMIN/DIRECTOR/TEACHER | Add one student with explicit name |
| `add_students_random` | ADMIN/DIRECTOR | Bulk-create students with random Russian names |
| `add_group` | ADMIN/DIRECTOR | Create a single class group |
| `add_multiple_groups` | ADMIN/DIRECTOR | Create multiple groups at once (+ students per group) |
| `add_teacher` | ADMIN/DIRECTOR | Create one or multiple teachers, assign subjects |
| `add_discipline` | ADMIN/DIRECTOR | Add a new subject |
| `generate_schedule` | ADMIN/DIRECTOR | Auto-generate a weekly schedule for a group |
| `analyze_performance` | ADMIN/DIRECTOR | Grade analytics: averages, top students, struggling students |
| `add_course` | ADMIN/DIRECTOR | Create an elective course and assign a teacher |
| `delete_student` | ADMIN/DIRECTOR | Delete a student by name |
| `delete_all_students` | ADMIN/DIRECTOR | Delete students (with optional group/count filter) |
| `delete_all_teachers` | ADMIN/DIRECTOR | Delete all teachers (clears schedule and curatorship first) |

### How it works technically

1. User sends a message in the chat UI
2. The backend builds a request to OpenAI with the current school state (groups, subjects, student/teacher counts) injected into the system prompt
3. GPT decides which tool(s) to call and returns `tool_calls` with function names and arguments
4. The backend executes the corresponding Java method directly in the database
5. The result is returned to the user as a formatted message

The assistant maintains **conversation history per user** (last 20 messages) so it understands follow-up requests in context.

---

## 🛠 Requirements

| Tool | Version | Purpose |
|---|---|---|
| **JDK** | 17+ | Compile and run the application |
| **Maven** | 3.6+ (or use Wrapper) | Build tool |
| **Docker** | 20.10+ | Run PostgreSQL in a container |
| **Docker Compose** | 2.0+ | Container orchestration |
| **OpenAI API Key** | — | Power the AI assistant (optional) |

---

## 🚀 Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/boyutov/Electronic-diary.git
cd Electronic-diary

# 2. Create environment file
cp env.example .env
# Edit .env and fill in DB_PASSWORD, JWT_SECRET, and optionally OPENAI_API_KEY

# 3. Start PostgreSQL
docker compose up -d

# 4. Run the application
./mvnw spring-boot:run       # Linux / macOS
mvnw.cmd spring-boot:run     # Windows

# 5. Open in browser
# Landing page:   http://localhost:8088
# Swagger UI:     http://localhost:8088/swagger-ui.html
```

---

## 🔑 Environment Variables

Create a `.env` file in the project root:

```dotenv
# PostgreSQL password
DB_PASSWORD=YourSecurePassword123!

# JWT signing secret (minimum 32 characters)
JWT_SECRET=yourJwtSecretKeyThatIsAtLeast32CharactersLong

# OpenAI API key (required for the AI assistant feature)
OPENAI_API_KEY=sk-...

# Optional: free access promo code
PROMOTIONAL_CODE=FREE_SCHOOL_2025
```

> ⚠️ `.env` is in `.gitignore` — never commit it to the repository.

---

## 🐘 Running the Database

```bash
# Start PostgreSQL container in background
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs postgres
```

PostgreSQL is available on port **5434** (non-standard to avoid conflicts):

- **Host:** `localhost`
- **Port:** `5434`
- **Database:** `school_management_db`
- **User:** `developer`
- **Password:** value from `DB_PASSWORD` in `.env`

On first startup, **Flyway automatically applies all 28 migrations** — the full schema is created without any manual steps.

---

## ▶️ Running the Application

```bash
# Option 1 — Maven Wrapper (recommended)
./mvnw spring-boot:run

# Option 2 — Build JAR and run
./mvnw clean package -DskipTests
java -jar target/school-0.0.1-SNAPSHOT.jar

# Option 3 — Load .env manually first
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

Application runs on port **8088**.

---

## 🏫 First School Setup

### Step 1 — Register a school

```bash
curl -X POST http://localhost:8088/api/purchase \
  -H "Content-Type: application/json" \
  -d '{"schoolName": "SchoolA", "contactEmail": "admin@schoola.com", "studentCount": 200, "months": 12}'
```

The response contains a temporary **activation password**.

### Step 2 — Activate and create the first admin

```bash
curl -X POST http://localhost:8088/api/purchase/register \
  -H "Content-Type: application/json" \
  -d '{
    "schoolName": "SchoolA",
    "accessPassword": "TEMP_PASSWORD_FROM_STEP_1",
    "firstName": "John",
    "secondName": "Doe",
    "email": "admin@schoola.com",
    "password": "AdminPass123!"
  }'
```

### Step 3 — Log in and get a JWT token

```bash
curl -X POST http://localhost:8088/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@schoola.com", "password": "AdminPass123!", "schoolName": "schoola"}'
```

Use the returned token in the `Authorization: Bearer <token>` header for all API requests.

### Step 4 — Open the admin panel

Navigate to `http://localhost:8088/schoola/admin` and log in.

---

## 📁 Project Structure

```
src/main/java/com/education/school/
├── config/        # Security, JWT, authentication filters
├── controller/    # 26 REST controllers + PageController for HTML routes
├── service/       # Business logic (20+ services including AI assistant)
├── repository/    # Spring Data JPA repositories
├── entity/        # 22 JPA entities
├── dto/           # 35+ request/response DTOs
└── handler/       # Global exception handlers

src/main/resources/
├── application.properties    # App configuration
├── templates/                # 35+ Thymeleaf HTML templates
├── static/js/                # 35 JavaScript modules
├── static/css/               # site.css
└── db/migration/             # 28 Flyway SQL migrations
```

---

## 👥 Roles & Access

| Role | Description | Dashboard URL |
|---|---|---|
| `ADMIN` | Full school management | `/{school}/admin` |
| `DIRECTOR` | Statistics, complaints, staff overview | `/{school}/director` |
| `TEACHER` | Grades, schedule, courses | `/{school}/teacher` |
| `STUDENT` | Grades, schedule, news | `/{school}/student` |
| `PARENT` | Child's performance | `/{school}/parent` |

All URLs use `/{schoolName}/` prefix — this is how multi-tenancy works at the routing level.

---

## 📖 API Documentation

Swagger UI is available after startup:

**`http://localhost:8088/swagger-ui.html`**

To authenticate in Swagger:
1. Get a token via `POST /api/auth/authenticate`
2. Click **Authorize** → enter `Bearer <your_token>`

---

## 🔧 Troubleshooting

### `Connection refused` to PostgreSQL
```bash
docker compose ps       # check if container is running
docker compose up -d    # start if it's not
```

### `DB_PASSWORD` not defined
```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

### Flyway checksum mismatch
```sql
-- Connect to DB and drop Flyway history (development only!)
DROP TABLE flyway_schema_history;
```

### Port 8088 already in use
```bash
lsof -i :8088           # find the process
kill -9 <PID>           # kill it
```

### AI assistant not responding
- Check that `OPENAI_API_KEY` is set in `.env`
- Verify the key is valid and has credits at platform.openai.com
- The assistant works without the key but only for read queries (no database changes)

---

## 📄 License

Proprietary license. All rights reserved.

---

*Documentation based on project version v1.0.0 · 2026*
