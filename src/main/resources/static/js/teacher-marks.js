document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    const groupSelect      = document.getElementById("group-select");
    const disciplineSelect = document.getElementById("discipline-select");
    const reasonModal      = document.getElementById("reason-modal");
    const reasonInput      = document.getElementById("reason-input");
    const reasonError      = document.getElementById("reason-error");
    const newValueInput    = document.getElementById("new-value");

    const addMarkModal    = document.getElementById("add-mark-modal");
    const addMarkValue    = document.getElementById("add-mark-value");
    const addMarkDate     = document.getElementById("add-mark-date");
    const addMarkComment  = document.getElementById("add-mark-comment");
    const addMarkError    = document.getElementById("add-mark-error");
    const addMarkHint     = document.getElementById("add-mark-date-hint");
    const addMarkLabel    = document.getElementById("add-mark-comment-label");

    let currentStudentId   = null;
    let currentStudentName = null;

    // Загружаем группы и предметы учителя
    apiFetch(`/api/${schoolName}/teachers/my/groups-disciplines`)
        .then(r => r.ok ? r.json() : [])
        .then(items => {
            const groups = {}, disciplines = {};
            items.forEach(i => {
                groups[i.groupId] = i.groupName;
                disciplines[i.disciplineId] = i.disciplineName;
            });
            Object.entries(groups).forEach(([id, name]) => {
                const opt = document.createElement("option");
                opt.value = id; opt.textContent = name;
                groupSelect.appendChild(opt);
            });
            Object.entries(disciplines).forEach(([id, name]) => {
                const opt = document.createElement("option");
                opt.value = id; opt.textContent = name;
                disciplineSelect.appendChild(opt);
            });
        });

    document.getElementById("load-btn").addEventListener("click", loadMarks);

    // ── Загрузка оценок ──
    function loadMarks() {
        const groupId      = groupSelect.value;
        const disciplineId = disciplineSelect.value;
        if (!groupId || !disciplineId) { alert("Выберите группу и предмет"); return; }

        const content = document.getElementById("marks-content");
        content.innerHTML = "<p style='color:#64748b;'>Загрузка...</p>";

        // Загружаем учеников группы и оценки параллельно
        Promise.all([
            apiFetch(`/api/${schoolName}/groups/${groupId}/students`).then(r => r.ok ? r.json() : []),
            apiFetch(`/api/${schoolName}/marks/group/${groupId}/discipline/${disciplineId}`).then(r => r.ok ? r.json() : [])
        ]).then(([students, marks]) => {
            content.innerHTML = "";

            const discName  = disciplineSelect.options[disciplineSelect.selectedIndex].text;
            const groupName = groupSelect.options[groupSelect.selectedIndex].text;

            // Индекс оценок по studentId
            const marksByStudent = {};
            marks.forEach(m => {
                if (!marksByStudent[m.studentId]) marksByStudent[m.studentId] = [];
                marksByStudent[m.studentId].push(m);
            });

            const card = document.createElement("div");
            card.className = "card";
            card.innerHTML = `
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;flex-wrap:wrap;gap:8px;">
                    <div>
                        <h3 style="margin:0;">${groupName} · ${discName}</h3>
                        <p style="color:#64748b;font-size:0.88em;margin-top:4px;">
                            Нажмите на оценку — изменить/удалить · Кнопка <strong>+</strong> — новая оценка
                        </p>
                    </div>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Ученик</th><th>Оценки</th><th class="col-center">Средний</th><th class="col-center">Добавить</th></tr></thead>
                        <tbody id="marks-tbody"></tbody>
                    </table>
                </div>
            `;
            content.appendChild(card);

            const tbody = document.getElementById("marks-tbody");

            if (!students.length) {
                tbody.innerHTML = "<tr><td colspan='4' class='td-empty'>Учеников нет</td></tr>";
                return;
            }

            students.forEach(s => {
                const sMarks = marksByStudent[s.id] || [];
                const avg = sMarks.length
                    ? (sMarks.reduce((sum, m) => sum + m.value, 0) / sMarks.length).toFixed(1)
                    : "—";
                const avgColor = avg === "—" ? "#94a3b8" : avg >= 80 ? "#15803d" : avg >= 60 ? "#854d0e" : "#b91c1c";
                const avgBg    = avg === "—" ? "#f1f5f9" : avg >= 80 ? "#dcfce7" : avg >= 60 ? "#fef9c3" : "#fee2e2";

                const chipsHtml = sMarks.map(m => {
                    const c  = m.value >= 80 ? "#dcfce7" : m.value >= 60 ? "#fef9c3" : "#fee2e2";
                    const tc = m.value >= 80 ? "#15803d" : m.value >= 60 ? "#854d0e" : "#b91c1c";
                    const date = m.createdAt
                        ? new Date(m.createdAt).toLocaleDateString("ru-RU", { day:"2-digit", month:"2-digit" })
                        : "";
                    const tip = `${m.value} · ${date}${m.comment ? " · " + m.comment : ""}`;
                    return `<span class="badge" style="background:${c};color:${tc};cursor:pointer;margin:2px;"
                        data-id="${m.id}" data-value="${m.value}" data-student="${s.secondName} ${s.firstName}"
                        title="${tip}">${m.value} <span style="font-size:0.75em;opacity:0.7;">${date}</span></span>`;
                }).join("") || `<span style="color:#cbd5e1;font-size:0.88em;">нет оценок</span>`;

                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${s.secondName} ${s.firstName}${s.thirdName ? " " + s.thirdName : ""}</strong></td>
                    <td>${chipsHtml}</td>
                    <td class="col-center">
                        <span style="font-weight:700;color:${avgColor};background:${avgBg};padding:3px 10px;border-radius:999px;">${avg}</span>
                    </td>
                    <td class="col-center">
                        <button class="button sm add-mark-btn" data-sid="${s.id}"
                            data-name="${s.secondName} ${s.firstName}"
                            style="background:#10b981;padding:4px 12px;font-size:0.85em;">+ Оценка</button>
                    </td>
                `;

                // Клик на оценку → изменить/удалить
                tr.querySelectorAll(".badge[data-id]").forEach(chip => {
                    chip.addEventListener("click", () => openReasonModal(
                        parseInt(chip.dataset.id),
                        parseInt(chip.dataset.value),
                        chip.dataset.student
                    ));
                });

                // Клик "+ Оценка"
                tr.querySelector(".add-mark-btn").addEventListener("click", btn => {
                    openAddMarkModal(
                        parseInt(btn.target.dataset.sid || btn.currentTarget.dataset.sid),
                        btn.target.dataset.name || btn.currentTarget.dataset.name
                    );
                });

                tbody.appendChild(tr);
            });
        }).catch(() => {
            document.getElementById("marks-content").innerHTML =
                "<div class='card'><p style='color:#ef4444;'>Ошибка загрузки.</p></div>";
        });
    }

    // ── Модал: новая оценка ──
    function openAddMarkModal(studentId, studentName) {
        currentStudentId   = studentId;
        currentStudentName = studentName;
        addMarkValue.value   = "";
        addMarkComment.value = "";
        addMarkError.style.display = "none";
        addMarkHint.style.display  = "none";

        const today = new Date().toISOString().split("T")[0];
        addMarkDate.value = today;
        addMarkLabel.textContent = "Комментарий";
        addMarkComment.required = false;
        addMarkComment.placeholder = "Комментарий к оценке (необязательно)";

        document.getElementById("add-mark-title").textContent = `Оценка — ${studentName}`;

        // Проверяем есть ли урок сегодня
        checkLesson(today);

        addMarkModal.classList.add("open");
    }

    // При смене даты — проверяем урок
    addMarkDate.addEventListener("change", () => checkLesson(addMarkDate.value));

    function checkLesson(date) {
        const groupId      = groupSelect.value;
        const disciplineId = disciplineSelect.value;
        if (!groupId || !disciplineId || !date) return;

        apiFetch(`/api/${schoolName}/marks/check-lesson?groupId=${groupId}&disciplineId=${disciplineId}&date=${date}`)
            .then(r => r.ok ? r.json() : { hasLesson: false })
            .then(data => {
                if (data.hasLesson) {
                    addMarkHint.textContent = "✅ Урок по расписанию — комментарий необязателен";
                    addMarkHint.style.color = "#15803d";
                    addMarkLabel.textContent = "Комментарий";
                    addMarkComment.required = false;
                    addMarkComment.placeholder = "Комментарий к оценке (необязательно)";
                } else {
                    addMarkHint.textContent = "⚠️ Урока нет в расписании — комментарий обязателен";
                    addMarkHint.style.color = "#854d0e";
                    addMarkLabel.innerHTML = "Комментарий <span style='color:#ef4444;'>*</span>";
                    addMarkComment.required = true;
                    addMarkComment.placeholder = "Обязательно укажите причину (урок вне расписания)...";
                }
                addMarkHint.style.display = "block";
            }).catch(() => {});
    }

    document.getElementById("add-mark-close").onclick  = () => addMarkModal.classList.remove("open");
    document.getElementById("add-mark-cancel").onclick = () => addMarkModal.classList.remove("open");
    addMarkModal.addEventListener("click", e => { if (e.target === addMarkModal) addMarkModal.classList.remove("open"); });

    document.getElementById("add-mark-confirm").addEventListener("click", () => {
        addMarkError.style.display = "none";
        const value   = parseInt(addMarkValue.value);
        const comment = addMarkComment.value.trim();
        const date    = addMarkDate.value;

        if (!value || value < 1 || value > 100) {
            addMarkError.textContent = "Введите оценку от 1 до 100";
            addMarkError.style.display = "block"; return;
        }
        if (addMarkComment.required && !comment) {
            addMarkError.textContent = "Комментарий обязателен — урок не по расписанию!";
            addMarkError.style.display = "block";
            addMarkComment.focus(); return;
        }

        const disciplineId = parseInt(disciplineSelect.value);
        apiFetch(`/api/${schoolName}/marks`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                studentId: currentStudentId,
                disciplineId,
                value,
                comment: comment || null,
                markDate: date || null
            })
        }).then(r => {
            if (r.ok) { addMarkModal.classList.remove("open"); loadMarks(); }
            else r.json().then(e => { addMarkError.textContent = e.error || "Ошибка"; addMarkError.style.display = "block"; });
        });
    });

    // ── Модал: изменить/удалить ──
    let currentMarkId = null;
    let currentMode   = null;

    function openReasonModal(markId, oldValue, studentName) {
        currentMarkId = markId;
        currentMode   = null;
        reasonInput.value = "";
        newValueInput.value = oldValue;
        reasonError.style.display = "none";
        document.getElementById("reason-modal-title").textContent = `Оценка ${oldValue} — ${studentName}`;
        document.getElementById("new-value-group").style.display = "block";
        document.getElementById("reason-confirm-btn").textContent = "Изменить";
        document.getElementById("reason-confirm-btn").className = "button";

        let delBtn = document.getElementById("delete-mark-btn");
        if (!delBtn) {
            delBtn = document.createElement("button");
            delBtn.id = "delete-mark-btn";
            delBtn.className = "button danger";
            delBtn.style.flex = "1";
            delBtn.textContent = "Удалить";
            document.querySelector("#reason-modal .modal-box > div:last-child").appendChild(delBtn);
        }
        delBtn.style.display = "inline-flex";
        delBtn.onclick = () => {
            currentMode = "delete";
            document.getElementById("new-value-group").style.display = "none";
            document.getElementById("reason-modal-title").textContent = `Удалить оценку ${oldValue} — ${studentName}`;
            document.getElementById("reason-confirm-btn").textContent = "Подтвердить удаление";
            document.getElementById("reason-confirm-btn").className = "button danger";
            delBtn.style.display = "none";
        };
        reasonModal.classList.add("open");
    }

    document.getElementById("reason-modal-close").onclick = () => reasonModal.classList.remove("open");
    document.getElementById("reason-cancel-btn").onclick  = () => reasonModal.classList.remove("open");
    reasonModal.addEventListener("click", e => { if (e.target === reasonModal) reasonModal.classList.remove("open"); });

    document.getElementById("reason-confirm-btn").addEventListener("click", () => {
        const reason = reasonInput.value.trim();
        if (!reason) {
            reasonError.textContent = "Причина обязательна!";
            reasonError.style.display = "block";
            reasonInput.focus(); return;
        }
        reasonError.style.display = "none";
        const mode = currentMode || "edit";

        if (mode === "delete") {
            apiFetch(`/api/${schoolName}/marks/${currentMarkId}`, {
                method: "DELETE",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ reason })
            }).then(r => {
                if (r.ok || r.status === 204) { reasonModal.classList.remove("open"); loadMarks(); }
                else r.json().then(e => { reasonError.textContent = e.error || "Ошибка"; reasonError.style.display = "block"; });
            });
        } else {
            const newVal = parseInt(newValueInput.value);
            if (!newVal || newVal < 1 || newVal > 100) {
                reasonError.textContent = "Введите корректную оценку (1–100)";
                reasonError.style.display = "block"; return;
            }
            apiFetch(`/api/${schoolName}/marks/${currentMarkId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ value: newVal, reason })
            }).then(r => {
                if (r.ok) { reasonModal.classList.remove("open"); loadMarks(); }
                else r.json().then(e => { reasonError.textContent = e.error || "Ошибка"; reasonError.style.display = "block"; });
            });
        }
    });
});
