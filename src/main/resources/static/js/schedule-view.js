document.addEventListener("DOMContentLoaded", () => {
    const resultsContainer = document.getElementById("schedule-results");
    const prevWeekBtn = document.getElementById("prev-week");
    const nextWeekBtn = document.getElementById("next-week");
    const currentPeriodLabel = document.getElementById("current-period");
    const schoolName = window.location.pathname.split('/')[1];

    let allSchedule = [];
    let weekOffset = 0;

    function getWeekDates(offset) {
        const today = new Date();
        const day = today.getDay();
        const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);
        const monday = new Date(today);
        monday.setDate(diffToMonday + offset * 7);
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        return { start: monday, end: sunday };
    }

    function toISODate(d) {
        return d.toISOString().split('T')[0];
    }

    function updatePeriodLabel() {
        const { start, end } = getWeekDates(weekOffset);
        const opts = { day: 'numeric', month: 'long' };
        const label = `${start.toLocaleDateString('ru-RU', opts)} — ${end.toLocaleDateString('ru-RU', opts)}`;
        if (currentPeriodLabel) currentPeriodLabel.textContent = label;
    }

    function renderSchedule() {
        const { start, end } = getWeekDates(weekOffset);
        const startStr = toISODate(start);
        const endStr = toISODate(end);

        const filtered = allSchedule.filter(item => item.date >= startStr && item.date <= endStr);

        resultsContainer.innerHTML = "";

        if (filtered.length === 0) {
            resultsContainer.innerHTML = "<p style='text-align:center;color:#64748b;margin-top:20px;'>На эту неделю уроков нет.</p>";
            return;
        }

        const grouped = {};
        filtered.forEach(item => {
            if (!grouped[item.date]) grouped[item.date] = [];
            grouped[item.date].push(item);
        });

        Object.keys(grouped).sort().forEach(date => {
            const dateObj = new Date(date + "T00:00:00");
            const h3 = document.createElement("h3");
            h3.style.cssText = "margin-top:20px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;";
            let label = dateObj.toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });
            h3.textContent = label.charAt(0).toUpperCase() + label.slice(1);
            resultsContainer.appendChild(h3);

            const grid = document.createElement("div");
            grid.className = "schedule-grid";

            grouped[date].sort((a, b) => a.startTime.localeCompare(b.startTime)).forEach(item => {
                const div = document.createElement("div");
                div.className = "schedule-item";
                div.innerHTML = `
                    <div class="schedule-time">${item.startTime.slice(0,5)} - ${item.endTime.slice(0,5)}</div>
                    <div class="schedule-subject">${item.disciplineName}</div>
                    <div class="schedule-details">
                        Каб. ${item.classroom || "—"}<br>
                        ${item.teacherName || item.groupName || ""}
                    </div>
                `;
                grid.appendChild(div);
            });
            resultsContainer.appendChild(grid);
        });
    }

    function loadAll() {
        resultsContainer.innerHTML = "<p style='color:#64748b;'>Загрузка...</p>";
        apiFetch(`/api/${schoolName}/schedules/my`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                allSchedule = data;
                updatePeriodLabel();
                renderSchedule();
            })
            .catch(err => {
                resultsContainer.innerHTML = `<p style='color:red;'>Ошибка загрузки: ${err.message}</p>`;
            });
    }

    if (prevWeekBtn) {
        prevWeekBtn.addEventListener("click", () => {
            weekOffset--;
            updatePeriodLabel();
            renderSchedule();
        });
    }

    if (nextWeekBtn) {
        nextWeekBtn.addEventListener("click", () => {
            weekOffset++;
            updatePeriodLabel();
            renderSchedule();
        });
    }

    loadAll();
});
