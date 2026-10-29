// Набор линейных иконок (стиль Lucide). Используется меню и элементами data-icon на страницах.
window.ICONS = {
    home: '<path d="m3 10 9-7 9 7v10a2 2 0 0 1-2 2h-4v-7H9v7H5a2 2 0 0 1-2-2z"/>',
    briefcase: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 13h18"/>',
    shield: '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/>',
    teacher: '<circle cx="9" cy="7" r="3"/><path d="M3 20v-1a5 5 0 0 1 5-5h2a5 5 0 0 1 5 5v1M15 4h6v6h-4"/>',
    student: '<path d="m2 9 10-5 10 5-10 5z"/><path d="M6 11v5c0 1.5 2.7 3 6 3s6-1.5 6-3v-5M22 9v6"/>',
    users: '<circle cx="9" cy="8" r="3.5"/><path d="M2 20v-1a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v1M16 4.5a3.5 3.5 0 0 1 0 7M22 20v-1a5 5 0 0 0-3.5-4.8"/>',
    layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 13 9 5 9-5"/>',
    book: '<path d="M4 4.5A1.5 1.5 0 0 1 5.5 3H20v15H5.5A1.5 1.5 0 0 0 4 19.5z"/><path d="M4 19.5A1.5 1.5 0 0 0 5.5 21H20"/>',
    target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
    calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>',
    tasks: '<path d="M9 5h11M9 12h11M9 19h11"/><path d="m3 5 1.5 1.5L7 4M3 12l1.5 1.5L7 11M3 19l1.5 1.5L7 18"/>',
    check: '<rect x="4" y="4" width="16" height="17" rx="2"/><path d="M9 3h6v3H9zM9 13l2 2 4-4"/>',
    sliders: '<path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0"/><circle cx="16" cy="6" r="2"/><circle cx="10" cy="12" r="2"/><circle cx="18" cy="18" r="2"/>',
    upload: '<path d="M12 15V3M7 8l5-5 5 5"/><path d="M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"/>',
    chart: '<path d="M3 3v18h18"/><path d="M7 15l4-4 3 3 6-7"/>',
    sparkles: '<path d="M12 3l1.8 4.7L18.5 9.5 13.8 11.3 12 16l-1.8-4.7L5.5 9.5l4.7-1.8z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>',
    wand: '<path d="m15 4 5 5L8 21l-5-5z"/><path d="m12 7 5 5M18 2v2M22 6h-2M20 2l-1 1"/>',
    news: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h10M7 12h10M7 16h6"/>',
    vote: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
    message: '<path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"/>',
    utensils: '<path d="M7 3v8M4 3v5a3 3 0 0 0 6 0V3M7 11v10M17 3c-2 1-3 3.5-3 7h3v11"/>',
    bell: '<path d="M6 9a6 6 0 1 1 12 0c0 6 3 8 3 8H3s3-2 3-8M10 21a2 2 0 0 0 4 0"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    logout: '<path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l-5-5 5-5M5 12h11"/>',
    menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
    graduation: '<path d="m2 9 10-5 10 5-10 5z"/><path d="M6 11v5c0 1.5 2.7 3 6 3s6-1.5 6-3v-5"/>',
    bot: '<rect x="4" y="8" width="16" height="12" rx="3"/><path d="M12 4v4M9 14h.01M15 14h.01M2 13v3M22 13v3"/>',
    star: '<path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    file: '<path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6"/>',
    plus: '<path d="M12 5v14M5 12h14"/>'
};
window.icon = (name, cls = "icon") =>
    `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true">${window.ICONS[name] || ""}</svg>`;

// Подставляет иконки в элементы <span data-icon="имя">
window.renderIcons = (root = document) =>
    root.querySelectorAll("[data-icon]:not([data-icon-done])").forEach(el => {
        el.innerHTML = window.icon(el.dataset.icon);
        el.setAttribute("data-icon-done", "");
    });

