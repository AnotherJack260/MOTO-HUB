#!/usr/bin/env python3
"""RideLink catalogue helper.

  tooling/i18n.py missing           -> JSON list of {key, source, at}: motoHubText literals with no
                                       entry in translations/strings-en-US.xml
  tooling/i18n.py add <file.json>   -> file: {"<key>": {"source": "...", "it-IT": "...", ...}};
                                       appends each entry to every catalogue (en-US gets the source)
  tooling/i18n.py key "<text>"      -> the key motoHubText derives for a source string

Keys mirror io.motohub.android.i18n.MotoHubStrings.keyFor: legacy_<slug48>_<java hashCode hex>.
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'apps/android/app/src/main/java')
TR = os.path.join(ROOT, 'translations')
LOCALES = ['en-US', 'it-IT', 'pt-PT', 'ko-KR', 'fr-FR', 'es-ES', 'de-DE', 'nl-NL', 'cs-CZ', 'tr-TR', 'ru-RU']
ESC = {'n': '\n', 't': '\t', 'r': '\r', '"': '"', "'": "'", '\\': '\\', '$': '$', 'b': '\b'}
CALL = re.compile(r'\b(?:motoHubText|MotoHubStrings\.get|localized)\(\s*(?:context\s*,\s*)?')


def java_hash(s):
    b = s.encode('utf-16-le')
    h = 0
    for i in range(0, len(b), 2):
        h = (31 * h + int.from_bytes(b[i:i + 2], 'little')) & 0xFFFFFFFF
    return h


def key_for(source):
    stem = re.sub(r'[^a-z0-9]+', '_', source.lower()).strip('_')[:48] or 'text'
    return 'legacy_%s_%x' % (stem, java_hash(source))


def parse_literal(text, i):
    """text[i] == '"'. Returns (value, end) or (None, i) for raw strings and templates."""
    if text.startswith('"""', i):
        return None, i
    i += 1
    out = []
    while i < len(text):
        c = text[i]
        if c == '\\':
            n = text[i + 1]
            if n == 'u':
                out.append(chr(int(text[i + 2:i + 6], 16))); i += 6; continue
            out.append(ESC.get(n, n)); i += 2; continue
        if c == '$' and i + 1 < len(text) and (text[i + 1] == '{' or text[i + 1].isalpha()):
            return None, i
        if c == '"':
            return ''.join(out), i + 1
        out.append(c); i += 1
    return None, i


def literal_expr(text, i):
    """A literal or a + concatenation of literals starting at text[i], else None."""
    parts = []
    while True:
        while i < len(text) and text[i] in ' \t\r\n':
            i += 1
        if i >= len(text) or text[i] != '"':
            return None
        v, i = parse_literal(text, i)
        if v is None:
            return None
        parts.append(v)
        j = i
        while j < len(text) and text[j] in ' \t\r\n':
            j += 1
        if j < len(text) and text[j] == '+':
            i = j + 1
            continue
        return ''.join(parts)


def used_sources():
    found = {}
    for d, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(d, f)
            text = open(p, encoding='utf-8').read()
            for m in CALL.finditer(text):
                v = literal_expr(text, m.end())
                if v:
                    found.setdefault(v, os.path.relpath(p, SRC) + ':' + str(text.count('\n', 0, m.start()) + 1))
    return found


def catalogue(locale):
    return os.path.join(TR, 'strings-%s.xml' % locale)


def xml_escape(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    return s.replace('\\', '\\\\').replace("'", "\\'").replace('"', '\\"').replace('\n', '\\n')


def missing():
    en = open(catalogue('en-US'), encoding='utf-8').read()
    out = [{'key': key_for(s), 'source': s, 'at': at}
           for s, at in sorted(used_sources().items()) if 'name="%s"' % key_for(s) not in en]
    print(json.dumps(out, ensure_ascii=False, indent=1))
    print('# %d missing' % len(out), file=sys.stderr)


def add(path):
    entries = json.load(open(path, encoding='utf-8'))
    for locale in LOCALES:
        p = catalogue(locale)
        text = open(p, encoding='utf-8').read()
        lines = []
        for key, e in entries.items():
            if 'name="%s"' % key in text:
                continue
            value = e['source'] if locale == 'en-US' else e[locale]
            lines.append('    <string name="%s">%s</string>\n' % (key, xml_escape(value)))
        text = text.replace('</resources>', ''.join(lines) + '</resources>')
        open(p, 'w', encoding='utf-8').write(text)
        print('%s + %d' % (locale, len(lines)))


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'missing':
        missing()
    elif cmd == 'add':
        add(sys.argv[2])
    elif cmd == 'key':
        print(key_for(sys.argv[2]))
    else:
        print(__doc__)
