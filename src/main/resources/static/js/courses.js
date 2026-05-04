document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";

    const studentsModal = document.getElementById("students-modal");
    document.getElementById("students-modal-close").onclick = () => studentsModal.classList.remove("open");
    studentsModal.addEventListener("click", e => { if (e.target === studentsModal) studentsModal.classList.remove("open"); });

    const subtitles = {
        STUDENT:  "Запишитесь на интересующие вас курсы",
        TEACHER:  "Ваши курсы — просматривайте записавшихся студентов",
        ADMIN:    "Все курсы школы",
        DIRECTOR: "Все курсы школы"
    };
    document.getElementById("page-subtitle").textContent = subtitles[role] || "";

    if (role === "STUDENT") loadStudentView();
    else if (role === "TEACHER") loadTeacherView();
    else loadAdminView();

    // ── СТУДЕНТ ──
    function loadStudentView() {
        apiFetch(`/api/${schoolName}/courses/for-student`)
            .then(r => r.ok ? r.json() : [])
            .then(courses => renderCourses(courses, "student"))
            .catch(() => showError());
    }

    // ── УЧИТЕЛЬ ──
    function loadTeacherView() {
        apiFetch(`/api/${schoolName}/courses/my`)
            .then(r => r.ok ? r.json() : [])
            .then(courses => {
                if (!courses.length) {
                    document.getElementById("courses-container").innerHTML =
                        "<p style='color:#64748b;'>У вас пока нет курсов. Обратитесь к администратору.</p>";
                    return;
                }
                renderCourses(courses, "teacher");
            })
            .catch(() => showError());
    }

    // ── АДМИН/ДИРЕКТОР ──
    function loadAdminView() {
        apiFetch(`/api/${schoolName}/courses`)
            .then(r => r.ok ? r.json() : [])
            .then(courses => renderCourses(courses, "admin"))
            .catch(() => showError());
    }

    function renderCourses(courses, viewMode) {
        const container = document.getElementById("courses-container");
        container.innerHTML = "";
        if (!courses.length) {
            container.innerHTML = "<p style='color:#64748b;'>Курсов нет.</p>";
            return;
        }

        courses.forEach(c => {
            const card = document.createElement("div");
            card.className = "course-card";

            let footer = "";
            if (viewMode === "student") {
                if (c.enrolled) {
                    footer = `
                        <span class="badge green">✓ Записан</span>
                        <button class="button danger sm unenroll-btn" data-id="${c.id}">Отписаться</button>
                    `;
                } else {
                    footer = `
                        <span class="student-chip">👥 ${c.studentCount} студ.</span>
                        <button class="button sm enroll-btn" data-id="${c.id}">Записаться</button>
                    `;
                }
            } else if (viewMode === "teacher") {
                footer = `
                    <span class="student-chip">👥 ${c.studentCount} студ.</span>
                    <button class="button ghost sm view-students-btn" data-id="${c.id}">Список студентов</button>
                `;
            } else {
                footer = `<span class="student-chip">👥 ${c.studentCount} студ.</span>`;
            }

            card.innerHTML = `
                <div class="course-title">${c.name}</div>
                <div class="course-teacher">👨🏫 ${c.teacherName || "—"}</div>
                <div class="course-desc">${c.description || "<span style='color:#cbd5e1;'>Описание не указано</span>"}</div>
                <div class="course-footer">${footer}</div>
            `;

            if (viewMode === "student") {
                const enrollBtn = card.querySelector(".enroll-btn");
                const unenrollBtn = card.querySelector(".unenroll-btn");
                if (enrollBtn) enrollBtn.addEventListener("click", () => enroll(c.id));
                if (unenrollBtn) unenrollBtn.addEventListener("click", () => unenroll(c.id));
            } else if (viewMode === "teacher") {
                card.querySelector(".view-students-btn").addEventListener("click", () => openStudentsModal(c));
            }

            container.appendChild(card);
        });
    }

    function enroll(courseId) {
        apiFetch(`/api/${schoolName}/courses/${courseId}/enroll`, { method: "POST" })
            .then(r => r.json())
            .then(res => {
                if (res.error) { alert(res.error); return; }
                loadStudentView();
            });
    }

    function unenroll(courseId) {
        if (!confirm("Отписаться от курса?")) return;
        apiFetch(`/api/${schoolName}/courses/${courseId}/enroll`, { method: "DELETE" })
            .then(r => r.json())
            .then(res => {
                if (res.error) { alert(res.error); return; }
                loadStudentView();
            });
    }

    function openStudentsModal(course) {
        document.getElementById("students-modal-title").textContent = `Студенты — ${course.name}`;
        const body = document.getElementById("students-modal-body");

        if (!course.students || !course.students.length) {
            body.innerHTML = "<p style='color:#64748b;'>Никто ещё не записался на этот курс.</p>";
            studentsModal.classList.add("open");
            return;
        }

        body.innerHTML = `
            <p style="color:#64748b;font-size:0.88em;margin-bottom:12px;">Всего записано: <strong>${course.students.length}</strong></p>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>#</th><th>ФИО</th><th>Email</th></tr></thead>
                    <tbody>
                        ${course.students.map((s, i) => `
                            <tr>
                                <td style="color:#94a3b8;">${i + 1}</td>
                                <td><strong>${s.name}</strong></td>
                                <td style="color:#64748b;font-size:0.88em;">${s.email}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
        studentsModal.classList.add("open");
    }

    function showError() {
        document.getElementById("courses-container").innerHTML =
            "<p style='color:#ef4444;'>Ошибка загрузки курсов.</p>";
    }
});
