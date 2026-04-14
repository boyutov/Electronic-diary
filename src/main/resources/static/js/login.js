document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");

    if (loginForm) {
        loginForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const schoolName = document.getElementById("schoolName").value;
            const email = document.getElementById("username").value;
            const password = document.getElementById("password").value;

            fetch("/api/auth/authenticate", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    schoolName: schoolName,
                    email: email,
                    password: password
                })
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error("Неверный email, пароль или название школы");
                }
                return response.json();
            })
            .then(data => {
                // Сохраняем токен в sessionStorage (уникален для каждой вкладки!)
                sessionStorage.setItem("jwtToken", data.token);
                sessionStorage.setItem("schoolName", data.schoolName);
                sessionStorage.setItem("userRole", data.role);

                // Перенаправляем на нужную страницу в зависимости от роли
                const role = data.role;
                let redirectUrl = `/${data.schoolName}`;

                if (role === "ADMIN") redirectUrl += "/admin";
                else if (role === "DIRECTOR") redirectUrl += "/director";
                else if (role === "TEACHER") redirectUrl += "/teacher";
                else if (role === "PARENT") redirectUrl += "/parent";
                else if (role === "STUDENT") redirectUrl += "/student";

                window.location.href = redirectUrl;
            })
            .catch(error => {
                const errEl = document.getElementById('error-msg');
                if (errEl) {
                    errEl.textContent = error.message;
                    errEl.style.display = 'block';
                } else {
                    alert(error.message);
                }
            });
        });
    }
});
