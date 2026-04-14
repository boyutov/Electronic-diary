document.addEventListener("DOMContentLoaded", () => {
    const groupSelect = document.getElementById("group-select");
    const resultsContainer = document.getElementById("schedule-results");
    const prevWeekBtn = document.getElementById("prev-week");
    const nextWeekBtn = document.getElementById("next-week");
    const currentPeriodLabel = document.getElementById("current-period");

    // Modal elements
    const modal = document.getElementById("edit-modal");
    const editForm = document.getElementById("edit-form");
    const editModalClose = document.getElementById("edit-modal-close");

    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];

    let currentStartDate = new Date();
    let currentEndDate = new Date();
    let allDisciplines = [];
    let allTeachers = [];
    let allGroups = [];

    // Загрузка данных для списков
    Promise.all([
        apiFetch(`/api/${schoolName}/groups`).then(r => r.json()),
        apiFetch(`/api/${schoolName}/disciplines`).then(r => r.json()),
        apiFetch(`/api/${schoolName}/teachers`).then(r => r.json())
    ]).then(([groups, disciplines, teachers]) => {
        allGroups = groups;
        allDisciplines = disciplines;
        allTeachers = teachers;

        // Заполняем основной селект групп
        groupSelect.innerHTML = "<option selected disabled>Выберите группу</option>";
        groups.forEach(group => {
            const option = document.createElement("option");
            option.value = group.id;
            option.textContent = group.name;
            groupSelect.appendChild(option);
        });

        // Заполняем селекты в модальном окне
        const editGroupSelect = document.getElementById("edit-group");
        groups.forEach(group => {
            const option = document.createElement("option");
            option.value = group.id;
            option.textContent = group.name;
            editGroupSelect.appendChild(option);
        });

        const editDisciplineSelect = document.getElementById("edit-discipline");
        disciplines.forEach(discipline => {
            const option = document.createElement("option");
            option.value = discipline.id;
            option.textContent = discipline.name;
            editDisciplineSelect.appendChild(option);
        });

        const editTeacherSelect = document.getElementById("edit-teacher");
        teachers.forEach(teacher => {
            const option = document.createElement("option");
            option.value = teacher.id;
            option.textContent = `${teacher.firstName} ${teacher.secondName}`;
            editTeacherSelect.appendChild(option);
        });
        
        // Загружаем расписание для первой группы
        if (groups.length > 0) {
            groupSelect.value = groups[0].id;
            initCurrentWeek(); // Вызываем здесь, чтобы сначала выбрать группу
        } else {
            resultsContainer.innerHTML = "<p style='text-align: center;'>Нет групп. Сначала создайте группу.</p>";
        }
    });

    function initCurrentWeek() {
        const today = new Date();
        const day = today.getDay();
        const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);
        
        currentStartDate = new Date(today.setDate(diffToMonday));
        currentEndDate = new Date(today.setDate(currentStartDate.getDate() + 6));
        
        updatePeriodLabel();
        loadSchedule(); // Вызываем загрузку расписания после установки дат
    }

    function updatePeriodLabel() {
        const options = { day: 'numeric', month: 'long' };
        currentPeriodLabel.textContent = `${currentStartDate.toLocaleDateString('ru-RU', options)} — ${currentEndDate.toLocaleDateString('ru-RU', options)}`;
    }

    function loadSchedule() {
        const groupId = groupSelect.value;
        if (!groupId || groupId === "Выберите группу") return;

        const start = new Date(currentStartDate.getTime() - (currentStartDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
        const end = new Date(currentEndDate.getTime() - (currentEndDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0];

        resultsContainer.innerHTML = "<p>Загрузка...</p>";

        apiFetch(`/api/${schoolName}/schedules/group/${groupId}?start=${start}&end=${end}`)
            .then(response => response.json())
            .then(schedule => {
                resultsContainer.innerHTML = "";
                
                // Создаем кнопку добавления даже если расписания нет
                const topAddBtnContainer = document.createElement("div");
                topAddBtnContainer.style.marginBottom = "15px";
                topAddBtnContainer.style.display = "flex";
                topAddBtnContainer.style.justifyContent = "flex-end";
                
                const topAddBtn = document.createElement("button");
                topAddBtn.className = "button";
                topAddBtn.textContent = "+ Создать урок";
                topAddBtn.onclick = () => openCreateModal(); // Открываем модалку для создания
                
                topAddBtnContainer.appendChild(topAddBtn);
                resultsContainer.appendChild(topAddBtnContainer);

                if (schedule.length === 0) {
                    const noScheduleMsg = document.createElement("p");
                    noScheduleMsg.style.textAlign = "center";
                    noScheduleMsg.style.color = "#64748b";
                    noScheduleMsg.textContent = "На эту неделю расписания нет.";
                    resultsContainer.appendChild(noScheduleMsg);
                    return;
                }

                const grouped = {};
                schedule.forEach(item => {
                    if (!grouped[item.date]) {
                        grouped[item.date] = [];
                    }
                    grouped[item.date].push(item);
                });

                const sortedDates = Object.keys(grouped).sort();

                sortedDates.forEach(date => {
                    const dateObj = new Date(date);
                    const dateHeader = document.createElement("h3");
                    dateHeader.style.marginTop = "20px";
                    dateHeader.style.borderBottom = "1px solid #e2e8f0";
                    dateHeader.style.paddingBottom = "8px";
                    dateHeader.textContent = dateObj.toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });
                    dateHeader.textContent = dateHeader.textContent.charAt(0).toUpperCase() + dateHeader.textContent.slice(1);
                    
                    resultsContainer.appendChild(dateHeader);

                    const grid = document.createElement("div");
                    grid.className = "schedule-grid";

                    grouped[date].sort((a, b) => a.startTime.localeCompare(b.startTime));

                    grouped[date].forEach(item => {
                        const div = document.createElement("div");
                        div.className = "schedule-item";
                        // Сохраняем полные данные в dataset для редактирования
                        div.dataset.schedule = JSON.stringify(item);

                        div.innerHTML = `
                            <div class="schedule-time">${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)}</div>
                            <div class="schedule-subject">${item.disciplineName}</div>
                            <div class="schedule-details">
                                Каб. ${item.classroom || "—"}<br>
                                ${item.teacherName}
                            </div>
                            <button class="action-btn edit-btn">Редактировать</button>
                            <button class="action-btn delete-btn" data-id="${item.id}">Удалить</button>
                        `;
                        grid.appendChild(div);
                    });
                    resultsContainer.appendChild(grid);
                });

                // Обработчики кнопок
                document.querySelectorAll(".delete-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => {
                        if (confirm("Вы уверены, что хотите удалить этот урок?")) {
                            deleteSchedule(e.target.dataset.id);
                        }
                    });
                });

                document.querySelectorAll(".edit-btn").forEach(btn => {
                    btn.addEventListener("click", (e) => {
                        const item = JSON.parse(e.target.parentElement.dataset.schedule);
                        openEditModal(item);
                    });
                });
            })
            .catch(error => {
                console.error("Error:", error);
                resultsContainer.innerHTML = "<p>Ошибка загрузки.</p>";
            });
    }

    function deleteSchedule(id) {
        apiFetch(`/api/${schoolName}/schedules/${id}`, {
            method: "DELETE"
        })
        .then(response => {
            if (response.ok) {
                loadSchedule();
            } else {
                alert("Ошибка при удалении урока");
            }
        });
    }
    
    // Новая функция для создания урока
    function openCreateModal() {
        document.getElementById("edit-id").value = ""; // Очищаем ID (признак создания)
        document.getElementById("edit-form").reset(); // Очищаем форму
        
        // Предзаполняем группу
        const groupId = groupSelect.value;
        if (groupId) {
            document.getElementById("edit-group").value = groupId;
        }
        
        // Предзаполняем дату (например, понедельник текущей выбранной недели)
        const start = new Date(currentStartDate.getTime() - (currentStartDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
        document.getElementById("edit-date").value = start;

        modal.classList.add("open");
    }

    function openEditModal(item) {
        document.getElementById("edit-id").value = item.id;

        document.getElementById("edit-discipline").value = item.disciplineId;
        document.getElementById("edit-teacher").value = item.teacherId;
        document.getElementById("edit-group").value = item.groupId;

        document.getElementById("edit-date").value = item.date;
        document.getElementById("edit-startTime").value = item.startTime;
        document.getElementById("edit-endTime").value = item.endTime;
        document.getElementById("edit-lessonNumber").value = item.lessonNumber;
        document.getElementById("edit-classroom").value = item.classroom;

        modal.classList.add("open");
    }

    // Modal close logic
    editModalClose.onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    // Edit/Create form submit
    editForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("edit-id").value;
        const isEdit = id !== ""; // Если есть ID, то редактируем, иначе создаем

        const data = {
            disciplineId: parseInt(document.getElementById("edit-discipline").value),
            teacherId: parseInt(document.getElementById("edit-teacher").value),
            groupId: parseInt(document.getElementById("edit-group").value),
            date: document.getElementById("edit-date").value,
            startTime: document.getElementById("edit-startTime").value + (document.getElementById("edit-startTime").value.length === 5 ? ":00" : ""),
            endTime: document.getElementById("edit-endTime").value + (document.getElementById("edit-endTime").value.length === 5 ? ":00" : ""),
            lessonNumber: parseInt(document.getElementById("edit-lessonNumber").value),
            classroom: document.getElementById("edit-classroom").value
        };

        const url = isEdit ? `/api/${schoolName}/schedules/${id}` : `/api/${schoolName}/schedules`;
        const method = isEdit ? "PUT" : "POST";

        apiFetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                modal.classList.remove("open");
                loadSchedule();
            } else {
                response.json().then(errors => {
                    alert(`Ошибка: ` + (typeof errors === 'object' ? Object.values(errors).join(", ") : String(errors)));
                });
            }
        });
    });

    groupSelect.addEventListener("change", loadSchedule);

    prevWeekBtn.addEventListener("click", () => {
        currentStartDate.setDate(currentStartDate.getDate() - 7);
        currentEndDate.setDate(currentEndDate.getDate() - 7);
        updatePeriodLabel();
        loadSchedule();
    });

    nextWeekBtn.addEventListener("click", () => {
        currentStartDate.setDate(currentStartDate.getDate() + 7);
        currentEndDate.setDate(currentEndDate.getDate() + 7);
        updatePeriodLabel();
        loadSchedule();
    });
});