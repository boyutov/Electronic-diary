document.addEventListener("DOMContentLoaded", () => {
    const groupSelect = document.getElementById("group-select");
    const resultsContainer = document.getElementById("schedule-results");
    const prevWeekBtn = document.getElementById("prev-week");
    const nextWeekBtn = document.getElementById("next-week");
    const currentPeriodLabel = document.getElementById("current-period");

    // Modal elements
    const modal = document.getElementById("edit-modal");
    const span = document.getElementsByClassName("close")[0];
    const editForm = document.getElementById("edit-form");

    const schoolName = window.location.pathname.split('/')[1];

    let currentStartDate = new Date();
    let currentEndDate = new Date();
    let allDisciplines = [];
    let allTeachers = [];
    let allGroups = [];

    // Загрузка данных для списков
    Promise.all([
        fetch(`/api/${schoolName}/groups`).then(r => r.json()),
        fetch(`/api/${schoolName}/disciplines`).then(r => r.json()),
        fetch(`/api/${schoolName}/teachers`).then(r => r.json())
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
    });

    function initCurrentWeek() {
        const today = new Date();
        const day = today.getDay();
        const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);
        
        currentStartDate = new Date(today.setDate(diffToMonday));
        currentEndDate = new Date(today.setDate(currentStartDate.getDate() + 6));
        
        updatePeriodLabel();
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

        fetch(`/api/${schoolName}/schedules/group/${groupId}?start=${start}&end=${end}`)
            .then(response => response.json())
            .then(schedule => {
                resultsContainer.innerHTML = "";
                if (schedule.length === 0) {
                    resultsContainer.innerHTML = "<p style='text-align: center; color: #64748b;'>На эту неделю расписания нет.</p>";
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
        fetch(`/api/${schoolName}/schedules/${id}`, {
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

        modal.style.display = "block";
    }

    // Modal close logic
    span.onclick = function() {
        modal.style.display = "none";
    }
    window.onclick = function(event) {
        if (event.target == modal) {
            modal.style.display = "none";
        }
    }

    // Edit form submit
    editForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("edit-id").value;

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

        fetch(`/api/${schoolName}/schedules/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.ok) {
                modal.style.display = "none";
                loadSchedule();
            } else {
                response.json().then(errors => {
                     let errorMessages = "";
                    if (typeof errors === 'object') {
                        for (const [field, message] of Object.entries(errors)) {
                            errorMessages += `${field}: ${message}\n`;
                        }
                    } else {
                        errorMessages = errors;
                    }
                    alert("Ошибка при обновлении урока:\n" + errorMessages);
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

    initCurrentWeek();
});
