document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";
    const canManage = ["TEACHER", "ADMIN", "DIRECTOR"].includes(role);

    const TYPE_LABELS = {
        EXAM:     { label: "Экзамен",          icon: "📋", cls: "exam-type-EXAM" },
        TEST:     { label: "Тест",            icon: "📄", cls: "exam-type-TEST" },
        QUIZ:     { label: "Контрольная",   icon: "❓", cls: "exam-type-QUIZ" },
        HOMEWORK: { label: "Домашнее задание", icon: "📚", cls: "exam-type-HOMEWORK" }
    };

    const subtitles = {
        TEACHER:  "Ваши задания — экзамены, тесты, домашние задания",
        ADMIN:    "Все задания школы",
        DIRECTOR: "Все задания школы",
        STUDENT:  "Предстоящие экзамены, тесты и домашние задания",
        PARENT:   "Задания ваших детей"
    };
    document.getElementById("page-subtitle").textContent = subtitles[role] || "";

    if (canManage) document.getElementById("add-btn").style.display = "inline-flex";

    const modal = document.getElementById("modal");
    const form = document.getElementById("exam-form");
    const formError = document.getElementById("form-error");
    const groupsContainer = document.getElementById("groups-container");
    let allGroups = [];

    // Загружаем предметы и группы для формы
    if (canManage) {
        apiFetch(`/api/${schoolName}/disciplines`)
            .then(r => r.ok ? r.json() : [])
            .then(disciplines => {
                const sel = document.getElementById("discipline");
                disciplines.forEach(d => {
                    const opt = document.createElement("option");
                    opt.value = d.id;
                    opt.textContent = d.name;
                    sel.appendChild(opt);
                });
            });

        apiFetch(`/api/${schoolName}/groups`)
            .then(r => r.ok ? r.json() : [])
            .then(groups => {
                allGroups = groups;
                renderGroupCheckboxes([]);
            });
    }

    function renderGroupCheckboxes(selectedIds) {
        groupsContainer.innerHTML = "";
        if (!allGroups.length) {
            groupsContainer.innerHTML = "<p style='color:#64748b;font-size:0.9em;'>Групп нет</p>";
            return;
        }
        allGroups.forEach(g => {
            const div = document.createElement("div");
            div.className = "checkbox-item";
            div.innerHTML = `
                <input type="checkbox" id="grp-${g.id}" value="${g.id}" name="groups"
                    ${selectedIds.includes(g.id) ? "checked" : ""}>
                <label for="grp-${g.id}">${g.name}${g.course ? " (" + g.course + " кл.)" : ""}</label>
            `;
            groupsContainer.appendChild(div);
        });
    }

    function getSelectedGroupIds() {
        return Array.from(document.querySelectorAll("input[name='groups']:checked"))
            .map(cb => parseInt(cb.value));
    }

    function daysLeftBadge(dateStr, type) {
        const today = new Date(); today.setHours(0,0,0,0);
        const exam = new Date(dateStr);
        const diff = Math.round((exam - today) / 86400000);
        const isHW = type === "HOMEWORK";
        if (diff < 0)  return `<span class="days-left days-past">${isHW ? "Прошло" : "Прошёл"}</span>`;
        if (diff === 0) return `<span class="days-left days-soon">${isHW ? "Сдать сегодня!" : "Сегодня!"}</span>`;
        if (diff <= 3)  return `<span class="days-left days-soon">Через ${diff} дн.</span>`;
        if (diff <= 7)  return `<span class="days-left days-near">Через ${diff} дн.</span>`;
        return `<span class="days-left days-normal">Через ${diff} дн.</span>`;
    }

    function loadExams() {
        const endpoint = (role === "ADMIN" || role === "DIRECTOR")
            ? `/api/${schoolName}/exams`
            : `/api/${schoolName}/exams/my`;

        apiFetch(endpoint)
            .then(r => r.ok ? r.json() : [])
            .then(exams => {
                const container = document.getElementById("exams-container");
                container.innerHTML = "";
                if (!exams.length) {
                    container.innerHTML = "<p style='color:#64748b;'>Экзаменов и тестов нет.</p>";
                    return;
                }

                // Сортируем: сначала предстоящие, потом прошедшие
                const today = new Date(); today.setHours(0,0,0,0);
                exams.sort((a, b) => {
                    const da = new Date(a.examDate), db = new Date(b.examDate);
                    const pastA = da < today, pastB = db < today;
                    if (pastA !== pastB) return pastA ? 1 : -1;
                    return da - db;
                });

                exams.forEach(e => {
                    const t = TYPE_LABELS[e.type] || { label: e.type, icon: "📝", cls: "" };
                    const isHW = e.type === "HOMEWORK";
                    const date = new Date(e.examDate).toLocaleDateString("ru-RU", { day: "2-digit", month: "long", year: "numeric" });
                    const time = e.examTime ? e.examTime.slice(0, 5) : "";
                    const dateLabel = isHW ? "📅 Сдать до:" : "📅";
                    const groupsHtml = e.groupNames && e.groupNames.length
                        ? e.groupNames.map(n => `<span class="badge blue">${n}</span>`).join("")
                        : `<span class="badge gray">Все классы</span>`;
                    const teacher = e.createdByName ? `<span>👨🏫 ${e.createdByName}</span>` : "";
                    const actionsHtml = canManage ? `
                        <div style="display:flex;gap:6px;margin-top:12px;">
                            <button class="button warning sm edit-btn" data-id="${e.id}">Ред.</button>
                            <button class="button danger sm delete-btn" data-id="${e.id}">Удал.</button>
                        </div>` : "";

                    const card = document.createElement("div");
                    card.className = "exam-card";
                    card.innerHTML = `
                        <div class="exam-card-header">
                            <div>
                                <span class="badge ${t.cls}" style="margin-bottom:6px;display:inline-block;">${t.icon} ${t.label}</span>
                                <div class="exam-card-title">${e.title}</div>
                            </div>
                            ${daysLeftBadge(e.examDate, e.type)}
                        </div>
                        <div class="exam-card-meta">
                            ${dateLabel} ${date}${time ? " · ⏰ " + time : ""}
                            ${e.disciplineName ? " · 📖 " + e.disciplineName : ""}
                        </div>
                        ${teacher ? `<div class="exam-card-meta">${teacher}</div>` : ""}
                        ${e.description ? `<div class="exam-card-desc">${e.description}</div>` : ""}
                        <div class="exam-card-groups">${groupsHtml}</div>
                        ${actionsHtml}
                    `;

                    if (canManage) {
                        card.querySelector(".edit-btn").addEventListener("click", () => openEditModal(e));
                        card.querySelector(".delete-btn").addEventListener("click", () => deleteExam(e.id));
                    }
                    container.appendChild(card);
                });
            })
            .catch(() => {
                document.getElementById("exams-container").innerHTML =
                    "<p style='color:#ef4444;'>Ошибка загрузки.</p>";
            });
    }

    function openEditModal(e) {
        document.getElementById("modal-title").textContent = "Редактировать";
        document.getElementById("exam-id").value = e.id;
        document.getElementById("type").value = e.type;
        document.getElementById("date-label").textContent = e.type === "HOMEWORK" ? "Срок сдачи" : "Дата";
        document.getElementById("title").value = e.title;
        document.getElementById("discipline").value = e.disciplineId || "";
        document.getElementById("examDate").value = e.examDate;
        document.getElementById("examTime").value = e.examTime ? e.examTime.slice(0, 5) : "";
        document.getElementById("description").value = e.description || "";
        renderGroupCheckboxes(e.groupIds || []);
        formError.style.display = "none";
        modal.classList.add("open");
    }

    // Динамическая метка даты в зависимости от типа
    document.getElementById("type").addEventListener("change", function() {
        const label = document.getElementById("date-label");
        label.textContent = this.value === "HOMEWORK" ? "Срок сдачи" : "Дата";
    });

    document.getElementById("add-btn").onclick = () => {
        document.getElementById("modal-title").textContent = "Добавить";
        form.reset();
        document.getElementById("exam-id").value = "";
        document.getElementById("date-label").textContent = "Дата";
        document.getElementById("examDate").value = new Date().toISOString().split("T")[0];
        renderGroupCheckboxes([]);
        formError.style.display = "none";
        modal.classList.add("open");
    };

    document.getElementById("modal-close").onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("exam-id").value;
        const discVal = document.getElementById("discipline").value;
        const data = {
            type: document.getElementById("type").value,
            title: document.getElementById("title").value,
            disciplineId: discVal ? parseInt(discVal) : null,
            examDate: document.getElementById("examDate").value,
            examTime: document.getElementById("examTime").value || null,
            description: document.getElementById("description").value,
            groupIds: getSelectedGroupIds()
        };
        apiFetch(id ? `/api/${schoolName}/exams/${id}` : `/api/${schoolName}/exams`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadExams(); }
            else r.json().then(err => {
                formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                formError.style.display = "block";
            });
        });
    });

    function deleteExam(id) {
        if (!confirm("Удалить?")) return;
        apiFetch(`/api/${schoolName}/exams/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadExams(); else alert("Ошибка при удалении"); });
    }

    loadExams();
});
