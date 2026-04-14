document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const form = document.getElementById("student-form");
    const successMsg = document.getElementById("success-msg");
    const errorMsg = document.getElementById("error-msg");

    form.addEventListener("submit", e => {
        e.preventDefault();
        successMsg.style.display = "none";
        errorMsg.style.display = "none";

        // First get curator's group
        apiFetch(`/api/${schoolName}/profile/curator`)
            .then(r => r.ok ? r.json() : Promise.reject("Вы не являетесь куратором"))
            .then(curatorProfile => {
                const data = {
                    firstName: document.getElementById("firstName").value,
                    secondName: document.getElementById("secondName").value,
                    thirdName: document.getElementById("thirdName").value,
                    email: document.getElementById("email").value,
                    password: document.getElementById("password").value,
                    age: parseInt(document.getElementById("age").value),
                    groupId: curatorProfile.groupId
                };

                return apiFetch(`/api/${schoolName}/students`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(data)
                });
            })
            .then(r => {
                if (r.ok) {
                    form.reset();
                    successMsg.textContent = "Ученик успешно создан!";
                    successMsg.style.display = "block";
                } else {
                    return r.json().then(err => Promise.reject(JSON.stringify(err)));
                }
            })
            .catch(err => {
                errorMsg.textContent = "Ошибка: " + (typeof err === "string" ? err : "Не удалось создать ученика");
                errorMsg.style.display = "block";
            });
    });
});
