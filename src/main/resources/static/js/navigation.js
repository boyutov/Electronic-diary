document.addEventListener("DOMContentLoaded", () => {
    const pathParts = window.location.pathname.split('/');
    let schoolName = sessionStorage.getItem("schoolName");

    // Если schoolName нет в sessionStorage, берём из URL
    if (!schoolName || schoolName === "null") {
        const fromUrl = pathParts[1];
        if (fromUrl && !["login", "activate", "css", "js", "api", ""].includes(fromUrl)) {
            schoolName = fromUrl;
            sessionStorage.setItem("schoolName", schoolName);
        }
    }

    if (!schoolName || ["login", "activate", "css", "js", "api", ""].includes(schoolName)) return;

    // Fix all internal links that don't already have the schoolName prefix
    document.querySelectorAll('a[href^="/"]').forEach(link => {
        const href = link.getAttribute("href");
        if (
            href === "/" ||
            href === "/logout" ||
            href.startsWith(`/${schoolName}/`) ||
            href.startsWith("/css/") ||
            href.startsWith("/js/") ||
            href.startsWith("/api/") ||
            href === "#"
        ) return;
        link.setAttribute("href", `/${schoolName}${href}`);
    });

    // Handle logout button
    const logoutBtn = document.getElementById("logout-btn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", e => {
            e.preventDefault();
            sessionStorage.removeItem("jwtToken");
            sessionStorage.removeItem("schoolName");
            sessionStorage.removeItem("userRole");
            window.location.href = "/login";
        });
    }

    // Legacy: replace form-based logout
    document.querySelectorAll("form[action='/logout']").forEach(form => {
        const btn = form.querySelector("button");
        if (!btn) return;
        const newBtn = document.createElement("button");
        newBtn.id = "logout-btn-legacy";
        newBtn.className = btn.className;
        newBtn.setAttribute("style", btn.getAttribute("style") || "");
        newBtn.textContent = btn.textContent;
        newBtn.addEventListener("click", e => {
            e.preventDefault();
            sessionStorage.removeItem("jwtToken");
            sessionStorage.removeItem("schoolName");
            sessionStorage.removeItem("userRole");
            window.location.href = "/login";
        });
        form.replaceWith(newBtn);
    });

    // Add profile link to public-layout nav if missing
    const nav = document.querySelector("body.public-layout nav, header nav");
    if (nav && !document.getElementById("nav-profile")) {
        const profileLink = document.createElement("a");
        profileLink.id = "nav-profile";
        profileLink.href = `/${schoolName}/profile`;
        profileLink.textContent = "Личный кабинет";
        profileLink.style.fontWeight = "bold";
        nav.appendChild(profileLink);
    }
});
