document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    const STATUS = {
        present:    { label: "Присутствует", icon: "✅", cls: "status-present",    color: "#15803d" },
        absent:     { label: "Отсутствует",  icon: "❌", cls: "status-absent",     color: "#b91c1c" },
        late:       { label: "Опоздал",       icon: "⏰", cls: "status-late",       color: "#854d0e" },
        reasonable: { label: "Уваж. причина", icon: "📄", cls: "status-reasonable", color: "#1d4ed8" }
    };

    // Устанавливаем даты по умолчанию — текущий месяц
    const now = new Date();
    document.getElementById("from-date").value = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split("T")[0];
    document.getElementById("to-date").value = now.toISOString().split("T")[0];

    // Загружаем группы для фильтра
    apiFetch(`/api/${schoolName}/groups`)
        .then(r => r.ok ? r.json() : [])
        .then(groups => {
            const sel = document.getElementById("group-filter");
            groups.forEach(g => {
                const opt = document.createElement("option");
                opt.value = g.id;
                opt.textContent = g.name;
                sel.appendChild(opt);
            });
        });

    document.getElementById("load-btn").addEventListener("click", loadAnalytics);
    loadAnalytics();

    function loadAnalytics() {
        const from = document.getElementById("from-date").value;
        const to = document.getElementById("to-date").value;
        const groupId = document.getElementById("group-filter").value;
        const container = document.getElementById("analytics-container");
        container.innerHTML = "<p style='color:#64748b;'>Загрузка...</p>";
        document.getElementById("summary-section").style.display = "none";

        const url = groupId
            ? `/api/${schoolName}/attendance/analytics/group/${groupId}?from=${from}&to=${to}`
            : `/api/${schoolName}/attendance/analytics?from=${from}&to=${to}`;

        apiFetch(url)
            .then(r => r.ok ? r.json() : [])
            .then(records => {
                container.innerHTML = "";
                if (!records.length) {
                    container.innerHTML = "<div class='card'><p style='color:#64748b;'>Нет данных за выбранный период.</p></div>";
                    return;
                }
                renderAnalytics(records);
            })
            .catch(() => {
                container.innerHTML = "<div class='card'><p style='color:#ef4444;'>Ошибка загрузки.</p></div>";
            });
    }

    function renderAnalytics(records) {
        // Группируем по группе → ученику
        const byGroup = {};
        records.forEach(a => {
            const groupName = a.groupName || "Без группы";
            if (!byGroup[groupName]) byGroup[groupName] = {};
            const studentKey = a.studentId + "|" + (a.studentName || "");
            if (!byGroup[groupName][studentKey]) byGroup[groupName][studentKey] = [];
            byGroup[groupName][studentKey].push(a);
        });

        // Сводная статистика
        const total = records.length;
        const totalPresent = records.filter(r => r.status === "present").length;
        const totalAbsent  = records.filter(r => r.status === "absent").length;
        const totalLate    = records.filter(r => r.status === "late").length;
        const totalReason  = records.filter(r => r.status === "reasonable").length;
        const attendanceRate = total > 0 ? Math.round(totalPresent / total * 100) : 0;

        document.getElementById("summary-section").style.display = "block";
        document.getElementById("summary-stats").innerHTML = `
            <div class="stat-card">
                <div class="stat-value" style="color:var(--primary);">${attendanceRate}%</div>
                <div class="stat-label">Общая посещаемость</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" style="color:#15803d;">${totalPresent}</div>
                <div class="stat-label">✅ Присутствовали</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" style="color:#b91c1c;">${totalAbsent}</div>
                <div class="stat-label">❌ Отсутствовали</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" style="color:#854d0e;">${totalLate}</div>
                <div class="stat-label">⏰ Опоздали</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" style="color:#1d4ed8;">${totalReason}</div>
                <div class="stat-label">📄 Уваж. причина</div>
            </div>
        `;

        const container = document.getElementById("analytics-container");

        // Рендерим каждую группу
        Object.entries(byGroup).sort(([a], [b]) => a.localeCompare(b)).forEach(([groupName, students]) => {
            const groupRecords = Object.values(students).flat();
            const gTotal   = groupRecords.length;
            const gPresent = groupRecords.filter(r => r.status === "present").length;
            const gAbsent  = groupRecords.filter(r => r.status === "absent").length;
            const gLate    = groupRecords.filter(r => r.status === "late").length;
            const gReason  = groupRecords.filter(r => r.status === "reasonable").length;
            const gRate    = gTotal > 0 ? Math.round(gPresent / gTotal * 100) : 0;
            const rateColor = gRate >= 80 ? "#15803d" : gRate >= 60 ? "#854d0e" : "#b91c1c";

            // Строки учеников
            const studentRows = Object.entries(students)
                .sort(([, a], [, b]) => {
                    // Сортируем по % посещаемости (худшие сверху)
                    const rateA = a.filter(r => r.status === "present").length / a.length;
                    const rateB = b.filter(r => r.status === "present").length / b.length;
                    return rateA - rateB;
                })
                .map(([key, recs]) => {
                    const name = key.split("|")[1] || "Ученик";
                    const sTotal   = recs.length;
                    const sPresent = recs.filter(r => r.status === "present").length;
                    const sAbsent  = recs.filter(r => r.status === "absent").length;
                    const sLate    = recs.filter(r => r.status === "late").length;
                    const sReason  = recs.filter(r => r.status === "reasonable").length;
                    const sRate    = sTotal > 0 ? Math.round(sPresent / sTotal * 100) : 0;
                    const sColor   = sRate >= 80 ? "#15803d" : sRate >= 60 ? "#854d0e" : "#b91c1c";

                    // Детали опозданий
                    const lateDetails = recs
                        .filter(r => r.status === "late" && r.lateForInMinutes)
                        .map(r => `${r.lateForInMinutes} мин.`)
                        .join(", ");

                    // Детали отсутствий с комментариями
                    const absentDetails = recs
                        .filter(r => r.status === "absent" || r.status === "reasonable")
                        .map(r => {
                            const date = r.lessonDate ? new Date(r.lessonDate).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" }) : "";
                            const disc = r.disciplineName || "";
                            const comment = r.comment ? ` (${r.comment})` : "";
                            return `${date} ${disc}${comment}`;
                        }).join("; ");

                    return `
                        <div class="student-row">
                            <div class="student-name">${name}</div>
                            <div class="student-stats">
                                <span class="status-badge status-present">✅ ${sPresent}</span>
                                <span class="status-badge status-absent">❌ ${sAbsent}</span>
                                ${sLate > 0 ? `<span class="status-badge status-late">⏰ ${sLate}${lateDetails ? " · " + lateDetails : ""}</span>` : ""}
                                ${sReason > 0 ? `<span class="status-badge status-reasonable">📄 ${sReason}</span>` : ""}
                                ${absentDetails ? `<span style="color:#94a3b8;font-size:0.8em;margin-left:4px;">${absentDetails}</span>` : ""}
                            </div>
                            <div class="student-rate" style="color:${sColor};">${sRate}%</div>
                        </div>
                    `;
                }).join("");

            const card = document.createElement("div");
            card.className = "card group-card";
            card.innerHTML = `
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                    <h3 style="margin:0;">📚 ${groupName}</h3>
                    <span style="font-size:1.3em;font-weight:700;color:${rateColor};">${gRate}%</span>
                </div>
                <div class="stat-row" style="margin-bottom:10px;">
                    <span class="status-badge status-present">✅ ${gPresent}</span>
                    <span class="status-badge status-absent">❌ ${gAbsent}</span>
                    <span class="status-badge status-late">⏰ ${gLate}</span>
                    <span class="status-badge status-reasonable">📄 ${gReason}</span>
                    <span style="color:#94a3b8;font-size:0.82em;margin-left:4px;">${gTotal} записей</span>
                </div>
                <div class="progress-bar" style="margin-bottom:16px;">
                    <div class="progress-fill" style="width:${gRate}%;background:${rateColor};"></div>
                </div>
                <div style="font-size:0.78em;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px;">
                    Ученики (отсортированы по посещаемости ↑)
                </div>
                ${studentRows}
            `;
            container.appendChild(card);
        });
    }
});
