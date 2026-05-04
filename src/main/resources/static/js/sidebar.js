// Строит sidebar динамически по роли и schoolName
document.addEventListener("DOMContentLoaded", () => {
    const sn = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";
    const path = window.location.pathname;

    function active(href) {
        return path.includes(href) ? ' class="active"' : '';
    }

    const roleLabels = {
        ADMIN: "Администратор", DIRECTOR: "Директор",
        TEACHER: "Учитель", STUDENT: "Ученик", PARENT: "Родитель"
    };

    const badge = document.querySelector(".role-badge");
    if (badge && roleLabels[role]) badge.textContent = roleLabels[role];

    const nav = document.getElementById("sidebar-nav");
    if (!nav || !sn) return;

    const notifLink = `<a href="/${sn}/notifications"${active('/notifications')} id="notif-nav-link">🔔 Уведомления</a>`;

    const navMap = {
        ADMIN: `
            <span class="nav-section">Управление</span>
            <a href="/${sn}/admin"${active('/admin') && !path.includes('/admin/') ? ' class="active"' : ''}>🏠 Главная</a>
            <a href="/${sn}/admin/director"${active('/admin/director')}>👔 Директора</a>
            <a href="/${sn}/admin/admin"${active('/admin/admin')}>🛡 Администраторы</a>
            <a href="/${sn}/admin/teacher"${active('/admin/teacher')}>👨🏫 Учителя</a>
            <a href="/${sn}/admin/parent"${active('/admin/parent')}>👨👩👧 Родители</a>
            <span class="nav-section">Учебный процесс</span>
            <a href="/${sn}/admin/group"${active('/admin/group')}>📚 Группы/Классы</a>
            <a href="/${sn}/admin/discipline"${active('/admin/discipline')}>📖 Предметы</a>
            <a href="/${sn}/admin/course"${active('/admin/course')}>🎯 Курсы</a>
            <a href="/${sn}/admin/schedule/view"${active('/admin/schedule')}>📅 Расписание</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/admin/attendance"${active('/admin/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/admin/grading"${active('/admin/grading')}>⚙️ Оценивание</a>
            <span class="nav-section">Контент</span>
            <a href="/${sn}/admin/news"${active('/admin/news')}>📰 Новости</a>
            <a href="/${sn}/admin/poll"${active('/admin/poll')}>📊 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            ${notifLink}`,
        MINISTRY: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            ${notifLink}
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        DIRECTOR: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/director"${active('/director')}>🏠 Панель</a>
            <a href="/${sn}/news"${active('/news')}>📰 Новости</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/admin/grading"${active('/admin/grading')}>⚙️ Оценивание</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            ${notifLink}
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        TEACHER: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/teacher"${active('/teacher') && !path.includes('/curator') ? ' class="active"' : ''}>🏠 Панель</a>
            <a href="/${sn}/news"${active('/news')}>📰 Новости</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/courses"${active('/courses')}>🎯 Мои курсы</a>
            <a href="/${sn}/teacher/marks"${active('/teacher/marks')}>📊 Журнал оценок</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            <a href="/${sn}/curator/students-list" id="curator-nav-link" style="display:none;">👥 Мой класс</a>
            ${notifLink}
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        STUDENT: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/student"${active('/student')}>🏠 Панель</a>
            <a href="/${sn}/news"${active('/news')}>📰 Новости</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/grades"${active('/grades')}>📊 Мои оценки</a>
            <a href="/${sn}/courses"${active('/courses')}>🎯 Курсы</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            ${notifLink}
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        PARENT: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/parent"${active('/parent')}>🏠 Панель</a>
            <a href="/${sn}/news"${active('/news')}>📰 Новости</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            ${notifLink}
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
    };

    nav.innerHTML = navMap[role] || "";

    // Счётчик непрочитанных уведомлений
    const token = sessionStorage.getItem("jwtToken");
    if (token) {
        apiFetch(`/api/${sn}/notifications/unread-count`)
            .then(r => r.ok ? r.json() : { count: 0 })
            .then(data => {
                if (data.count > 0) {
                    const link = document.getElementById("notif-nav-link");
                    if (link) link.innerHTML =
                        `🔔 Уведомления <span style="background:var(--danger);color:#fff;border-radius:999px;padding:1px 7px;font-size:0.75em;margin-left:4px;">${data.count}</span>`;
                }
            }).catch(() => {});
    }

    if (role === "TEACHER") {
        apiFetch(`/api/${sn}/profile/curator`)
            .then(r => r.ok ? r.json() : null)
            .then(p => {
                const link = document.getElementById("curator-nav-link");
                if (p && link) link.style.display = "flex";
            }).catch(() => {});
    }

    const logoutBtn = document.getElementById("logout-btn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", () => {
            sessionStorage.removeItem("jwtToken");
            sessionStorage.removeItem("schoolName");
            sessionStorage.removeItem("userRole");
            window.location.href = "/login";
        });
    }

    // ── Мобильный hamburger ──
    const sidebar = document.querySelector(".sidebar");
    if (!sidebar) return;

    // Создаём мобильный header
    const mobileHeader = document.createElement("div");
    mobileHeader.className = "mobile-header";
    mobileHeader.innerHTML = `
        <button class="hamburger" id="hamburger-btn" aria-label="Меню">
            <span></span><span></span><span></span>
        </button>
        <span style="color:#fff;font-weight:700;font-size:15px;">Electronic Diary</span>
        <div style="width:30px;"></div>
    `;
    document.body.prepend(mobileHeader);

    // Оверлей
    const overlay = document.createElement("div");
    overlay.className = "sidebar-overlay";
    document.body.appendChild(overlay);

    function openSidebar()  { sidebar.classList.add("open"); overlay.classList.add("open"); document.body.style.overflow = "hidden"; }
    function closeSidebar() { sidebar.classList.remove("open"); overlay.classList.remove("open"); document.body.style.overflow = ""; }

    document.getElementById("hamburger-btn").addEventListener("click", openSidebar);
    overlay.addEventListener("click", closeSidebar);
    nav.addEventListener("click", e => { if (e.target.tagName === "A" && window.innerWidth <= 768) closeSidebar(); });
});
