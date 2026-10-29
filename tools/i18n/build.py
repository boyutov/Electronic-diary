import re, json, sys, importlib.util
S, OUT = sys.argv[1], sys.argv[2]
spec = importlib.util.spec_from_file_location("d", S + "/ky_dict.py"); d = importlib.util.module_from_spec(spec); spec.loader.exec_module(d)

# ВАЖНО: те же правила, что в i18n.js — края (значки, знаки) отрезаются, числа становятся {n}
EDGE = re.compile(r'^([\W_]*)(.*?)([\W_]*)$', re.S)
def key(t):
    t = re.sub(r'\s+', ' ', t).strip()
    core = EDGE.match(t).group(2)
    return re.sub(r'\d+', '{n}', core)

table, conflicts = {}, []
for ru, ky in d.KY.items():
    k, v = key(ru), key(ky)
    if k.count('{n}') != v.count('{n}'):
        conflicts.append(f'числа не совпадают: {ru!r} → {ky!r}'); continue
    if k in table and table[k] != v: conflicts.append(f'разные переводы для {k!r}: {table[k]!r} / {v!r}')
    table[k] = v

phrases = json.load(open(S + '/phrases.json'))
missing = sorted({key(p) for p in phrases if key(p) and re.search('[А-Яа-яЁё]', key(p)) and key(p) not in table})
print('переводов:', len(table), '| фраз интерфейса:', len(phrases), '| без перевода:', len(missing))
for c in conflicts: print('  !', c)
if '-v' in sys.argv:
    for m in missing: print('   -', m)

with open(OUT, 'w', encoding='utf-8') as f:
    f.write('// Словарь интерфейса: русский → кыргызский. Сгенерирован из tools/i18n/ky_dict.py — правьте переводы там\n')
    f.write('window.I18N_KY = ' + json.dumps(table, ensure_ascii=False, indent=0, sort_keys=True) + ';\n')
