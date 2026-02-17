document.addEventListener("DOMContentLoaded", () => {
    const startDateInput = document.getElementById("start-date");
    const endDateInput = document.getElementById("end-date");
    const loadButton = document.getElementById("load-schedule");
    const resultsContainer = document.getElementById("schedule-results");
    const prevWeekBtn = document.getElementById("prev-week");
    const nextWeekBtn = document.getElementById("next-week");
    const currentPeriodLabel = document.getElementById("current-period");
    const manualSelector = document.getElementById("manual-date-selector");
    const toggleManualBtn = document.getElementById("toggle-manual");

    const schoolName = window.location.pathname.split('/')[1];

    let currentStartDate = new Date();
    let currentEndDate = new Date();

    // Инициализация: устанавливаем текущую неделю (Пн-Вс)
    function initCurrentWeek() {
        const today = new Date();
        const day = today.getDay(); // 0 (Вс) - 6 (Сб)

        // Вычисляем разницу до понедельника
        // Если сегодня Вс (0), то diff = -6. Если Пн (1), diff = 0.
        const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);

        currentStartDate = new Date(today.setDate(diffToMonday));
        currentEndDate = new Date(today.setDate(currentStartDate.getDate() + 6));

        updateInputs();
        loadSchedule();
    }

    function updateInputs() {
        // Форматируем даты для input type="date" (YYYY-MM-DD)
        startDateInput.valueAsDate = new Date(Date.UTC(currentStartDate.getFullYear(), currentStartDate.getMonth(), currentStartDate.getDate()));
        endDateInput.valueAsDate = new Date(Date.UTC(currentEndDate.getFullYear(), currentEndDate.getMonth(), currentEndDate.getDate()));

        // Обновляем заголовок периода
        const options = { day: 'numeric', month: 'long' };
        currentPeriodLabel.textContent = `${currentStartDate.toLocaleDateString('ru-RU', options)} — ${currentEndDate.toLocaleDateString('ru-RU', options)}`;
    }

    function loadSchedule() {
        const start = startDateInput.value;
        const end = endDateInput.value;

        resultsContainer.innerHTML = "<p>Загрузка...</p>";

        fetch(`/api/${schoolName}/schedules/my/period?start=${start}&end=${end}`)
            .then(response => response.json())
            .then(schedule => {
                resultsContainer.innerHTML = "";
                if (schedule.length === 0) {
                    resultsContainer.innerHTML = "<p style='text-align: center; color: #64748b;'>На эту неделю расписания нет.</p>";
                    return;
                }

                // Группируем по датам
                const grouped = {};
                schedule.forEach(item => {
                    if (!grouped[item.date]) {
                        grouped[item.date] = [];
                    }
                    grouped[item.date].push(item);
                });

                // Сортируем даты
                const sortedDates = Object.keys(grouped).sort();

                sortedDates.forEach(date => {
                    const dateObj = new Date(date);
                    const dateHeader = document.createElement("h3");
                    dateHeader.style.marginTop = "20px";
                    dateHeader.style.borderBottom = "1px solid #e2e8f0";
                    dateHeader.style.paddingBottom = "8px";
                    dateHeader.textContent = dateObj.toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });
                    // Делаем первую букву заглавной
                    dateHeader.textContent = dateHeader.textContent.charAt(0).toUpperCase() + dateHeader.textContent.slice(1);

                    resultsContainer.appendChild(dateHeader);

                    const grid = document.createElement("div");
                    grid.className = "schedule-grid";

                    // Сортируем уроки внутри дня
                    grouped[date].sort((a, b) => a.startTime.localeCompare(b.startTime));

                    grouped[date].forEach(item => {
                        const div = document.createElement("div");
                        div.className = "schedule-item";
                        div.innerHTML = `
                            <div class="schedule-time">${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)}</div>
                            <div class="schedule-subject">${item.disciplineName}</div>
                            <div class="schedule-details">
                                Каб. ${item.classroom || "—"}<br>
                                ${item.teacherName || item.groupName}
                            </div>
                        `;
                        grid.appendChild(div);
                    });
                    resultsContainer.appendChild(grid);
                });
            })
            .catch(error => {
                console.error("Error:", error);
                resultsContainer.innerHTML = "<p>Ошибка загрузки.</p>";
            });
    }

    // Обработчики событий
    prevWeekBtn.addEventListener("click", () => {
        currentStartDate.setDate(currentStartDate.getDate() - 7);
        currentEndDate.setDate(currentEndDate.getDate() - 7);
        updateInputs();
        loadSchedule();
    });

    nextWeekBtn.addEventListener("click", () => {
        currentStartDate.setDate(currentStartDate.getDate() + 7);
        currentEndDate.setDate(currentEndDate.getDate() + 7);
        updateInputs();
        loadSchedule();
    });

    loadButton.addEventListener("click", loadSchedule);

    toggleManualBtn.addEventListener("click", () => {
        if (manualSelector.style.display === "none") {
            manualSelector.style.display = "grid";
            toggleManualBtn.textContent = "Скрыть выбор дат";
        } else {
            manualSelector.style.display = "none";
            toggleManualBtn.textContent = "Выбрать произвольный период";
        }
    });

    // Запуск
    initCurrentWeek();
});
