const toggleConditional = (select) => {
    const targetId = select.dataset.toggleTarget;
    if (!targetId) {
        return;
    }
    const target = document.getElementById(targetId);
    if (!target) {
        return;
    }
    if (select.value === "yes") {
        target.classList.add("is-visible");
    } else {
        target.classList.remove("is-visible");
    }
};

document.querySelectorAll("[data-toggle-target]").forEach((select) => {
    toggleConditional(select);
    select.addEventListener("change", () => toggleConditional(select));
});
