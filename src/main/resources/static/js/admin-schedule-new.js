document.addEventListener("DOMContentLoaded", () => {
    const groupSelect = document.getElementById("group-select");
    const scheduleContainer = document.getElementById("schedule-container");
    const currentDateLabel = document.getElementById("current-date-label");
    const prevDayBtn = document.getElementById("prev-day");
    const nextDayBtn = document.getElementById("next-day");
    const dayScheduleContainer = document.getElementById("day-schedule");
    const scheduleGroupName = document.getElementById("schedule-group-name");

    // Modal elements
    const modal = document.getElementById("lesson-modal");
    const modalTitle = document.getElementById("modal-title");
    const closeModalBtn = document.getElementsByClassName("close")[0];
    const lessonForm = document.getElementById("lesson-form");
    const disciplineSelect = document.getElementById("discipline-select");
    const teacherSelect = document.getElementById("teacher-select");

    const schoolName = window.location.pathname.split('/')[1];

    let currentDate = new Date();
    let selectedGroupId = null;
    let allDisciplines = [];
    let allTeachers = [];
    let dailySchedule = [];

    // --- Data Loading ---
    function loadInitialData() {
        Promise.all([
            fetch(`/api/${schoolName}/groups`).then(r => r.json()),
            fetch(`/api/${schoolName}/disciplines`).then(r => r.json()),
            fetch(`/api/${schoolName}/teachers`).then(r => r.json())
        ]).then(([groups, disciplines, teachers]) => {
            allDisciplines = disciplines;
            allTeachers = teachers;

            groupSelect.innerHTML = "<option selected disabled>Выберите группу</option>";
            groups.forEach(group => {
                const option = document.createElement("option");
                option.value = group.id;
                option.textContent = group.name;
                groupSelect.appendChild(option);
            });

            disciplineSelect.innerHTML = "<option selected disabled>Выберите предмет</option>";
            disciplines.forEach(d => {
                const option = document.createElement("option");
                option.value = d.id;
                option.textContent = d.name;
                disciplineSelect.appendChild(option);
            });

            populateTeacherSelect();

            // Automatically select the first group if available
            if (groups.length > 0) {
                groupSelect.value = groups[0].id;
                selectedGroupId = groups[0].id;
                scheduleGroupName.textContent = `Расписание для группы: ${groups[0].name}`;
                scheduleContainer.style.display = "block";
                renderDaySchedule();
            }
        });
    }

    function populateTeacherSelect(preferredDisciplineId = null) {
        teacherSelect.innerHTML = "<option selected disabled>Выберите учителя</option>";

        let sortedTeachers = [...allTeachers];

        if (preferredDisciplineId) {
            sortedTeachers.sort((a, b) => {
                const aHas = a.disciplineIds && a.disciplineIds.includes(parseInt(preferredDisciplineId));
                const bHas = b.disciplineIds && b.disciplineIds.includes(parseInt(preferredDisciplineId));
                if (aHas && !bHas) return -1;
                if (!aHas && bHas) return 1;
                return 0;
            });
        }

        sortedTeachers.forEach(t => {
            const option = document.createElement("option");
            option.value = t.id;
            option.textContent = `${t.firstName} ${t.secondName}`;
            if (preferredDisciplineId && t.disciplineIds && t.disciplineIds.includes(parseInt(preferredDisciplineId))) {
                option.style.fontWeight = "bold";
            }
            teacherSelect.appendChild(option);
        });
    }

    disciplineSelect.addEventListener("change", () => {
        const selectedDisciplineId = disciplineSelect.value;
        populateTeacherSelect(selectedDisciplineId);
    });

    // --- Schedule Rendering ---
    function renderDaySchedule() {
        if (!selectedGroupId) return;

        const dateStr = toISODateString(currentDate);
        updateDateLabel();

        fetch(`/api/${schoolName}/schedules/group/${selectedGroupId}?start=${dateStr}&end=${dateStr}`)
            .then(response => response.json())
            .then(schedule => {
                dailySchedule = schedule.sort((a, b) => a.lessonNumber - b.lessonNumber);
                dayScheduleContainer.innerHTML = "";

                // Determine the number of slots to display
                // Show rows up to the max lesson number + 1 (for adding the next lesson)
                const maxLessonNumber = dailySchedule.reduce((max, lesson) => Math.max(max, lesson.lessonNumber), 0);
                const totalSlots = maxLessonNumber + 1;

                for (let i = 1; i <= totalSlots; i++) {
                    const lessonsForSlot = dailySchedule.filter(l => l.lessonNumber === i);
                    dayScheduleContainer.appendChild(createLessonRow(i, lessonsForSlot));
                }
            });
    }

    function createLessonRow(lessonNumber, lessons) {
        const row = document.createElement("div");
        row.className = "lesson-slot";
        
        // Show add button ONLY if there are no lessons in this slot
        const showAddButton = lessons.length === 0;
        
        const totalElements = lessons.length + (showAddButton ? 1 : 0);
        const widthPercent = 100 / totalElements;

        // Render existing lessons
        lessons.forEach(lesson => {
            const slot = createFilledLessonSlot(lesson);
            slot.style.flex = `0 0 ${widthPercent}%`; 
            slot.style.maxWidth = `${widthPercent}%`;
            row.appendChild(slot);
        });

        // Render "Add" button slot
        if (showAddButton) {
            const addSlot = createEmptyLessonSlot(lessonNumber);
            addSlot.style.flex = `0 0 ${widthPercent}%`;
            addSlot.style.maxWidth = `${widthPercent}%`;
            row.appendChild(addSlot);
        }

        return row;
    }

    function createFilledLessonSlot(lesson) {
        const slot = document.createElement("div");
        slot.className = "lesson-sub-slot filled";
        slot.innerHTML = `
            <div class="lesson-content">
                <div class="lesson-time">${lesson.startTime.slice(0, 5)} - ${lesson.endTime.slice(0, 5)}</div>
                <div class="lesson-subject">${lesson.disciplineName}</div>
                <div class="lesson-details">${lesson.teacherName} <br> Каб. ${lesson.classroom || "—"}</div>
            </div>
            <div class="lesson-actions">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-three-dots-vertical" viewBox="0 0 16 16">
                  <path d="M9.5 13a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z"/>
                </svg>
                <div class="lesson-menu">
                    <button class="edit-lesson-btn" type="button">Изменить</button>
                    <button class="delete-lesson-btn" type="button">Удалить</button>
                </div>
            </div>
        `;

        const menuContainer = slot.querySelector('.lesson-actions');
        const menu = slot.querySelector('.lesson-menu');

        menuContainer.addEventListener('click', (e) => {
            e.stopPropagation();
            // Close other menus
            document.querySelectorAll('.lesson-menu.show').forEach(m => {
                if (m !== menu) m.classList.remove('show');
            });
            menu.classList.toggle('show');
        });

        slot.querySelector('.edit-lesson-btn').addEventListener('click', (e) => {
            e.stopPropagation(); // Prevent menu toggle
            openModalForEdit(lesson);
            menu.classList.remove('show'); // Close menu
        });

        slot.querySelector('.delete-lesson-btn').addEventListener('click', (e) => {
            e.stopPropagation(); // Prevent menu toggle
            deleteLesson(lesson.id);
            menu.classList.remove('show'); // Close menu
        });

        return slot;
    }

    function createEmptyLessonSlot(lessonNumber) {
        const slot = document.createElement("div");
        slot.className = "lesson-sub-slot";
        slot.innerHTML = `<button class="add-lesson-btn">Добавить</button>`;
        slot.querySelector('button').addEventListener('click', () => openModalForAdd(lessonNumber));
        return slot;
    }

    // --- Modal Handling ---
    function openModalForAdd(lessonNumber) {
        lessonForm.reset();
        modalTitle.textContent = "Добавить урок";
        document.getElementById("lesson-id").value = "";
        document.getElementById("lesson-date").value = toISODateString(currentDate);
        document.getElementById("lesson-number").value = lessonNumber;
        modal.style.display = "block";
        populateTeacherSelect(); // Reset teacher order
    }

    function openModalForEdit(lesson) {
        lessonForm.reset();
        modalTitle.textContent = "Изменить урок";
        document.getElementById("lesson-id").value = lesson.id;
        document.getElementById("lesson-date").value = lesson.date;
        document.getElementById("lesson-number").value = lesson.lessonNumber;
        
        document.getElementById("discipline-select").value = lesson.disciplineId;

        // Populate teachers sorted by the lesson's discipline
        populateTeacherSelect(lesson.disciplineId);
        document.getElementById("teacher-select").value = lesson.teacherId;

        // Ensure time format is HH:mm
        const start = lesson.startTime.length > 5 ? lesson.startTime.substring(0, 5) : lesson.startTime;
        const end = lesson.endTime.length > 5 ? lesson.endTime.substring(0, 5) : lesson.endTime;

        document.getElementById("start-time").value = start;
        document.getElementById("end-time").value = end;
        document.getElementById("classroom-input").value = lesson.classroom;
        
        modal.style.display = "block";
    }

    function closeModal() {
        modal.style.display = "none";
    }

    // --- API Calls ---
    function handleFormSubmit(e) {
        e.preventDefault();
        const id = document.getElementById("lesson-id").value;
        const isEditing = !!id;

        const data = {
            disciplineId: parseInt(document.getElementById("discipline-select").value),
            teacherId: parseInt(document.getElementById("teacher-select").value),
            groupId: parseInt(selectedGroupId),
            date: document.getElementById("lesson-date").value,
            startTime: document.getElementById("start-time").value + ":00",
            endTime: document.getElementById("end-time").value + ":00",
            lessonNumber: parseInt(document.getElementById("lesson-number").value),
            classroom: document.getElementById("classroom-input").value
        };

        const url = isEditing ? `/api/${schoolName}/schedules/${id}` : `/api/${schoolName}/schedules`;
        const method = isEditing ? "PUT" : "POST";

        fetch(url, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(response => {
            if (response.ok) {
                closeModal();
                renderDaySchedule();
            } else {
                response.json().then(err => alert("Ошибка: " + JSON.stringify(err)));
            }
        });
    }

    function deleteLesson(id) {
        if (!confirm("Вы уверены, что хотите удалить этот урок?")) return;
        fetch(`/api/${schoolName}/schedules/${id}`, { method: "DELETE" })
            .then(response => {
                if (response.ok) {
                    renderDaySchedule();
                } else {
                    alert("Ошибка при удалении.");
                }
            });
    }

    // --- Event Listeners ---
    groupSelect.addEventListener("change", () => {
        selectedGroupId = groupSelect.value;
        const selectedOption = groupSelect.options[groupSelect.selectedIndex];
        scheduleGroupName.textContent = `Расписание для группы: ${selectedOption.text}`;
        scheduleContainer.style.display = "block";
        currentDate = new Date(); // Reset to today on group change
        renderDaySchedule();
    });

    prevDayBtn.addEventListener("click", () => {
        currentDate.setDate(currentDate.getDate() - 1);
        renderDaySchedule();
    });

    nextDayBtn.addEventListener("click", () => {
        currentDate.setDate(currentDate.getDate() + 1);
        renderDaySchedule();
    });

    closeModalBtn.onclick = closeModal;
    window.onclick = (event) => {
        if (event.target == modal) closeModal();
        if (!event.target.closest('.lesson-actions')) {
            document.querySelectorAll('.lesson-menu.show').forEach(m => m.classList.remove('show'));
        }
    };
    lessonForm.addEventListener("submit", handleFormSubmit);

    // --- Helpers ---
    function updateDateLabel() {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        let dateString = currentDate.toLocaleDateString('ru-RU', options);
        // Capitalize first letter
        dateString = dateString.charAt(0).toUpperCase() + dateString.slice(1);
        currentDateLabel.textContent = dateString;
    }

    function toISODateString(date) {
        return new Date(date.getTime() - (date.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
    }

    // --- Init ---
    loadInitialData();
});
