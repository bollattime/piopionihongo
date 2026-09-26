#!/usr/bin/env python3
"""
Kanji Kartları – derleme betiği

data/ klasöründeki JSON dosyalarını okuyup şunları üretir:

  docs/index.html                         Web sürümü (GitHub Pages ana sayfası)
  docs/data.json                          Uygulamanın indirdiği güncel veri
  docs/version.json                       Veri sürümü + SHA-256 özeti
  docs/privacy.html                       Gizlilik politikası (Google Play için)
  docs/strokes.js                         Yazma çalışması için çizgi verisi (KanjiVG)
  docs/words.js                           Kelime sözlüğü (50.000 kelime) + cümleleri
  docs/sentences.js                       Örnek cümleler (Tatoeba), istek üzerine yüklenir
  docs/dict.js                            Sözlük katmanı: JLPT dışı 8.000+ kanji (istek üzerine yüklenir)
  android/app/src/main/assets/index.html  Android'in içindeki hazır veri
  android/app/src/main/assets/config.json Uzaktan güncelleme adresi
  android/app/src/main/assets/strokes.js  Çizgi verisi (Android)

Veri sürümü otomatik artar: data/ içeriği değiştiyse bir önceki
docs/version.json'daki sayı +1 yapılır, değişmediyse aynı kalır.

Kullanım:
  python3 tools/build.py            # derle
  python3 tools/build.py --check    # sadece verileri doğrula
"""
import hashlib, json, pathlib, shutil, sys, datetime

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "data"
DOCS = ROOT / "docs"
ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets"
LEVELS = ["n5", "n4", "n3", "n2", "n1"]


def load(p):
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except FileNotFoundError:
        sys.exit(f"HATA: {p.relative_to(ROOT)} bulunamadı")
    except json.JSONDecodeError as e:
        sys.exit(f"HATA: {p.relative_to(ROOT)} satır {e.lineno}, sütun {e.colno}: {e.msg}")


