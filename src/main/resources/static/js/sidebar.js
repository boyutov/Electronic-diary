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
        TEACHER: "Учитель", STUDENT: "Ученик", PARENT: "Родитель", MINISTRY: "Министерство"
    };

    const badge = document.querySelector(".role-badge");
    if (badge && roleLabels[role]) badge.textContent = roleLabels[role];

    const nav = document.getElementById("sidebar-nav");
    if (!nav || !sn) return;

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
            <span class="nav-section">Контент</span>
            <a href="/${sn}/admin/news"${active('/admin/news')}>📰 Новости</a>
            <a href="/${sn}/admin/poll"${active('/admin/poll')}>📊 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>`,
        MINISTRY: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        DIRECTOR: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/director"${active('/director')}>🏠 Панель</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        TEACHER: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/teacher"${active('/teacher') && !path.includes('/curator') ? ' class="active"' : ''}>🏠 Панель</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/exams"${active('/exams')}>📚 Задания</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            <a href="/${sn}/curator/students-list" id="curator-nav-link" style="display:none;">👥 Мой класс</a>
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        STUDENT: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/student"${active('/student')}>🏠 Панель</a>
            <a href="/${sn}/schedule"${active('/schedule')}>📅 Расписание</a>
            <a href="/${sn}/exams"${active('/exams')}>📝 Экзамены/Тесты</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
        PARENT: `
            <span class="nav-section">Главное</span>
            <a href="/${sn}/parent"${active('/parent')}>🏠 Панель</a>
            <a href="/${sn}/exams"${active('/exams')}>📝 Экзамены/Тесты</a>
            <a href="/${sn}/attendance"${active('/attendance')}>📋 Посещаемость</a>
            <a href="/${sn}/polls"${active('/polls')}>📋 Голосования</a>
            <a href="/${sn}/complaints"${active('/complaints')}>📝 Жалобы</a>
            <a href="/${sn}/canteen"${active('/canteen')}>🍽️ Столовая</a>
            <span class="nav-section">Прочее</span>
            <a href="/${sn}/profile"${active('/profile')}>👤 Профиль</a>`,
    };

    nav.innerHTML = navMap[role] || "";

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
});
