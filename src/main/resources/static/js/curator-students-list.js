document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const groupNameDisplay = document.getElementById("group-name-display");
    const studentsContainer = document.getElementById("students-container");
    const createStudentBtn = document.getElementById("create-student-btn");
    const marksModal = document.getElementById("marks-modal");
    const marksModalTitle = document.getElementById("marks-modal-title");
    const marksModalBody = document.getElementById("marks-modal-body");

    createStudentBtn.href = `/${schoolName}/curator/student`;

    document.getElementById("marks-modal-close").onclick = () => marksModal.classList.remove("open");
    marksModal.addEventListener("click", e => { if (e.target === marksModal) marksModal.classList.remove("open"); });

    apiFetch(`/api/${schoolName}/profile/curator`)
        .then(res => res.ok ? res.json() : Promise.reject("no curator"))
        .then(profile => {
            groupNameDisplay.textContent = profile.groupName;
            return apiFetch(`/api/${schoolName}/groups/${profile.groupId}/students`);
        })
        .then(res => res.ok ? res.json() : [])
        .then(students => {
            if (!students.length) {
                studentsContainer.innerHTML = "<p style='color:#64748b;'>В вашей группе пока нет учеников.</p>";
                return;
            }

            let html = `<div class="table-wrap"><table>
                <thead><tr>
                    <th>#</th>
                    <th>ФИО</th>
                    <th>Email</th>
                    <th class="col-center">Возраст</th>
                    <th class="col-center">Средний балл</th>
                    <th class="col-center">Оценки</th>
                </tr></thead>
                <tbody>`;

            students.forEach((s, i) => {
                const marks = s.marks || [];
                const avg = marks.length
                    ? (marks.reduce((a, m) => a + m.value, 0) / marks.length).toFixed(1)
                    : null;
                const avgColor = !avg ? "#94a3b8" : avg >= 80 ? "#15803d" : avg >= 60 ? "#854d0e" : "#b91c1c";
                const avgBg   = !avg ? "#f1f5f9" : avg >= 80 ? "#dcfce7" : avg >= 60 ? "#fef9c3" : "#fee2e2";

                html += `<tr>
                    <td style="color:#94a3b8;">${i + 1}</td>
                    <td><strong>${s.secondName} ${s.firstName}${s.thirdName ? " " + s.thirdName : ""}</strong></td>
                    <td style="color:#64748b;font-size:0.88em;">${s.email}</td>
                    <td class="col-center">${s.age}</td>
                    <td class="col-center">
                        ${avg
                            ? `<span style="font-weight:700;color:${avgColor};background:${avgBg};padding:3px 10px;border-radius:999px;font-size:0.88em;">${avg}</span>`
                            : `<span style="color:#94a3b8;font-size:0.88em;">—</span>`}
                    </td>
                    <td class="col-center">
                        <button class="button ghost sm" data-id="${s.id}" data-name="${s.firstName} ${s.secondName}">
                            Посмотреть
                        </button>
                    </td>
                </tr>`;
            });

            html += `</tbody></table></div>`;
            studentsContainer.innerHTML = html;

            // Кнопки оценок
            studentsContainer.querySelectorAll("button[data-id]").forEach(btn => {
                btn.addEventListener("click", () => {
                    const sid = btn.dataset.id;
                    const name = btn.dataset.name;
                    openMarksModal(sid, name, students);
                });
            });
        })
        .catch(() => {
            studentsContainer.innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки. Убедитесь, что вы являетесь куратором.</p>";
        });

    function openMarksModal(studentId, studentName, students) {
        const student = students.find(s => String(s.id) === String(studentId));
        marksModalTitle.textContent = `Оценки — ${studentName}`;
        marksModalBody.innerHTML = "";

        const marks = student?.marks || [];
        if (!marks.length) {
            marksModalBody.innerHTML = "<p style='color:#64748b;'>Оценок пока нет.</p>";
            marksModal.classList.add("open");
            return;
        }

        const grouped = {};
        marks.forEach(m => {
            if (!grouped[m.disciplineName]) grouped[m.disciplineName] = [];
            grouped[m.disciplineName].push(m);
        });

        Object.entries(grouped).forEach(([discipline, dMarks]) => {
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

            const item = document.createElement("div");
            item.className = "accordion-item open";
            item.innerHTML = `
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
            `;
            item.querySelector(".accordion-header").addEventListener("click", () => item.classList.toggle("open"));
            marksModalBody.appendChild(item);
        });

        marksModal.classList.add("open");
    }
});