def collect():
    errors = []
    kanji, words = [], {}
    radicals = load(DATA / "radicals" / "radicals.json")
    rad_keys = {r["key"] for r in radicals}
    seen = set()

    for lv in LEVELS:
        path = f"data/kanji/{lv}.json"
        for i, k in enumerate(load(DATA / "kanji" / f"{lv}.json"), start=2):
            where = f"{path} satır {i}"
            missing = [f for f in ("kanji", "level", "radical", "on", "kun", "easy", "meaning") if f not in k]
            if missing:
                errors.append(f"{where}: eksik alan {missing}")
                continue
            if k["kanji"] in seen:
                errors.append(f"{where}: {k['kanji']} birden fazla kez var")
            seen.add(k["kanji"])
            if k["level"].lower() != lv:
                errors.append(f"{where}: level '{k['level']}' dosya adıyla ({lv}) uyuşmuyor")
            if k["radical"] not in rad_keys:
                errors.append(f"{where}: radikal '{k['radical']}' radicals.json içinde yok")
            m = k["meaning"]
            if not m.get("tr") or not m.get("en"):
                errors.append(f"{where}: meaning.tr ve meaning.en dolu olmalı")
            kanji.append([k["kanji"], k["level"], k["radical"], k["on"], k["kun"], k["easy"],
                          m.get("tr", ""), m.get("en", "")])

        path = f"data/vocabulary/{lv}.json"
        for i, w in enumerate(load(DATA / "vocabulary" / f"{lv}.json"), start=2):
            where = f"{path} satır {i}"
            missing = [f for f in ("kanji", "word", "reading", "meaning") if f not in w]
            if missing:
                errors.append(f"{where}: eksik alan {missing}")
                continue
            if w["kanji"] not in seen:
                errors.append(f"{where}: kanji '{w['kanji']}' bu seviyenin kanji dosyasında yok")
            m = w["meaning"]
            words.setdefault(w["kanji"], []).append([w["word"], w["reading"], m.get("tr", ""), m.get("en", ""), w.get("pos", "")])

    i18n = {p.stem: load(p) for p in sorted((DATA / "i18n").glob("*.json"))}
    base_keys = set(i18n.get("tr", {}))
    for name, d in i18n.items():
        diff = base_keys - set(d)
        if diff:
            errors.append(f"data/i18n/{name}.json: eksik anahtarlar {sorted(diff)}")

    config = load(DATA / "config.json")
    free = config.get("freeLevels", ["N5"])
    if not isinstance(free, list) or not all(l in ("N5", "N4", "N3", "N2", "N1") for l in free):
        errors.append("data/config.json: freeLevels N5–N1 değerlerinden oluşan bir liste olmalı")

    # Örnek cümleler (kelime -> [[ja, en, tr]])
    sentences = {}
    for s in load(DATA / "sentences" / "sentences.json"):
        if s.get("word") and s.get("ja"):
            sentences.setdefault(s["word"], []).append([s["ja"], s.get("en", ""), s.get("tr", "")])

    # Bileşen (radikal parça) verisi – parçaya göre arama için
    comps = {}
    for i, c in enumerate(load(DATA / "dict" / "components.json"), start=2):
        if c.get("kanji") and c.get("components"):
            comps[c["kanji"]] = c["components"]

    # Kelime sözlüğü
    dwords = []
    for i, w in enumerate(load(DATA / "dict" / "words.json"), start=2):
        if not w.get("w") or not w.get("r") or not w.get("en"):
            errors.append(f"data/dict/words.json satır {i}: 'w', 'r' ve 'en' gerekli")
            continue
        dwords.append([w["w"], w["r"], w.get("pos", ""), w["en"], w.get("tr", ""), 1 if w.get("core") else 0])
    dsent = {}
    for s in load(DATA / "sentences" / "dict-sentences.json"):
        if s.get("word") and s.get("ja"):
            dsent.setdefault(s["word"], []).append([s["ja"], s.get("en", ""), s.get("tr", "")])

    # Ekranda kelime başına en fazla 3 örnek gösteriliyor (önce sentences.json); fazlasını pakete koyma
    for wd in list(dsent):
        room = max(0, 3 - len(sentences.get(wd, [])))
        if room == 0:
            del dsent[wd]
        else:
            dsent[wd] = dsent[wd][:room]

    # Çekim biçimlerine bağlı örnek cümleler
    forms = {}
    for f in load(DATA / "sentences" / "forms.json"):
        if f.get("word") and f.get("formKey") and f.get("ja"):
            forms.setdefault(f["word"], {})[f["formKey"]] = [f["ja"], f.get("en", ""), f.get("tr", "")]

    # Sözlük katmanı (JLPT dışı kanjiler)
    dict_kanji, dict_words, dict_strokes = [], {}, {}
    n_g = 0
    for i, k in enumerate(load(DATA / "dict" / "kanji.json"), start=2):
        where = f"data/dict/kanji.json satır {i}"
        if not k.get("kanji") or not k.get("meaning", {}).get("en"):
            errors.append(f"{where}: 'kanji' ve 'meaning.en' gerekli")
            continue
        m = k["meaning"]
        dict_kanji.append([k["kanji"], k.get("tier", "X"), k.get("radical", ""), k.get("on", []),
                           k.get("kun", []), k.get("easy", ""), m.get("tr", ""), m["en"]])
        if k.get("tier") == "G":
            n_g += 1
    dict_set = {k[0] for k in dict_kanji}
    for w in load(DATA / "dict" / "vocabulary.json"):
        if w.get("kanji") in dict_set:
            dict_words.setdefault(w["kanji"], []).append([w["word"], w["reading"], w["meaning"].get("tr", ""), w["meaning"].get("en", "")])
    for s in load(DATA / "dict" / "strokes.json"):
        if s.get("kanji") and s.get("strokes"):
            dict_strokes[s["kanji"]] = s["strokes"]

    # Yazma çalışması verisi
    strokes = {}
    for i, s in enumerate(load(DATA / "strokes" / "strokes.json"), start=2):
        if not s.get("kanji") or not isinstance(s.get("strokes"), list) or not s["strokes"]:
            errors.append(f"data/strokes/strokes.json satır {i}: 'kanji' ve boş olmayan 'strokes' gerekli")
            continue
        strokes[s["kanji"]] = s["strokes"]
    missing = [k[0] for k in kanji if k[0] not in strokes]
    if missing:
        print(f"UYARI: {len(missing)} kanjinin çizgi verisi yok: {''.join(missing[:30])}")

    # Gramer, günlük konuşmalar/ifadeler ve konu gruplu kelimeler (study.js)
    study = {"grammar": {}, "conv": [], "phrases": [], "words": []}
    for lv in ("n5", "n4", "n3", "n2", "n1"):
        gp = DATA / "grammar" / f"{lv}.json"
        if gp.exists():
            items = load(gp)
            for g in items:
                for k in ("id", "p", "m", "f", "d", "ex", "q"):
                    if k not in g:
                        errors.append(f"data/grammar/{lv}.json: {g.get('id')} içinde '{k}' eksik")
            study["grammar"][lv.upper()] = items
    for name in ("conversations", "phrases", "words"):
        fp = DATA / "daily" / f"{name}.json"
        if fp.exists():
            study["conv" if name == "conversations" else name] = load(fp)

    content = {
        "kanji": kanji,
        "words": words,
        "radicals": [[r["key"], r["forms"], r["name_ja"], r["strokes"], r["tr"], r["en"]] for r in radicals],
        "i18n": i18n,
        "comps": {k: v for k, v in comps.items() if k in {c[0] for c in kanji}},
        "config": {
            "freeLevels": free,
            "playUrl": config.get("playUrl", ""),
            "donateUrl": config.get("donateUrl", ""),
            "supportEmail": config.get("supportEmail", ""),
        },
    }
    dict_payload = {
        "kanji": dict_kanji,
        "words": dict_words,
        "strokes": dict_strokes,
        "comps": {k: v for k, v in comps.items() if k in dict_set},
    }
    core = [w[:5] for w in dwords if w[5]]
    extra = [w[:5] for w in dwords if not w[5]]
    words_payload = {"words": core, "sentences": dsent, "forms": forms}
    extra_payload = {"words": extra}
    return content, config, strokes, dict_payload, sentences, (words_payload, extra_payload), study, errors


