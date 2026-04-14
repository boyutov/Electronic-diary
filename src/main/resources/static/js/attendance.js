document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";

    const STATUS_LABELS = {
        present:    { label: "Присутствует", cls: "status-present",    icon: "✅" },
        absent:     { label: "Отсутствует",  cls: "status-absent",     icon: "❌" },
        late:       { label: "Опоздал",       cls: "status-late",       icon: "⏰" },
        reasonable: { label: "Уваж. причина", cls: "status-reasonable", icon: "📄" }
    };

    // ── УЧИТЕЛЬ / ДИРЕКТОР / АДМИН ──
    if (["TEACHER", "DIRECTOR", "ADMIN"].includes(role)) {
        document.getElementById("page-subtitle").textContent = "Отметьте посещаемость по каждому уроку";
        document.getElementById("teacher-panel").style.display = "block";

        const dateSelect = document.getElementById("date-select");
        dateSelect.value = new Date().toISOString().split("T")[0];

        document.getElementById("load-btn").addEventListener("click", loadLessons);
        loadLessons(); // загружаем уроки на сегодня сразу
    }

    // ── УЧЕНИК ──
    else if (role === "STUDENT") {
        document.getElementById("page-subtitle").textContent = "Ваша посещаемость за период";
        document.getElementById("view-panel").style.display = "block";
        initViewPanel(`/api/${schoolName}/attendance/my`);
    }

    // ── РОДИТЕЛЬ ──
    else if (role === "PARENT") {
        document.getElementById("page-subtitle").textContent = "Посещаемость ваших детей";
        document.getElementById("view-panel").style.display = "block";
        initViewPanel(`/api/${schoolName}/attendance/children`);
    }

    // ── Загрузка уроков учителя на дату ──
    function loadLessons() {
        const date = document.getElementById("date-select").value;
        if (!date) return;

        const lessonsContainer = document.getElementById("lessons-container");
        lessonsContainer.innerHTML = "<p style='color:#64748b;'>Загрузка уроков...</p>";
        document.getElementById("attendance-form-section").style.display = "block";

        // Получаем расписание учителя за выбранную дату
        apiFetch(`/api/${schoolName}/schedules/my/period?start=${date}&end=${date}`)
            .then(r => r.ok ? r.json() : [])
            .then(lessons => {
                lessonsContainer.innerHTML = "";
                if (!lessons.length) {
                    lessonsContainer.innerHTML = `<p style='color:#64748b;'>На ${new Date(date).toLocaleDateString("ru-RU")} уроков нет.</p>`;
                    return;
                }
                lessons.sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""));
                lessons.forEach(lesson => renderLessonBlock(lesson));
            })
            .catch(() => {
                lessonsContainer.innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки расписания.</p>";
            });
    }

    function renderLessonBlock(lesson) {
        const container = document.getElementById("lessons-container");
        const block = document.createElement("div");
        block.className = "card";
        block.style.marginBottom = "16px";
        block.dataset.scheduleId = lesson.id;

        const time = lesson.startTime ? lesson.startTime.slice(0, 5) + " — " + (lesson.endTime || "").slice(0, 5) : "";
        block.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;">
                <div>
                    <h3 style="margin:0;">${lesson.disciplineName}</h3>
                    <p style="color:#64748b;font-size:0.88em;margin-top:4px;">
                        ${time} · Группа: <strong>${lesson.groupName}</strong> · Каб. ${lesson.classroom || "—"}
                    </p>
                </div>
                <button class="button save-lesson-btn" data-schedule-id="${lesson.id}">💾 Сохранить</button>
            </div>
            <div class="lesson-students" data-schedule-id="${lesson.id}">
                <p style="color:#64748b;font-size:0.9em;">Загрузка учеников...</p>
            </div>
        `;

        block.querySelector(".save-lesson-btn").addEventListener("click", () => saveLessonAttendance(lesson.id));
        container.appendChild(block);

        // Загружаем учеников группы и уже сохранённую посещаемость параллельно
        Promise.all([
            apiFetch(`/api/${schoolName}/groups/${lesson.groupId}/students`).then(r => r.ok ? r.json() : []),
            apiFetch(`/api/${schoolName}/attendance/schedule/${lesson.id}`).then(r => r.ok ? r.json() : [])
        ]).then(([students, existing]) => {
            renderStudentRows(block.querySelector(".lesson-students"), students, existing);
        });
    }

    function renderStudentRows(container, students, existing) {
        if (!students.length) {
            container.innerHTML = "<p style='color:#64748b;font-size:0.9em;'>В группе нет учеников.</p>";
            return;
        }

        const existingMap = {};
        existing.forEach(a => { existingMap[a.studentId] = a; });

        // Заголовок колонок
        container.innerHTML = `
            <div style="display:grid;grid-template-columns:1fr 170px 100px 1fr;gap:8px;padding:6px 0 10px;border-bottom:2px solid var(--border);font-size:0.8em;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;">
                <span>Ученик</span><span>Статус</span><span>Опозд. (мин)</span><span>Комментарий</span>
            </div>
        `;

        students.forEach(s => {
            const ex = existingMap[s.id] || {};
            const status = ex.status || "present";

            const row = document.createElement("div");
            row.className = "attendance-row";
            row.dataset.studentId = s.id;
            row.style.cssText = "display:grid;grid-template-columns:1fr 170px 100px 1fr;gap:8px;align-items:center;padding:8px 0;border-bottom:1px solid var(--border);";
            row.innerHTML = `
                <div style="font-weight:500;font-size:0.92em;">${s.secondName} ${s.firstName}${s.thirdName ? " " + s.thirdName : ""}</div>
                <select class="status-select" style="padding:6px 8px;border-radius:8px;border:1.5px solid var(--border);font-size:0.88em;font-family:inherit;">
                    <option value="present"    ${status === "present"    ? "selected" : ""}>✅ Присутствует</option>
                    <option value="absent"     ${status === "absent"     ? "selected" : ""}>❌ Отсутствует</option>
                    <option value="late"       ${status === "late"       ? "selected" : ""}>⏰ Опоздал</option>
                    <option value="reasonable" ${status === "reasonable" ? "selected" : ""}>📄 Уваж. причина</option>
                </select>
                <input type="number" class="late-input" placeholder="мин." min="1" max="90"
                    value="${ex.lateForInMinutes || ""}"
                    style="padding:6px 8px;border-radius:8px;border:1.5px solid var(--border);font-size:0.88em;${status !== 'late' ? 'visibility:hidden;' : ''}">
                <input type="text" class="comment-input" placeholder="Комментарий"
                    value="${ex.comment || ""}"
                    style="padding:6px 8px;border-radius:8px;border:1.5px solid var(--border);font-size:0.88em;font-family:inherit;">
            `;

            const sel = row.querySelector(".status-select");
            const lateInp = row.querySelector(".late-input");
            sel.addEventListener("change", () => {
                lateInp.style.visibility = sel.value === "late" ? "visible" : "hidden";
                if (sel.value !== "late") lateInp.value = "";
            });

            container.appendChild(row);
        });
    }

    function saveLessonAttendance(scheduleId) {
        const block = document.querySelector(`[data-schedule-id="${scheduleId}"].card`);
        const rows = block.querySelectorAll(".attendance-row");
        if (!rows.length) return;

        const records = Array.from(rows).map(row => ({
            studentId: parseInt(row.dataset.studentId),
            status: row.querySelector(".status-select").value,
            lateForInMinutes: row.querySelector(".late-input").value
                ? parseInt(row.querySelector(".late-input").value) : null,
            comment: row.querySelector(".comment-input").value || null
        }));

        const btn = block.querySelector(".save-lesson-btn");
        btn.disabled = true;

        apiFetch(`/api/${schoolName}/attendance/schedule/${scheduleId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(records)
        }).then(r => {
            if (r.ok) {
                btn.textContent = "✓ Сохранено";
                btn.style.background = "var(--success)";
                setTimeout(() => {
                    btn.textContent = "💾 Сохранить";
                    btn.style.background = "";
                    btn.disabled = false;
                }, 2000);
            } else {
                alert("Ошибка при сохранении");
                btn.disabled = false;
            }
        });
    }

    // ── Просмотр (ученик/родитель) ──
    function initViewPanel(endpoint) {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split("T")[0];
        const today = now.toISOString().split("T")[0];
        document.getElementById("from-date").value = firstDay;
        document.getElementById("to-date").value = today;
        document.getElementById("view-load-btn").addEventListener("click", () => loadView(endpoint));
        loadView(endpoint);
    }

    function loadView(endpoint) {
        const from = document.getElementById("from-date").value;
        const to = document.getElementById("to-date").value;
        const container = document.getElementById("view-container");
        container.innerHTML = "<p style='color:#64748b;'>Загрузка...</p>";

        apiFetch(`${endpoint}?from=${from}&to=${to}`)
            .then(r => r.ok ? r.json() : [])
            .then(records => {
                container.innerHTML = "";
                if (!records.length) {
                    container.innerHTML = "<div class='card'><p style='color:#64748b;'>Нет данных за выбранный период.</p></div>";
                    return;
                }

                // Группируем по ученику
                const byStudent = {};
                records.forEach(a => {
                    const key = a.studentId + "|" + (a.studentName || "");
                    if (!byStudent[key]) byStudent[key] = [];
                    byStudent[key].push(a);
                });

                Object.entries(byStudent).forEach(([key, recs]) => {
                    const name = key.split("|")[1] || "Ученик";
                    const counts = { present: 0, absent: 0, late: 0, reasonable: 0 };
                    recs.forEach(r => { if (counts[r.status] !== undefined) counts[r.status]++; });

                    const rows = recs
                        .sort((a, b) => new Date(b.lessonDate) - new Date(a.lessonDate))
                        .map(a => {
                            const s = STATUS_LABELS[a.status] || { label: a.status, cls: "", icon: "" };
                            const date = a.lessonDate
                                ? new Date(a.lessonDate).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit", year: "numeric" })
                                : "—";
                            const lesson = a.lessonNumber ? `Урок ${a.lessonNumber}` : "";
                            const extra = a.status === "late" && a.lateForInMinutes
                                ? ` <span style="color:#854d0e;font-size:0.85em;">(${a.lateForInMinutes} мин.)</span>` : "";
                            return `<tr>
                                <td>${date}</td>
                                <td>${a.disciplineName || "—"} <span style="color:#94a3b8;font-size:0.82em;">${lesson}</span></td>
                                <td><span class="status-badge ${s.cls}">${s.icon} ${s.label}</span>${extra}</td>
                                <td style="color:#64748b;font-size:0.88em;">${a.comment || ""}</td>
                            </tr>`;
                        }).join("");

                    const card = document.createElement("div");
                    card.className = "card";
                    card.style.marginBottom = "16px";
                    card.innerHTML = `
                        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;flex-wrap:wrap;gap:8px;">
                            <h3 style="margin:0;">${name}</h3>
                            <div style="display:flex;gap:6px;flex-wrap:wrap;">
                                <span class="status-badge status-present">✅ ${counts.present}</span>
                                <span class="status-badge status-absent">❌ ${counts.absent}</span>
                                <span class="status-badge status-late">⏰ ${counts.late}</span>
                                <span class="status-badge status-reasonable">📄 ${counts.reasonable}</span>
                            </div>
                        </div>
                        <div class="table-wrap">
                            <table>
                                <thead><tr><th>Дата</th><th>Предмет</th><th>Статус</th><th>Комментарий</th></tr></thead>
                                <tbody>${rows}</tbody>
                            </table>
                        </div>
                    `;
                    container.appendChild(card);
                });
            })
            .catch(() => {
                container.innerHTML = "<div class='card'><p style='color:#ef4444;'>Ошибка загрузки.</p></div>";
            });
    }
});
