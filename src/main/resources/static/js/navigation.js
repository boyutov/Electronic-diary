document.addEventListener("DOMContentLoaded", () => {
    const pathParts = window.location.pathname.split('/');
    const schoolName = pathParts[1];

    // Игнорируем страницы, где нет контекста школы (например, главная, логин)
    if (!schoolName || ["login", "activate", "css", "js", "api", ""].includes(schoolName)) {
        return;
    }

    // 1. Исправляем ВСЕ внутренние ссылки на странице
    document.querySelectorAll('a[href^="/"]').forEach(link => {
        const href = link.getAttribute("href");
        
        // Исключаем ссылки, которые уже исправлены или являются системными
        if (href.startsWith(`/${schoolName}/`) || href === "/logout" || href === "/") {
            return;
        }

        // Обновляем ссылку, добавляя префикс школы
        link.setAttribute("href", `/${schoolName}${href}`);
    });

    // 2. Добавляем кнопку "Личный кабинет" в навигацию
    const nav = document.querySelector("nav");
    if (nav && !document.getElementById("nav-profile")) {
        const profileLink = document.createElement("a");
        profileLink.id = "nav-profile";
        profileLink.href = `/${schoolName}/profile`;
        profileLink.textContent = "Личный кабинет";
        profileLink.style.fontWeight = "bold";
        profileLink.style.marginLeft = "16px";
        
        const logoutForm = nav.querySelector("form[action='/logout']");
        if (logoutForm) {
            nav.insertBefore(profileLink, logoutForm);
        } else {
            nav.appendChild(profileLink);
        }
    }
});
