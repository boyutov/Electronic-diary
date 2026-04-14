document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const successMsg = document.getElementById("success-msg");
    const errorMsg = document.getElementById("error-msg");

    const roleRoutes = {
        ADMIN: "/admin",
        DIRECTOR: "/director",
        TEACHER: "/teacher",
        STUDENT: "/student",
        PARENT: "/parent"
    };

    apiFetch(`/api/${schoolName}/profile/me`)
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(user => {
            document.getElementById("firstName").value = user.firstName || "";
            document.getElementById("secondName").value = user.secondName || "";
            document.getElementById("thirdName").value = user.thirdName || "";
            document.getElementById("email").value = user.email || "";

            // Sidebar role badge
            const badge = document.getElementById("sidebar-role");
            if (badge) badge.textContent = user.role || "Профиль";

            // Dashboard link
            const dashLink = document.getElementById("nav-dashboard");
            if (dashLink && user.role && roleRoutes[user.role]) {
                dashLink.href = `/${schoolName}${roleRoutes[user.role]}`;
            }

            const phoneContainer = document.getElementById("phone-container");
            const bioContainer = document.getElementById("bio-container");
            const officeContainer = document.getElementById("office-container");

            if (user.role === "TEACHER") {
                phoneContainer.style.display = "block";
                bioContainer.style.display = "block";
                officeContainer.style.display = "block";
                document.getElementById("phone").value = user.phone || "";
                document.getElementById("bio").value = user.bio || "";
                document.getElementById("office").value = user.office || "";
            } else if (user.role === "STUDENT" || user.role === "PARENT") {
                phoneContainer.style.display = "block";
                document.getElementById("phone").value = user.phone || "";
            }
        })
        .catch(() => {
            if (errorMsg) { errorMsg.textContent = "Ошибка загрузки профиля."; errorMsg.style.display = "block"; }
        });

    document.getElementById("profile-form").addEventListener("submit", e => {
        e.preventDefault();
        successMsg.style.display = "none";
        errorMsg.style.display = "none";

        const data = {
            firstName: document.getElementById("firstName").value,
            secondName: document.getElementById("secondName").value,
            thirdName: document.getElementById("thirdName").value,
            email: document.getElementById("email").value,
            newPassword: document.getElementById("newPassword").value || null,
            phone: document.getElementById("phone") ? document.getElementById("phone").value : null,
            bio: document.getElementById("bio") ? document.getElementById("bio").value : null,
            office: document.getElementById("office") ? document.getElementById("office").value : null
        };

        apiFetch(`/api/${schoolName}/profile/me`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) {
                successMsg.textContent = "Профиль успешно обновлён!";
                successMsg.style.display = "block";
                document.getElementById("newPassword").value = "";
            } else {
                r.json().then(err => {
                    errorMsg.textContent = "Ошибка: " + (err.message || JSON.stringify(err));
                    errorMsg.style.display = "block";
                });
            }
        });
    });
});
