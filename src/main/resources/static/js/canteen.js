document.addEventListener("DOMContentLoaded", () => {
    const schoolName = sessionStorage.getItem("schoolName") || window.location.pathname.split('/')[1];
    const role = sessionStorage.getItem("userRole") || "";
    const canManage = ["ADMIN", "DIRECTOR"].includes(role);

    const container = document.getElementById("menu-container");
    const modal = document.getElementById("modal");
    const form = document.getElementById("menu-form");
    const formError = document.getElementById("form-error");
    const addBtn = document.getElementById("add-btn");
    const groupsContainer = document.getElementById("groups-container");

    if (canManage) addBtn.style.display = "inline-flex";

    let imageData = null;
    let imageType = null;
    let allGroups = [];

    // Загружаем группы для формы
    if (canManage) {
        apiFetch(`/api/${schoolName}/groups`)
            .then(r => r.ok ? r.json() : [])
            .then(groups => {
                allGroups = groups;
                renderGroupCheckboxes([]);
            });
    }

    // Превью фото
    document.getElementById("imageFile").addEventListener("change", function () {
        const file = this.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = e => {
            imageData = e.target.result.split(",")[1]; // base64 без префикса
            imageType = file.type;
            document.getElementById("preview-img").src = e.target.result;
            document.getElementById("image-preview").style.display = "block";
        };
        reader.readAsDataURL(file);
    });

    function renderGroupCheckboxes(selectedIds) {
        groupsContainer.innerHTML = "";
        if (!allGroups.length) {
            groupsContainer.innerHTML = "<p style='color:#64748b;font-size:0.9em;'>Групп нет</p>";
            return;
        }
        allGroups.forEach(g => {
            const div = document.createElement("div");
            div.className = "checkbox-item";
            div.innerHTML = `
                <input type="checkbox" id="grp-${g.id}" value="${g.id}" name="groups"
                    ${selectedIds.includes(g.id) ? "checked" : ""}>
                <label for="grp-${g.id}">${g.name}${g.course ? " (" + g.course + " курс)" : ""}</label>
            `;
            groupsContainer.appendChild(div);
        });
    }

    function getSelectedGroupIds() {
        return Array.from(document.querySelectorAll("input[name='groups']:checked"))
            .map(cb => parseInt(cb.value));
    }

    function loadMenus() {
        const endpoint = canManage
            ? `/api/${schoolName}/canteen`
            : `/api/${schoolName}/canteen/my`;

        apiFetch(endpoint)
            .then(r => r.ok ? r.json() : [])
            .then(menus => {
                container.innerHTML = "";
                if (!menus.length) {
                    container.innerHTML = "<p style='color:#64748b;'>Меню пока нет.</p>";
                    return;
                }
                menus.sort((a, b) => new Date(b.menuDate) - new Date(a.menuDate));
                menus.forEach(m => renderMenuCard(m));
            })
            .catch(() => {
                container.innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки.</p>";
            });
    }

    function renderMenuCard(m) {
        const card = document.createElement("div");
        card.className = "menu-card";

        const imgHtml = m.imageData
            ? `<img src="data:${m.imageType};base64,${m.imageData}" alt="${m.title}">`
            : `<div class="img-placeholder">🍽️</div>`;

        const date = m.menuDate
            ? new Date(m.menuDate).toLocaleDateString("ru-RU", { day: "2-digit", month: "long", year: "numeric" })
            : "—";

        const groupsHtml = m.groupNames && m.groupNames.length
            ? m.groupNames.map(n => `<span class="badge blue">${n}</span>`).join("")
            : `<span class="badge gray">Все классы</span>`;

        const actionsHtml = canManage ? `
            <div class="menu-card-actions">
                <button class="button warning sm edit-btn" data-id="${m.id}">Ред.</button>
                <button class="button danger sm delete-btn" data-id="${m.id}">Удал.</button>
            </div>` : "";

        card.innerHTML = `
            ${imgHtml}
            <div class="menu-card-body">
                <div class="menu-card-title">${m.title}</div>
                <div class="menu-card-date">📅 ${date}</div>
                ${m.description ? `<div class="menu-card-desc">${m.description}</div>` : ""}
                <div class="menu-card-groups">${groupsHtml}</div>
                ${actionsHtml}
            </div>
        `;

        if (canManage) {
            card.querySelector(".edit-btn").addEventListener("click", () => openEditModal(m));
            card.querySelector(".delete-btn").addEventListener("click", () => deleteMenu(m.id));
        }

        container.appendChild(card);
    }

    function openEditModal(m) {
        document.getElementById("modal-title").textContent = "Редактировать меню";
        document.getElementById("menu-id").value = m.id;
        document.getElementById("title").value = m.title;
        document.getElementById("description").value = m.description || "";
        document.getElementById("menuDate").value = m.menuDate;
        document.getElementById("image-preview").style.display = m.imageData ? "block" : "none";
        if (m.imageData) document.getElementById("preview-img").src = `data:${m.imageType};base64,${m.imageData}`;
        imageData = null; imageType = null; // сбрасываем — не меняем фото если не выбрали новое
        renderGroupCheckboxes(m.groupIds || []);
        formError.style.display = "none";
        modal.classList.add("open");
    }

    addBtn.onclick = () => {
        document.getElementById("modal-title").textContent = "Добавить меню";
        form.reset();
        document.getElementById("menu-id").value = "";
        document.getElementById("image-preview").style.display = "none";
        document.getElementById("menuDate").value = new Date().toISOString().split("T")[0];
        imageData = null; imageType = null;
        renderGroupCheckboxes([]);
        formError.style.display = "none";
        modal.classList.add("open");
    };

    document.getElementById("modal-close").onclick = () => modal.classList.remove("open");
    modal.addEventListener("click", e => { if (e.target === modal) modal.classList.remove("open"); });

    form.addEventListener("submit", e => {
        e.preventDefault();
        formError.style.display = "none";
        const id = document.getElementById("menu-id").value;
        const data = {
            title: document.getElementById("title").value,
            description: document.getElementById("description").value,
            menuDate: document.getElementById("menuDate").value,
            groupIds: getSelectedGroupIds()
        };
        if (imageData) { data.imageData = imageData; data.imageType = imageType; }

        apiFetch(id ? `/api/${schoolName}/canteen/${id}` : `/api/${schoolName}/canteen`, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        }).then(r => {
            if (r.ok) { modal.classList.remove("open"); loadMenus(); }
            else r.json().then(err => {
                formError.textContent = typeof err === "object" ? Object.values(err).join(", ") : String(err);
                formError.style.display = "block";
            });
        });
    });

    function deleteMenu(id) {
        if (!confirm("Удалить меню?")) return;
        apiFetch(`/api/${schoolName}/canteen/${id}`, { method: "DELETE" })
            .then(r => { if (r.ok) loadMenus(); else alert("Ошибка при удалении"); });
    }

    loadMenus();
});
