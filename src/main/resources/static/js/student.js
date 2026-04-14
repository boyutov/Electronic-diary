document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    // Ссылка на полное расписание
    const schedLink = document.getElementById("full-schedule-link");
    if (schedLink) schedLink.href = `/${schoolName}/schedule`;

    // Расписание
    apiFetch(`/api/${schoolName}/schedules/my/today`)
        .then(r => r.ok ? r.json() : [])
        .then(schedule => {
            const c = document.getElementById("schedule-container");
            if (!schedule.length) { c.innerHTML = "<p style='color:#10b981;'>Сегодня отдыхаете! 🎉</p>"; return; }
            c.innerHTML = "";
            schedule.sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""));
            schedule.forEach(item => {
                const div = document.createElement("div");
                div.className = "schedule-item";
                div.innerHTML = `
                    <div class="schedule-time">${(item.startTime || "").slice(0,5)} — ${(item.endTime || "").slice(0,5)}</div>
                    <div class="schedule-subject">${item.disciplineName}</div>
                    <div class="schedule-details">Каб. ${item.classroom || "—"} · ${item.teacherName || ""}</div>
                `;
                c.appendChild(div);
            });
        }).catch(() => {
            document.getElementById("schedule-container").innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки.</p>";
        });

    // Оценки + приветствие
    apiFetch(`/api/${schoolName}/students/me`)
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(data => {
            document.getElementById("student-greeting").textContent = `Привет, ${data.firstName}!`;
            document.getElementById("student-info").textContent =
                `Класс: ${data.groupName || "—"} · Возраст: ${data.age}`;

            const container = document.getElementById("marks-container");
            const marks = data.marks || [];

            if (!marks.length) {
                container.innerHTML = "<p style='color:#64748b;'>У вас пока нет оценок.</p>";
                return;
            }

            const grouped = {};
            marks.forEach(m => {
                if (!grouped[m.disciplineName]) grouped[m.disciplineName] = [];
                grouped[m.disciplineName].push(m);
            });

            const allVals = marks.map(m => m.value);
            const totalAvg = (allVals.reduce((a, b) => a + b, 0) / allVals.length).toFixed(1);
            document.getElementById("marks-overall").textContent = `Средний балл: ${totalAvg}`;

            container.innerHTML = "";
            Object.entries(grouped).forEach(([discipline, dMarks]) => {
                dMarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                const avg = (dMarks.reduce((s, m) => s + m.value, 0) / dMarks.length).toFixed(1);
                const avgClass = avg >= 80 ? "green" : avg >= 60 ? "yellow" : "red";

                const chipsHtml = dMarks.map(m => {
                    const date = m.createdAt
                        ? new Date(m.createdAt).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" })
                        : "";
                    const valClass = m.value >= 80 ? "high" : m.value >= 60 ? "mid" : "low";
                    return `<div class="mark-chip">
                        <span class="chip-val ${valClass}">${m.value}</span>
                        <span class="chip-date">${date}</span>
                        ${m.comment ? `<span class="chip-comment">${m.comment}</span>` : ""}
                    </div>`;
                }).join("");

                const item = document.createElement("div");
                item.className = "accordion-item";
                item.innerHTML = `
                    <div class="accordion-header">
                        <span class="acc-title">${discipline}</span>
                        <span class="acc-meta">
                            <span class="acc-avg ${avgClass}">★ ${avg}</span>
                            <span style="color:#94a3b8;font-size:0.82em;">${dMarks.length} оц.</span>
                            <span class="accordion-arrow">▼</span>
                        </span>
                    </div>
                    <div class="accordion-body">
                        <div class="marks-row">${chipsHtml}</div>
                    </div>
                `;
                item.querySelector(".accordion-header").addEventListener("click", () => item.classList.toggle("open"));
                container.appendChild(item);
            });
        }).catch(() => {
            document.getElementById("marks-container").innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки оценок.</p>";
        });
});
