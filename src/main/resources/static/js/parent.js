document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    if (!schoolName || schoolName === "parent") return;

    const container = document.getElementById("children-container");

    apiFetch(`/api/${schoolName}/parents/my/children`)
        .then(r => r.ok ? r.json() : [])
        .then(children => {
            container.innerHTML = "";

            if (!children.length) {
                document.getElementById("parent-info").textContent = "Дети не привязаны к вашему аккаунту";
                container.innerHTML = "<div class='card'><p style='color:#64748b;'>Обратитесь к администратору для привязки детей.</p></div>";
                return;
            }

            document.getElementById("parent-greeting").textContent = "Добро пожаловать!";
            document.getElementById("parent-info").textContent =
                `Детей: ${children.length}`;

            children.forEach(child => {
                const marks = child.marks || [];
                const allVals = marks.map(m => m.value);
                const totalAvg = allVals.length
                    ? (allVals.reduce((a, b) => a + b, 0) / allVals.length).toFixed(1)
                    : null;

                // Группируем оценки по предмету
                const grouped = {};
                marks.forEach(m => {
                    if (!grouped[m.disciplineName]) grouped[m.disciplineName] = [];
                    grouped[m.disciplineName].push(m);
                });

                // Строим аккордеоны оценок
                let marksHtml = "";
                if (!marks.length) {
                    marksHtml = "<p style='color:#64748b;font-size:0.9em;margin-top:12px;'>Оценок пока нет.</p>";
                } else {
                    marksHtml = Object.entries(grouped).map(([discipline, dMarks]) => {
                        dMarks.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                        const avg = (dMarks.reduce((s, m) => s + m.value, 0) / dMarks.length).toFixed(1);
                        const avgClass = avg >= 80 ? "green" : avg >= 60 ? "yellow" : "red";

                        const chipsHtml = dMarks.map(m => {
                            const date = m.createdAt
                                ? new Date(m.createdAt).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" })
                                : "";
                            const valClass = m.value >= 80 ? "high" : m.value >= 60 ? "mid" : "low";
                            return `<div class="mark-chip">
                                <span class="chip-val ${valClass}">${m.value}</span>
                                <span class="chip-date">${date}</span>
                                ${m.comment ? `<span class="chip-comment">${m.comment}</span>` : ""}
                            </div>`;
                        }).join("");

                        return `<div class="accordion-item" style="margin-top:8px;">
                            <div class="accordion-header">
                                <span class="acc-title">${discipline}</span>
                                <span class="acc-meta">
                                    <span class="acc-avg ${avgClass}">★ ${avg}</span>
                                    <span style="color:#94a3b8;font-size:0.82em;">${dMarks.length} оц.</span>
                                    <span class="accordion-arrow">▼</span>
                                </span>
                            </div>
                            <div class="accordion-body">
                                <div class="marks-row">${chipsHtml}</div>
                            </div>
                        </div>`;
                    }).join("");
                }

                const card = document.createElement("div");
                card.className = "card";
                card.style.marginBottom = "24px";
                card.innerHTML = `
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
                        <div>
                            <h3 style="margin:0;">${child.secondName} ${child.firstName}${child.thirdName ? " " + child.thirdName : ""}</h3>
                            <p style="margin-top:4px;color:#64748b;font-size:0.9em;">
                                ${child.email} · Возраст: ${child.age} · Класс: ${child.groupName || "—"}
                            </p>
                        </div>
                        <div style="text-align:right;">
                            ${totalAvg
                                ? `<span style="font-size:1.4em;font-weight:700;color:${totalAvg >= 80 ? '#15803d' : totalAvg >= 60 ? '#854d0e' : '#b91c1c'};">${totalAvg}</span>
                                   <div style="font-size:0.75em;color:#94a3b8;">средний балл</div>`
                                : `<span class="badge gray">Нет оценок</span>`}
                        </div>
                    </div>
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
                        <h4 style="font-size:0.88em;color:#64748b;text-transform:uppercase;letter-spacing:0.05em;margin:0;">📊 Оценки</h4>
                    </div>
                    ${marksHtml}
                `;

                // Вешаем обработчики на аккордеоны
                card.querySelectorAll(".accordion-header").forEach(header => {
                    header.addEventListener("click", () => header.closest(".accordion-item").classList.toggle("open"));
                });

                container.appendChild(card);
            });
        })
        .catch(() => {
            container.innerHTML = "<div class='card'><p style='color:#ef4444;'>Ошибка загрузки данных.</p></div>";
        });
});
