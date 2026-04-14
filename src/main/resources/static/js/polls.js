function initPolls(containerId, schoolName) {
    const container = document.getElementById(containerId);
    if (!container) return;

    function render(polls) {
        container.innerHTML = "";
        if (!polls.length) {
            container.innerHTML = "<p style='color:#64748b;'>Активных голосований нет.</p>";
            return;
        }
        polls.forEach(poll => {
            const totalVotes = poll.options.reduce((s, o) => s + o.voteCount, 0);
            const card = document.createElement("div");
            card.className = "accordion-item open";
            card.style.cssText = "margin-bottom:12px;";

            const optionsHtml = poll.options.map(opt => {
                const pct = totalVotes > 0 ? Math.round(opt.voteCount / totalVotes * 100) : 0;
                if (poll.votedByMe) {
                    return `<div style="margin-bottom:10px;">
                        <div style="display:flex;justify-content:space-between;font-size:0.9em;margin-bottom:4px;">
                            <span>${opt.optionText}</span>
                            <span style="color:#64748b;font-weight:600;">${opt.voteCount} (${pct}%)</span>
                        </div>
                        <div style="background:#f1f5f9;border-radius:999px;height:8px;">
                            <div style="background:var(--primary);border-radius:999px;height:8px;width:${pct}%;transition:width 0.4s;"></div>
                        </div>
                    </div>`;
                }
                return `<button class="vote-btn" data-poll="${poll.id}" data-option="${opt.id}"
                    style="display:block;width:100%;text-align:left;padding:10px 14px;margin-bottom:8px;
                    border:1.5px solid var(--border);border-radius:8px;background:#f8fafc;cursor:pointer;
                    font-size:0.92em;font-family:inherit;transition:border-color 0.15s,background 0.15s;">
                    ${opt.optionText}
                </button>`;
            }).join("");

            card.innerHTML = `
                <div class="accordion-header" style="cursor:default;">
                    <span class="acc-title">${poll.title}</span>
                    <span class="acc-meta">
                        <span style="color:#94a3b8;font-size:0.82em;">${totalVotes} голос(ов)</span>
                        ${poll.votedByMe ? '<span class="badge green" style="font-size:0.78em;">✓ Проголосовано</span>' : '<span class="badge blue" style="font-size:0.78em;">Открыто</span>'}
                    </span>
                </div>
                <div class="accordion-body" style="display:block;">
                    ${poll.description ? `<p style="color:#64748b;font-size:0.88em;margin-bottom:12px;">${poll.description}</p>` : ""}
                    ${optionsHtml}
                </div>
            `;
            container.appendChild(card);
        });

        container.querySelectorAll(".vote-btn").forEach(btn => {
            btn.addEventListener("mouseover", () => { btn.style.borderColor = "var(--primary)"; btn.style.background = "#eff6ff"; });
            btn.addEventListener("mouseout",  () => { btn.style.borderColor = "var(--border)";  btn.style.background = "#f8fafc"; });
            btn.addEventListener("click", () => {
                btn.disabled = true;
                apiFetch(`/api/${schoolName}/polls/${btn.dataset.poll}/vote`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ optionId: parseInt(btn.dataset.option) })
                }).then(r => r.json()).then(res => {
                    if (res.error) { alert(res.error); btn.disabled = false; return; }
                    loadPolls();
                }).catch(() => { alert("Ошибка при голосовании"); btn.disabled = false; });
            });
        });
    }

    function loadPolls() {
        apiFetch(`/api/${schoolName}/polls/my`)
            .then(r => r.ok ? r.json() : [])
            .then(render)
            .catch(() => {
                container.innerHTML = "<p style='color:#ef4444;'>Ошибка загрузки голосований.</p>";
            });
    }

    loadPolls();
}
