// Двуязычный интерфейс: русский (исходный текст страниц) и кыргызский (словарь window.I18N_KY).
// Подключается в <head> каждой страницы ПОСЛЕ i18n-ky.js. Переводит весь текст страницы, подсказки в полях,
// заголовок вкладки, alert/confirm и даты — в том числе то, что JS дорисовывает позже (MutationObserver).
(function () {
    const STORAGE_KEY = "lang";
    let lang = "ru";
    try { lang = localStorage.getItem(STORAGE_KEY) || "ru"; } catch (ignored) { }

    window.I18N = {
        lang,
        setLang(next) {
            try { localStorage.setItem(STORAGE_KEY, next); } catch (ignored) { }
            window.location.reload();
        },
        t: (text) => text
    };

    document.addEventListener("DOMContentLoaded", addSwitcher);
    if (lang !== "ky" || !window.I18N_KY) return;

    const dict = window.I18N_KY;
    document.documentElement.lang = "ky";

    // Те же правила, что в сборке словаря: края (значки, знаки) отрезаются, числа становятся {n}
    const EDGE = /^([^\p{L}\p{N}]*)([\s\S]*?)([^\p{L}\p{N}]*)$/u;
    const CYRILLIC = /[А-Яа-яЁё]/;

    // Фразы, которые JS собирает по кусочкам ("Оценки — " + имя): переводим известные куски внутри текста.
    // Только фразы от 4 символов и только в коротком тексте, чтобы не портить пользовательский контент
    const fragmentKeys = Object.keys(dict)
        .filter(k => !k.includes("{n}") && k.length >= 4)
        .sort((a, b) => b.length - a.length)
        .map(k => k.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
    const FRAGMENTS = new RegExp("(?<![\\p{L}])(" + fragmentKeys.join("|") + ")(?![\\p{L}])", "gu");
    const FRAGMENT_MAX_LENGTH = 60;

    function translateCore(core) {
        const numbers = core.match(/\d+/g) || [];
        const translated = dict[core.replace(/\d+/g, "{n}")];
        if (translated === undefined) return null;
        let i = 0;
        return translated.replace(/\{n\}/g, () => numbers[i++] ?? "");
    }

    function translate(text, allowFragments) {
        if (!text || !CYRILLIC.test(text)) return text;
        const lead = text.match(/^\s*/)[0];
        const trail = text.match(/\s*$/)[0];
        const inner = text.trim().replace(/\s+/g, " ");
        const [, pre, core, post] = EDGE.exec(inner);

        const exact = translateCore(core);
        if (exact !== null) return lead + pre + exact + post + trail;

        if (allowFragments && inner.length <= FRAGMENT_MAX_LENGTH) {
            const partial = inner.replace(FRAGMENTS, m => dict[m]);
            if (partial !== inner) return lead + partial + trail;
        }
        return text;
    }
    window.I18N.t = (text) => translate(text, true);

    const SKIP_TAGS = new Set(["SCRIPT", "STYLE", "TEXTAREA", "PRE", "CODE", "NOSCRIPT"]);
    const ATTRIBUTES = ["placeholder", "title", "aria-label", "alt"];
    const done = new WeakMap(); // узел → наш перевод: не переводим повторно то, что уже перевели

    function skipped(el) {
        return !el || SKIP_TAGS.has(el.tagName) || el.closest("[data-no-i18n]");
    }

    function translateTextNode(node) {
        const parent = node.parentElement;
        if (skipped(parent) || done.get(node) === node.nodeValue) return;
        // Переписку с ИИ переводим только целиком (приветствие), а не по кусочкам
        const allowFragments = !parent.closest(".ai-msg, [data-i18n-exact]");
        const translated = translate(node.nodeValue, allowFragments);
        if (translated !== node.nodeValue) node.nodeValue = translated;
        done.set(node, node.nodeValue);
    }

    // У <textarea> не трогаем введённый текст, но подсказку (placeholder) переводим
    function translateAttributes(el) {
        if (el.tagName === "SCRIPT" || el.tagName === "STYLE" || el.closest("[data-no-i18n]")) return;
        for (const name of ATTRIBUTES) {
            const value = el.getAttribute(name);
            if (value) {
                const translated = translate(value, true);
                if (translated !== value) el.setAttribute(name, translated);
            }
        }
        if (el.tagName === "INPUT" && (el.type === "button" || el.type === "submit") && el.value) {
            el.value = translate(el.value, true);
        }
    }

    function translateTree(root) {
        if (root.nodeType === Node.TEXT_NODE) { translateTextNode(root); return; }
        if (root.nodeType !== Node.ELEMENT_NODE) return;
        translateAttributes(root);
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
        let node;
        while ((node = walker.nextNode())) {
            if (node.nodeType === Node.TEXT_NODE) translateTextNode(node);
            else translateAttributes(node);
        }
    }

    // Пока страница не переведена — не показываем её, чтобы не мелькал русский текст
    const hideStyle = document.createElement("style");
    hideStyle.textContent = "html.i18n-pending body { visibility: hidden !important; }";
    document.head.appendChild(hideStyle);
    document.documentElement.classList.add("i18n-pending");
    const reveal = () => document.documentElement.classList.remove("i18n-pending");
    setTimeout(reveal, 1500); // страховка: страница не должна остаться скрытой

    // Всё, что появляется позже (данные с сервера, модальные окна), переводится до отрисовки
    new MutationObserver(mutations => {
        for (const m of mutations) {
            if (m.type === "characterData") translateTextNode(m.target);
            else if (m.type === "attributes") translateAttributes(m.target);
            else m.addedNodes.forEach(translateTree);
        }
    }).observe(document.documentElement, {
        childList: true, subtree: true, characterData: true,
        attributes: true, attributeFilter: ATTRIBUTES.concat(["value"])
    });

    document.addEventListener("DOMContentLoaded", () => {
        translateTree(document.documentElement);
        document.title = translate(document.title, true);
        reveal();
    });

    // Сообщения alert/confirm/prompt тоже переводим
    for (const name of ["alert", "confirm", "prompt"]) {
        const original = window[name].bind(window);
        window[name] = (message, ...rest) => original(translate(String(message ?? ""), true), ...rest);
    }

    // Даты: "ru-RU" в коде страниц → кыргызская локаль
    for (const method of ["toLocaleDateString", "toLocaleString", "toLocaleTimeString"]) {
        const original = Date.prototype[method];
        Date.prototype[method] = function (locale, options) {
            const target = !locale || String(locale).startsWith("ru") ? "ky-KG" : locale;
            return original.call(this, target, options);
        };
    }
})();

// Переключатель языка: внизу бокового меню, а на страницах без меню — в правом верхнем углу
function addSwitcher() {
    if (document.getElementById("lang-switcher")) return;
    const current = window.I18N.lang;
    const wrap = document.createElement("div");
    wrap.id = "lang-switcher";
    wrap.className = "lang-switch";
    wrap.setAttribute("data-no-i18n", "");
    for (const [code, label] of [["ru", "Русский"], ["ky", "Кыргызча"]]) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "lang-btn";
        btn.textContent = label;
        btn.setAttribute("aria-pressed", String(code === current));
        if (code !== current) btn.addEventListener("click", () => window.I18N.setLang(code));
        wrap.appendChild(btn);
    }
    const footer = document.querySelector(".sidebar-footer");
    const slot = document.querySelector("[data-lang-slot]");
    if (footer) {
        footer.insertBefore(wrap, footer.querySelector("#logout-btn"));
    } else if (slot) {
        slot.appendChild(wrap);
    } else {
        wrap.classList.add("floating");
        document.body.appendChild(wrap);
    }
}
