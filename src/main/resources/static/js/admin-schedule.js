document.addEventListener("DOMContentLoaded", () => {
    const groupSelect = document.getElementById("group-select");
    const lessonsContainer = document.getElementById("lessons-container");
    const prevDayBtn = document.getElementById("prev-day");
    const nextDayBtn = document.getElementById("next-day");
    const dayNameEl = document.getElementById("day-name");
    const fullDateEl = document.getElementById("full-date");
    
    // Modal
    const modal = document.getElementById("modal");
    const modalTitle = document.getElementById("modal-title");
    const form = document.getElementById("schedule-form");
    const closeBtn = document.getElementsByClassName("close")[0];
    
    const schoolName = window.location.pathname.split('/')[1];
    let currentDate = new Date();
    let currentSchedule = [];

    // Инициализация
    Promise.all([
        fetch(`/api/${schoolName}/groups`).then(r => r.json()),
        fetch(`/api/${schoolName}/disciplines`).then(r => r.json()),
        fetch(`/api/${schoolName}/teachers`).then(r => r.json())
    ]).then(([groups, disciplines, teachers]) => {
        // Заполняем группы
        groupSelect.innerHTML = "";
        groups.forEach(group => {
            const option = document.createElement("option");
            option.value = group.id;
            option.textContent = group.name;
            groupSelect.appendChild(option);
        });
        
        // Заполняем селекты в модальном окне
        const disciplineSelect = document.getElementById("discipline");
        disciplines.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.textContent = d.name;
            disciplineSelect.appendChild(opt);
        });
        
        const teacherSelect = document.getElementById("teacher");
        teachers.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.id;
            opt.textContent = `${t.firstName} ${t.secondName}`;
            teacherSelect.appendChild(opt);
        });

        // Загружаем расписание для первой группы
        if (groups.length > 0) {
            groupSelect.value = groups[0].id;
            updateDateDisplay();
            loadSchedule();
        } else {
            lessonsContainer.innerHTML = "<p style='text-align: center;'>Нет групп. Сначала создайте группу.</p>";
        }
    });

    function updateDateDisplay() {
        const options = { weekday: 'long' };
        const dateOptions = { day: 'numeric', month: 'long', year: 'numeric' };
        
        let dayName = currentDate.toLocaleDateString('ru-RU', options);
        dayName = dayName.charAt(0).toUpperCase() + dayName.slice(1);
        
        dayNameEl.textContent = dayName;
        fullDateEl.textContent = currentDate.toLocaleDateString('ru-RU', dateOptions);
    }

    function loadSchedule() {
        const groupId = groupSelect.value;
        if (!groupId) return;

        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        const day = String(currentDate.getDate()).padStart(2, '0');
        const dateStr = `${year}-${month}-${day}`;

        fetch(`/api/${schoolName}/schedules/group/${groupId}?start=${dateStr}&end=${dateStr}`)
            .then(r => r.json())
            .then(schedule => {
                // Сортируем по номеру урока
                currentSchedule = schedule.sort((a, b) => a.lessonNumber - b.lessonNumber);
                renderSlots();
            });
    }

    function renderSlots() {
        lessonsContainer.innerHTML = "";
        
        // Рендерим существующие уроки
        currentSchedule.forEach(lesson => {
            const slot = document.createElement("div");
            slot.className = "lesson-slot";
            
            const numberDiv = document.createElement("div");
            numberDiv.className = "lesson-number";
            numberDiv.textContent = lesson.lessonNumber;
            slot.appendChild(numberDiv);
            
            const contentDiv = document.createElement("div");
            contentDiv.className = "lesson-content";
            
            contentDiv.innerHTML = `
                <div class="lesson-card">
                    <div class="lesson-info">
                        <h4>${lesson.disciplineName}</h4>
                        <p>${lesson.startTime.slice(0,5)} - ${lesson.endTime.slice(0,5)} | Каб. ${lesson.classroom || "—"} | ${lesson.teacherName}</p>
                    </div>
                    <div class="lesson-actions">
                        <div class="menu-dots" onclick="toggleMenu(${lesson.id})">⋮</div>
                        <div id="menu-${lesson.id}" class="dropdown-menu">
                            <button class="dropdown-item" onclick="openEditModal(${lesson.id})">Изменить</button>
                            <button class="dropdown-item delete" onclick="deleteLesson(${lesson.id})">Удалить</button>
                        </div>
                    </div>
                </div>
            `;
            
            slot.appendChild(contentDiv);
            lessonsContainer.appendChild(slot);
        });

        // Добавляем кнопку "Добавить урок" в конце
        // Вычисляем следующий номер урока
        let nextLessonNumber = 1;
        if (currentSchedule.length > 0) {
            nextLessonNumber = currentSchedule[currentSchedule.length - 1].lessonNumber + 1;
        }

        const addBtnContainer = document.createElement("div");
        addBtnContainer.style.marginTop = "10px";
        
        const btn = document.createElement("button");
        btn.className = "empty-slot-btn";
        btn.textContent = `+ Добавить урок ${nextLessonNumber}`;
        btn.onclick = () => openCreateModal(nextLessonNumber);
        
        addBtnContainer.appendChild(btn);
        lessonsContainer.appendChild(addBtnContainer);
    }

    // Глобальные функции
    window.toggleMenu = function(id) {
        document.querySelectorAll('.dropdown-menu').forEach(el => {
            if (el.id !== `menu-${id}`) el.classList.remove('show');
        });
        
        const menu = document.getElementById(`menu-${id}`);
        menu.classList.toggle('show');
        
        const closeMenu = (e) => {
            if (!e.target.matches('.menu-dots')) {
                menu.classList.remove('show');
                document.removeEventListener('click', closeMenu);
            }
        };
        setTimeout(() => document.addEventListener('click', closeMenu), 0);
    };

    window.openCreateModal = function(lessonNum) {
        modalTitle.textContent = "Добавить урок";
        document.getElementById("schedule-id").value = "";
        document.getElementById("lesson-number").value = lessonNum;
        form.reset();
        
        // Устанавливаем дефолтное время
        const startHour = 8 + lessonNum - 1;
        document.getElementById("startTime").value = `${String(startHour).padStart(2,'0')}:00`;
        document.getElementById("endTime").value = `${String(startHour).padStart(2,'0')}:45`;
        
        modal.style.display = "block";
    };

    window.openEditModal = function(id) {
        const lesson = currentSchedule.find(l => l.id === id);
        if (!lesson) return;
        
        modalTitle.textContent = "Изменить урок";
        document.getElementById("schedule-id").value = lesson.id;
        document.getElementById("lesson-number").value = lesson.lessonNumber;
        
        document.getElementById("discipline").value = lesson.disciplineId;
        document.getElementById("teacher").value = lesson.teacherId;
        document.getElementById("startTime").value = lesson.startTime;
        document.getElementById("endTime").value = lesson.endTime;
        document.getElementById("classroom").value = lesson.classroom;
        
        modal.style.display = "block";
    };

    window.deleteLesson = function(id) {
        if (confirm("Удалить этот урок?")) {
            fetch(`/api/${schoolName}/schedules/${id}`, { method: "DELETE" })
                .then(r => {
                    if (r.ok) loadSchedule();
                    else alert("Ошибка удаления");
                });
        }
    };

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("schedule-id").value;
        const isEdit = !!id;
        
        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        const day = String(currentDate.getDate()).padStart(2, '0');
        const dateStr = `${year}-${month}-${day}`;

        const data = {
            disciplineId: parseInt(document.getElementById("discipline").value),
            teacherId: parseInt(document.getElementById("teacher").value),
            groupId: parseInt(groupSelect.value),
            lessonNumber: parseInt(document.getElementById("lesson-number").value),
            date: dateStr,
            startTime: document.getElementById("startTime").value + (document.getElementById("startTime").value.length === 5 ? ":00" : ""),
            endTime: document.getElementById("endTime").value + (document.getElementById("endTime").value.length === 5 ? ":00" : ""),
            classroom: document.getElementById("classroom").value
        };

        const url = isEdit ? `/api/${schoolName}/schedules/${id}` : `/api/${schoolName}/schedules`;
        const method = isEdit ? "PUT" : "POST";

        fetch(url, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                modal.style.display = "none";
                loadSchedule();
            } else {
                r.json().then(err => alert("Ошибка: " + JSON.stringify(err)));
            }
        });
    });

    prevDayBtn.onclick = () => {
        currentDate.setDate(currentDate.getDate() - 1);
        updateDateDisplay();
        loadSchedule();
    };
    
    nextDayBtn.onclick = () => {
        currentDate.setDate(currentDate.getDate() + 1);
        updateDateDisplay();
        loadSchedule();
    };
    
    groupSelect.onchange = loadSchedule;
    
    closeBtn.onclick = () => modal.style.display = "none";
    window.onclick = (e) => { if (e.target == modal) modal.style.display = "none"; };
});