// Строит sidebar динамически по роли и schoolName
document.addEventListener("DOMContentLoaded", () => {
    const sn = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";
    const path = window.location.pathname;
    const icon = window.icon;

    window.renderIcons();

    const roleLabels = {
        ADMIN: "Администратор", DIRECTOR: "Директор",
        TEACHER: "Учитель", STUDENT: "Ученик", PARENT: "Родитель"
    };

    const badge = document.querySelector(".role-badge");
    if (badge && roleLabels[role]) badge.textContent = roleLabels[role];

    const brandTitle = document.querySelector(".sidebar-brand h1");
    if (brandTitle && !brandTitle.querySelector(".brand-mark")) {
        brandTitle.insertAdjacentHTML("afterbegin", `<span class="brand-mark">${icon("book")}</span>`);
    }

    const nav = document.getElementById("sidebar-nav");
    if (!nav || !sn) return;

    // Один пункт меню; активным считается самый длинный совпавший путь
    const items = [];
    const link = (href, iconName, label, extra = "") => {
        items.push(href);
        return `<a href="/${sn}${href}" data-path="${href}" ${extra}>${icon(iconName)}<span>${label}</span></a>`;
    };
    const section = (label) => `<span class="nav-section">${label}</span>`;
    const notifLink = link("/notifications", "bell", "Уведомления", 'id="notif-nav-link"');
    const profileLink = link("/profile", "user", "Профиль");

    const navMap = {
        ADMIN: () => [
            section("Управление"),
            link("/admin", "home", "Главная"),
            link("/admin/director", "briefcase", "Директора"),
            link("/admin/admin", "shield", "Администраторы"),
            link("/admin/teacher", "teacher", "Учителя"),
            link("/admin/student", "student", "Ученики"),
            link("/admin/parent", "users", "Родители"),
            section("Учебный процесс"),
            link("/admin/group", "layers", "Группы/Классы"),
            link("/admin/discipline", "book", "Предметы"),
            link("/admin/course", "target", "Курсы"),
            link("/admin/schedule/view", "calendar", "Расписание", 'data-also="/admin/schedule"'),
            link("/exams", "tasks", "Задания"),
            link("/admin/attendance", "check", "Посещаемость"),
            link("/admin/grading", "sliders", "Оценивание"),
            link("/import-students", "upload", "Импорт Excel"),
            section("AI"),
            link("/ai-analysis", "sparkles", "AI Анализ"),
            link("/test-data", "wand", "AI Генератор"),
            section("Контент"),
            link("/admin/news", "news", "Новости"),
            link("/admin/poll", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            link("/canteen", "utensils", "Столовая"),
            notifLink,
            profileLink
        ],
        MINISTRY: () => [
            section("Главное"),
            link("/polls", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            notifLink
        ],
        DIRECTOR: () => [
            section("Главное"),
            link("/director", "home", "Панель"),
            link("/news", "news", "Новости"),
            link("/schedule", "calendar", "Расписание"),
            link("/exams", "tasks", "Задания"),
            link("/attendance", "check", "Посещаемость"),
            link("/admin/grading", "sliders", "Оценивание"),
            link("/polls", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            link("/canteen", "utensils", "Столовая"),
            notifLink,
            section("Прочее"),
            profileLink
        ],
        TEACHER: () => [
            section("Главное"),
            link("/teacher", "home", "Панель"),
            link("/news", "news", "Новости"),
            link("/schedule", "calendar", "Расписание"),
            link("/courses", "target", "Мои курсы"),
            link("/teacher/marks", "star", "Журнал оценок"),
            link("/exams", "tasks", "Задания"),
            link("/attendance", "check", "Посещаемость"),
            link("/curator/students-list", "users", "Мой класс", 'id="curator-nav-link" style="display:none;" data-also="/curator/"'),
            section("Школа"),
            link("/polls", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            link("/canteen", "utensils", "Столовая"),
            notifLink,
            section("Прочее"),
            profileLink
        ],
        STUDENT: () => [
            section("Главное"),
            link("/student", "home", "Панель"),
            link("/schedule", "calendar", "Расписание"),
            link("/grades", "star", "Мои оценки"),
            link("/exams", "tasks", "Задания"),
            link("/attendance", "check", "Посещаемость"),
            link("/courses", "target", "Курсы"),
            section("Школа"),
            link("/news", "news", "Новости"),
            link("/polls", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            link("/canteen", "utensils", "Столовая"),
            notifLink,
            section("Прочее"),
            profileLink
        ],
        PARENT: () => [
            section("Главное"),
            link("/parent", "home", "Панель"),
            link("/exams", "tasks", "Задания"),
            link("/attendance", "check", "Посещаемость"),
            section("Школа"),
            link("/news", "news", "Новости"),
            link("/polls", "vote", "Голосования"),
            link("/complaints", "message", "Жалобы"),
            link("/canteen", "utensils", "Столовая"),
            notifLink,
            section("Прочее"),
            profileLink
        ]
    };

    nav.innerHTML = (navMap[role] ? navMap[role]() : []).join("");

    // Активный пункт: самое длинное совпадение пути (иначе «/admin» подсвечивался бы на всех страницах админа)
    const current = path.slice(sn.length + 1) || "/";
    let best = null, bestLen = -1;
    nav.querySelectorAll("a[data-path]").forEach(a => {
        for (const p of [a.dataset.path, a.dataset.also].filter(Boolean)) {
            const hit = current === p || current.startsWith(p.endsWith("/") ? p : p + "/");
            if (hit && p.length > bestLen) { best = a; bestLen = p.length; }
        }
    });
    if (best) best.classList.add("active");

    // Счётчик непрочитанных уведомлений
    const token = sessionStorage.getItem("jwtToken");
    if (token) {
        apiFetch(`/api/${sn}/notifications/unread-count`)
            .then(r => r.ok ? r.json() : { count: 0 })
            .then(data => {
                const notif = document.getElementById("notif-nav-link");
                if (data.count > 0 && notif) {
                    notif.insertAdjacentHTML("beforeend", `<span class="nav-count" data-no-i18n>${data.count > 99 ? "99+" : data.count}</span>`);
                }
            }).catch(() => {});
    }

    if (role === "TEACHER") {
        apiFetch(`/api/${sn}/profile/curator`)
            .then(r => r.status === 200 ? r.json() : null)
            .then(p => {
                const curator = document.getElementById("curator-nav-link");
                if (p && curator) curator.style.display = "flex";
            }).catch(() => {});
    }

    // Пользователь внизу меню
    const footer = document.querySelector(".sidebar-footer");
    if (footer && token) {
        apiFetch(`/api/${sn}/profile/me`)
            .then(r => r.ok ? r.json() : null)
            .then(me => {
                if (!me) return;
                const name = [me.firstName, me.secondName].filter(Boolean).join(" ");
                const initials = ((me.firstName || "")[0] || "") + ((me.secondName || "")[0] || "");
                const userEl = document.createElement("a");
                userEl.className = "sidebar-user";
                userEl.href = `/${sn}/profile`;
                userEl.setAttribute("data-no-i18n", "");
                userEl.innerHTML = `<span class="avatar"></span><div><div class="user-name"></div><div class="user-email"></div></div>`;
                userEl.querySelector(".avatar").textContent = initials.toUpperCase() || "?";
                userEl.querySelector(".user-name").textContent = name;
                userEl.querySelector(".user-email").textContent = me.email || "";
                footer.prepend(userEl);
            }).catch(() => {});
    }

    const logoutBtn = document.getElementById("logout-btn");
    if (logoutBtn) {
        logoutBtn.innerHTML = `${icon("logout")}<span>Выйти</span>`;
        logoutBtn.addEventListener("click", () => {
            sessionStorage.removeItem("jwtToken");
            sessionStorage.removeItem("schoolName");
            sessionStorage.removeItem("userRole");
            window.location.href = "/login";
        });
    }

    // ── Мобильное меню ──
    const sidebar = document.querySelector(".sidebar");
    if (!sidebar) return;

    const mobileHeader = document.createElement("div");
    mobileHeader.className = "mobile-header";
    mobileHeader.innerHTML = `
        <button class="hamburger" id="hamburger-btn" aria-label="Меню">${icon("menu")}</button>
        <span class="mobile-title"><span class="brand-mark">${icon("book")}</span>Electronic Diary</span>`;
    document.body.prepend(mobileHeader);

    const overlay = document.createElement("div");
    overlay.className = "sidebar-overlay";
    document.body.appendChild(overlay);

    function openSidebar()  { sidebar.classList.add("open"); overlay.classList.add("open"); document.body.style.overflow = "hidden"; }
    function closeSidebar() { sidebar.classList.remove("open"); overlay.classList.remove("open"); document.body.style.overflow = ""; }

    document.getElementById("hamburger-btn").addEventListener("click", openSidebar);
    overlay.addEventListener("click", closeSidebar);
    nav.addEventListener("click", e => { if (e.target.closest("a") && window.innerWidth <= 860) closeSidebar(); });

    // ── AI чат ──
    const aiScript = document.createElement("script");
    aiScript.src = "/js/ai-chat.js";
    document.body.appendChild(aiScript);
});