def main():
    content, config, strokes, dict_payload, sentences, (words_payload, extra_payload), study, errors = collect()
    if errors:
        print("\n".join(errors))
        sys.exit(f"\n{len(errors)} hata bulundu, derleme yapılmadı.")
    n_words = sum(map(len, content["words"].values()))
    print(f"Veri tamam: {len(content['kanji'])} kanji, {n_words} kelime, {len(content['radicals'])} radikal, "
          f"{len(strokes)} kanjinin çizgi verisi")
    print(f"Örnek cümle: {sum(map(len, sentences.values()))} cümle, {len(sentences)} kelime")
    print(f"Çekim örneği: {sum(len(v) for v in words_payload['forms'].values())} cümle, "
          f"{len(words_payload['forms'])} kelime")
    print(f"Kelime sözlüğü: {len(words_payload['words'])} sık + {len(extra_payload['words'])} nadir kelime, "
          f"{sum(map(len, words_payload['sentences'].values()))} cümle")
    print(f"Gramer: {sum(map(len, study['grammar'].values()))} konu · Konuşma: {len(study['conv'])} · "
          f"İfade: {len(study['phrases'])} · Günlük kelime: {len(study['words'])}")
    print(f"Günlük (sık kullanılan) seviyesi: "
          f"{sum(1 for k in dict_payload['kanji'] if k[1] == 'G')} kanji")
    print(f"Sözlük katmanı: {len(dict_payload['kanji'])} kanji, "
          f"{sum(map(len, dict_payload['words'].values()))} kelime, "
          f"{len(dict_payload['strokes'])} çizgi verisi, "
          f"{len(content['comps']) + len(dict_payload['comps'])} bileşen kaydı")
    if "--check" in sys.argv:
        return

    remote = config.get("remoteBaseUrl", "")
    if "KULLANICI" in remote or not remote.startswith("https://"):
        print("UYARI: data/config.json içindeki remoteBaseUrl ayarlanmamış; uygulama uzaktan güncelleme yapmaz.")

    # İçerik özeti ve sürüm
    canonical = json.dumps(content, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    content_sha = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    prev = {}
    if (DOCS / "version.json").exists():
        prev = json.loads((DOCS / "version.json").read_text(encoding="utf-8"))
    if prev.get("contentSha") == content_sha:
        version = prev["dataVersion"]
    else:
        version = int(prev.get("dataVersion", 0)) + 1

    payload = {"meta": {"version": version, "contentSha": content_sha,
                        "built": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")},
               **content}
    if prev.get("contentSha") == content_sha and (DOCS / "data.json").exists():
        data_bytes = (DOCS / "data.json").read_bytes()          # değişmediyse dosyaya dokunma
        payload = json.loads(data_bytes)
    else:
        data_bytes = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

    js = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")
    html = (ROOT / "src" / "app.template.html").read_text(encoding="utf-8").replace("/*__DATA__*/null", js)

    DOCS.mkdir(exist_ok=True)
    (DOCS / "index.html").write_text(html, encoding="utf-8")
    (DOCS / "data.json").write_bytes(data_bytes)
    (DOCS / "version.json").write_text(json.dumps({
        "dataVersion": version,
        "minAppVersion": int(config.get("minAppVersion", 1)),
        "file": "data.json",
        "sha256": hashlib.sha256(data_bytes).hexdigest(),
        "contentSha": content_sha,
    }, indent=2) + "\n", encoding="utf-8")
    # Gizlilik politikası: iletişim satırı data/config.json'daki supportEmail'den gelir
    email = (config.get("supportEmail") or "").strip()
    issues = "https://github.com/bollattime/piopionihongo/issues"
    if email:
        c_tr = c_en = f'<a href="mailto:{email}">{email}</a>'
    else:
        c_tr = f'<a href="{issues}">GitHub üzerinden</a>'
        c_en = f'<a href="{issues}">via GitHub</a>'
        print("UYARI: data/config.json içindeki supportEmail boş; gizlilik politikasında iletişim olarak GitHub gösteriliyor.")
    privacy = (ROOT / "src" / "privacy.html").read_text(encoding="utf-8")
    privacy = privacy.replace("<!--CONTACT_TR-->", c_tr).replace("<!--CONTACT_EN-->", c_en)
    (DOCS / "privacy.html").write_text(privacy, encoding="utf-8")
    strokes_js = ("window.KANJI_STROKES=" +
                  json.dumps(strokes, ensure_ascii=False, separators=(",", ":")) + ";\n")
    (DOCS / "strokes.js").write_text(strokes_js, encoding="utf-8")
    sent_js = ("window.KANJI_SENTENCES=" +
               json.dumps(sentences, ensure_ascii=False, separators=(",", ":")) + ";\n")
    (DOCS / "sentences.js").write_text(sent_js, encoding="utf-8")
    words_js = ("window.KANJI_WORDS=" +
                json.dumps(words_payload, ensure_ascii=False, separators=(",", ":")) + ";\n")
    (DOCS / "words.js").write_text(words_js, encoding="utf-8")
    extra_js = ("window.KANJI_WORDS_EXTRA=" +
                json.dumps(extra_payload, ensure_ascii=False, separators=(",", ":")) + ";\n")
    (DOCS / "words-extra.js").write_text(extra_js, encoding="utf-8")
    dict_js = ("window.KANJI_DICT=" +
               json.dumps(dict_payload, ensure_ascii=False, separators=(",", ":")) + ";\n")
    (DOCS / "dict.js").write_text(dict_js, encoding="utf-8")
    study_js = ("window.PIO_STUDY=" +
                json.dumps(study, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/") + ";\n")
    (DOCS / "study.js").write_text(study_js, encoding="utf-8")
    (DOCS / ".nojekyll").write_text("", encoding="utf-8")

    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / "index.html").write_text(html, encoding="utf-8")
    (ASSETS / "strokes.js").write_text(strokes_js, encoding="utf-8")
    (ASSETS / "dict.js").write_text(dict_js, encoding="utf-8")
    (ASSETS / "study.js").write_text(study_js, encoding="utf-8")
    (ASSETS / "sentences.js").write_text(sent_js, encoding="utf-8")
    (ASSETS / "words.js").write_text(words_js, encoding="utf-8")
    (ASSETS / "words-extra.js").write_text(extra_js, encoding="utf-8")
    (ASSETS / "config.json").write_text(json.dumps({"remoteBaseUrl": remote}, indent=2) + "\n", encoding="utf-8")

    print(f"Veri sürümü: {version}" + ("  (değişiklik yok)" if prev.get("contentSha") == content_sha else "  (yeni)"))
    print("Yazıldı: docs/ ve android/app/src/main/assets/")


if __name__ == "__main__":
    main()
