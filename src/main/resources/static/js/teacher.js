document.addEventListener("DOMContentLoaded", () => {
    const curatorPanel = document.getElementById("curator-panel");
    const teacherPanel = document.getElementById("teacher-panel");
    const groupNameElement = document.getElementById("curator-group-name");
    const studentsListElement = document.getElementById("students-list");
    const createStudentBtn = document.getElementById("create-student-btn");
    const schoolName = window.location.pathname.split('/')[1];

    fetch(`/api/${schoolName}/profile/curator`)
        .then(response => {
            if (response.ok) {
                return response.json();
            } else if (response.status === 404) {
                teacherPanel.classList.add("is-visible");
                curatorPanel.classList.remove("is-visible");
                return Promise.reject("Not a curator");
            } else {
                return Promise.reject("Error fetching curator profile");
            }
        })
        .then(curatorProfile => {
            teacherPanel.classList.remove("is-visible");
            curatorPanel.classList.add("is-visible");

            groupNameElement.textContent = curatorProfile.groupName;
            createStudentBtn.href = `/${schoolName}/curator/student`; // Обновленная ссылка

            return fetch(`/api/${schoolName}/groups/${curatorProfile.groupId}/students`);
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return Promise.reject("Error fetching students");
            }
        })
        .then(students => {
            studentsListElement.innerHTML = "";
            if (students.length === 0) {
                studentsListElement.innerHTML = "<p>В вашей группе пока нет учеников.</p>";
            } else {
                const ul = document.createElement("ul");
                students.forEach(student => {
                    const li = document.createElement("li");
                    li.innerHTML = `<strong>${student.firstName} ${student.secondName}</strong> (${student.email})`;
                    
                    if (student.marks && student.marks.length > 0) {
                        const marksList = document.createElement("ul");
                        student.marks.forEach(mark => {
                            const markItem = document.createElement("li");
                            markItem.textContent = `${mark.disciplineName}: ${mark.value}`;
                            marksList.appendChild(markItem);
                        });
                        li.appendChild(marksList);
                    } else {
                        const noMarks = document.createElement("p");
                        noMarks.textContent = "Нет оценок";
                        li.appendChild(noMarks);
                    }
                    
                    ul.appendChild(li);
                });
                studentsListElement.appendChild(ul);
            }
        })
        .catch(error => {
            if (error !== "Not a curator") {
                console.error("Error:", error);
                teacherPanel.classList.add("is-visible");
                curatorPanel.classList.remove("is-visible");
            }
        });
});
