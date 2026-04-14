document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    const markModal = document.getElementById("mark-modal");
    const markForm = document.getElementById("mark-form");
    const markStudentsContainer = document.getElementById("mark-students-container");
    const markModalDiscipline = document.getElementById("mark-modal-discipline");
    const markModalGroup = document.getElementById("mark-modal-group");
    let currentMarkingDisciplineId = null;

    // Приветствие
    apiFetch(`/api/${schoolName}/profile/me`)
        .then(r => r.ok ? r.json() : null)
        .then(profile => {
            if (profile) document.getElementById("teacher-greeting").textContent =
                `${profile.firstName} ${profile.secondName}`;
        }).catch(() => {});

    // Куратор
    apiFetch(`/api/${schoolName}/profile/curator`)
        .then(r => r.ok ? r.json() : null)
        .then(p => {
            if (!p) return;
            document.getElementById("curator-panel").style.display = "block";
            document.getElementById("curator-group-name").textContent = p.groupName;
            document.getElementById("view-my-class-btn").href = `/${schoolName}/curator/students-list`;
            const link = document.getElementById("curator-link");
            if (link) { link.style.display = "flex"; link.href = `/${schoolName}/curator/students-list`; }
        }).catch(() => {});

    // Мои группы
    apiFetch(`/api/${schoolName}/teachers/my/groups-disciplines`)
        .then(r => r.ok ? r.json() : [])
        .then(items => {
            const container = document.getElementById("groups-container");
            if (!items.length) {
                container.innerHTML = "<p style='color:#64748b;'>У вас пока нет групп в расписании.</p>";
                return;
            }
            const byGroup = {};
            items.forEach(item => {
                if (!byGroup[item.groupId]) byGroup[item.groupId] = { name: item.groupName, disciplines: [] };
                if (!byGroup[item.groupId].disciplines.find(d => d.id === item.disciplineId)) {
                    byGroup[item.groupId].disciplines.push({ id: item.disciplineId, name: item.disciplineName });
                }
            });
            container.innerHTML = "";
            Object.entries(byGroup).forEach(([groupId, group]) => {
                const card = document.createElement("div");
                card.style.cssText = "border:1.5px solid var(--border);border-radius:10px;padding:16px;margin-bottom:12px;";
                card.innerHTML = `
                    <div style="font-weight:700;font-size:1em;margin-bottom:10px;">📚 ${group.name}</div>
                    <div style="display:flex;flex-wrap:wrap;gap:8px;">
                        ${group.disciplines.map(d => `
                            <button class="button sm" style="background:#0ea5e9;"
                                onclick="openMassMarkModal(${groupId}, '${group.name}', ${d.id}, '${d.name}')">
                                ${d.name} — Оценить
                            </button>
                        `).join("")}
                    </div>
                `;
                container.appendChild(card);
            });
        }).catch(() => {
            document.getElementById("groups-container").innerHTML =
                "<p style='color:#ef4444;'>Ошибка загрузки групп.</p>";
        });

    // Расписание на сегодня
    apiFetch(`/api/${schoolName}/schedules/my/today`)
        .then(r => r.ok ? r.json() : [])
        .then(schedule => {
            const container = document.getElementById("schedule-container");
            if (!schedule.length) {
                container.innerHTML = "<p style='color:#10b981;font-size:1.1em;'>Сегодня отдыхаете! 🎉</p>";
                return;
            }
            container.innerHTML = "";
            schedule.sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""));
            schedule.forEach(item => {
                const div = document.createElement("div");
                div.className = "schedule-item";
                div.innerHTML = `
                    <div class="schedule-time">${(item.startTime || "").slice(0,5)} — ${(item.endTime || "").slice(0,5)}</div>
                    <div class="schedule-subject">${item.disciplineName}</div>
                    <div class="schedule-details">Каб. ${item.classroom || "—"} · ${item.groupName}</div>
                `;
                container.appendChild(div);
            });
        }).catch(() => {
            document.getElementById("schedule-container").innerHTML =
                "<p style='color:#ef4444;'>Ошибка загрузки расписания.</p>";
        });

    // Модал оценок
    window.openMassMarkModal = function(groupId, groupName, disciplineId, disciplineName) {
        currentMarkingDisciplineId = disciplineId;
        markModalDiscipline.textContent = disciplineName;
        markModalGroup.textContent = groupName;
        markStudentsContainer.innerHTML = "<p style='color:#64748b;'>Загрузка учеников...</p>";

        apiFetch(`/api/${schoolName}/groups/${groupId}/students`)
            .then(r => r.ok ? r.json() : [])
            .then(students => {
                markStudentsContainer.innerHTML = "";
                if (!students.length) {
                    markStudentsContainer.innerHTML = "<p style='color:#64748b;'>В этом классе нет учеников.</p>";
                    return;
                }
                students.forEach(student => {
                    const row = document.createElement("div");
                    row.className = "student-mark-row";
                    row.innerHTML = `
                        <div style="flex:1;font-weight:500;">${student.secondName} ${student.firstName}</div>
                        <input type="text" placeholder="Комментарий" class="mark-comment-input" data-student-id="${student.id}" style="width:160px;">
                        <input type="number" min="1" max="100" placeholder="Балл" class="mark-value-input" data-student-id="${student.id}" style="width:80px;text-align:center;">
                    `;
                    markStudentsContainer.appendChild(row);
                });
            });

        markModal.classList.add("open");
    };

    document.getElementById("mark-modal-close").onclick = () => markModal.classList.remove("open");
    markModal.addEventListener("click", e => { if (e.target === markModal) markModal.classList.remove("open"); });

    markForm.addEventListener("submit", e => {
        e.preventDefault();
        const markInputs = document.querySelectorAll(".mark-value-input");
        const commentInputs = document.querySelectorAll(".mark-comment-input");
        const promises = [];

        markInputs.forEach((input, i) => {
            if (!input.value) return;
            promises.push(
                apiFetch(`/api/${schoolName}/marks`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        studentId: parseInt(input.dataset.studentId),
                        disciplineId: currentMarkingDisciplineId,
                        value: parseInt(input.value),
                        comment: commentInputs[i].value || null
                    })
                }).then(r => { if (!r.ok) throw new Error("Failed"); })
            );
        });

        if (!promises.length) { alert("Вы не поставили ни одной оценки."); return; }

        Promise.all(promises)
            .then(() => { markModal.classList.remove("open"); alert("Оценки сохранены!"); })
            .catch(() => alert("Ошибка при сохранении некоторых оценок."));
    });

    // Голосования
    if (window.initPolls) initPolls("polls-container", schoolName);
});
