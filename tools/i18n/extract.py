import re, sys, json, glob, os
from html.parser import HTMLParser
root = sys.argv[1]
CYR = re.compile(r'[А-Яа-яЁё]')
found = {}   # phrase -> set(files)

def norm(t):
    return re.sub(r'\s+', ' ', t).strip()

def add(t, f):
    t = norm(t)
    if t and CYR.search(t):
        found.setdefault(t, set()).add(os.path.basename(f))

def js_strings(code, f):
    # строковые литералы '..', "..", `..` (с ${...} внутри шаблонов)
    for m in re.finditer(r"'((?:\\.|[^'\\\n])*)'|\"((?:\\.|[^\"\\\n])*)\"|`((?:\\.|[^`\\])*)`", code):
        s = m.group(1) or m.group(2) or m.group(3) or ''
        if not CYR.search(s): continue
        s = s.replace("\\'", "'").replace('\\"', '"').replace('\\n', ' ')
        # внутри может быть HTML и ${...}: режем по тегам и выражениям
        for part in re.split(r'<[^>]*>|\$\{(?:[^{}]|\{[^{}]*\})*\}', s):
            add(part, f)
        for am in re.finditer(r'(?:placeholder|title|alt|aria-label|value)="([^"]*)"', s):
            add(am.group(1), f)

class P(HTMLParser):
    def __init__(self, f):
        super().__init__(convert_charrefs=True); self.f = f; self.in_script = False; self.in_style = False
    def handle_starttag(self, tag, attrs):
        self.in_script = tag == 'script'; self.in_style = tag == 'style'
        for k, v in attrs:
            if k in ('placeholder', 'title', 'alt', 'aria-label', 'value', 'content') and v: add(v, self.f)
            if k.startswith('on') and v: js_strings(v, self.f)
    def handle_endtag(self, tag):
        if tag in ('script', 'style'): self.in_script = self.in_style = False
    def handle_data(self, d):
        if self.in_style: return
        if self.in_script: js_strings(d, self.f)
        else:
            # Thymeleaf-выражения не трогаем
            for part in re.split(r'\[\[.*?\]\]', d): add(part, self.f)

for f in glob.glob(root + '/templates/*.html'):
    P(f).feed(open(f, encoding='utf-8').read())
for f in glob.glob(root + '/static/js/*.js'):
    if os.path.basename(f).startswith('i18n'): continue   # сам переводчик и словарь не извлекаем
    code = open(f, encoding='utf-8').read()
    code = re.sub(r'(?<![:\'"\\])//[^\n]*', '', code)        # комментарии (и в конце строк) не переводим
    code = re.sub(r'/\*.*?\*/', '', code, flags=re.S)
    js_strings(code, f)
    # Второй проход: вложенные шаблонные строки (`...${x.map(d => `...`)}...`) разбором литералов не ловятся,
    # поэтому берём любой кусок с кириллицей между кавычками, тегами и ${}
    for run in re.findall(r"[^<>`'\"{}\n]*[А-Яа-яЁё][^<>`'\"{}\n]*", code):
        add(run, f)

out = sorted(found)
json.dump(out, open(sys.argv[2], 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(out), 'фраз;', sum(len(p) for p in out), 'символов')
