# Pio Pio Nihongo – Geliştirici rehberi

Japonca öğrenme uygulaması (eski adı Kanji Kartları): JLPT N5–N1 kanji kartları, sözlük, kelimeler, gramer, günlük konuşmalar, yazma çalışması ve test; Türkçe/İngilizce arayüz.
Veriler GitHub'da tutulur; Google Play'deki uygulama açılışta yeni veri olup olmadığına bakar ve indirir.

```
data/ düzenle ──push──▶ GitHub Actions (tools/build.py) ──▶ docs/ ──▶ GitHub Pages
                                                                     │
          Google Play'deki uygulama ◀── version.json + data.json ────┘
          (internet yoksa kendi içindeki veriyle çalışır)
```

## Klasörler

| Klasör / dosya | Ne işe yarar |
|---|---|
| `data/kanji/n5.json … n1.json` | Kanjiler, **her satır bir kanji** |
| `data/vocabulary/n5.json … n1.json` | Örnek kelimeler, **her satır bir kelime** |
| `data/radicals/radicals.json` | 194 radikal |
| `data/strokes/strokes.json` | Yazma çalışması için çizgi verisi (KanjiVG), her satır bir kanji |
| `data/dict/kanji.json` | Sözlük katmanı: JLPT dışı 8.172 kanji |
| `data/dict/vocabulary.json` | Sözlük katmanı örnek kelimeleri (İngilizce) |
| `data/dict/strokes.json` | Sözlük katmanı çizgi verisi |
| `data/dict/components.json` | Kanji parçaları (parçaya göre arama) |
| `data/dict/words.json` | Kelime sözlüğü: 50.000 kelime (`w`, `r`, `pos`, `en`, `tr`) |
| `data/sentences/dict-sentences.json` | Sözlük kelimelerinin örnek cümleleri |
| `data/sentences/sentences.json` | Örnek cümleler (Tatoeba): `word`, `ja`, `en`, isteğe bağlı `tr` |
| `data/grammar/n5.json … n1.json` | Gramer konuları (açıklama, örnek cümleler, test soruları) |
| `data/daily/conversations.json`, `phrases.json`, `words.json` | Günlük Japonca: konuşmalar, ifadeler, sık kelimeler |
| `data/i18n/tr.json`, `en.json` | Arayüz metinleri |
| `data/config.json` | GitHub Pages adresi, en düşük uygulama sürümü, ücretsiz seviyeler (`freeLevels`), Play ve bağış bağlantıları, destek e-postası (`supportEmail`: Hata bildir ve gizlilik politikası bunu kullanır) |
| `src/app.template.html` | Uygulama arayüzü (veri içermez) |
| `src/privacy.html` | Gizlilik politikası |
| `tools/build.py` | `data/` → `docs/` ve Android `assets/` |
| `docs/` | GitHub Pages'te yayınlanan dosyalar (otomatik üretilir): `index.html`, `data.json`, `strokes.js`, `dict.js`, `version.json`, `privacy.html` |
| `android/app/` | Android Studio uygulama modülü |
| `.github/workflows/build.yml` | `data/` değişince `docs/`'u otomatik günceller |
| `store/` | Play mağazası simgesi (512×512), tanıtım görselleri (1024×500, TR/EN), mağaza metinleri ve yayın rehberi |

---

## 1. GitHub kurulumu

### 1.1 Depoyu oluştur
1. GitHub'da **New repository** → ad örn. `kanji-kartlari` → **Public** → oluştur.
   (GitHub Pages'in ücretsiz planda çalışması için depo herkese açık olmalı.)
2. Bu klasörü yükle:
   ```bash
   cd kanji-kartlari
   git init
   git add .
   git commit -m "İlk sürüm"
   git branch -M main
   git remote add origin https://github.com/KULLANICI/kanji-kartlari.git
   git push -u origin main
   ```
   `.gitignore` imza anahtarlarını (`*.jks`, `*.keystore`) ve derleme çıktılarını dışarıda bırakır.

### 1.2 Adresi ayarla
`data/config.json` dosyasında `KULLANICI` ve `DEPO` kısımlarını değiştir:
```json
{
  "remoteBaseUrl": "https://KULLANICI.github.io/kanji-kartlari/",
  "minAppVersion": 1
}
```
Sonra bilgisayarında bir kez `python3 tools/build.py` çalıştırıp commit + push yap.
Bu adres Android uygulamasının içine de yazılır, bu yüzden **AAB almadan önce** yapılmalı.

### 1.3 GitHub Pages'i aç
Depo → **Settings → Pages** → *Build and deployment*:
- Source: **Deploy from a branch**
- Branch: **main**, klasör: **/docs** → **Save**

Birkaç dakika sonra şu adresler çalışmalı:
- `https://KULLANICI.github.io/kanji-kartlari/` → web sürümü
- `https://KULLANICI.github.io/kanji-kartlari/version.json` → veri sürümü
- `https://KULLANICI.github.io/kanji-kartlari/privacy.html` → gizlilik politikası (Play için)

### 1.4 Otomatik derlemeye izin ver
Depo → **Settings → Actions → General → Workflow permissions** → **Read and write permissions** → Save.

Artık `data/` altındaki bir dosyayı GitHub web arayüzünde düzenleyip kaydettiğinde (commit) Actions otomatik çalışır, `docs/` güncellenir ve veri sürümü bir artar. Kullanıcıların uygulaması bir sonraki açılışta yeni veriyi indirir.
Hatalı bir satır varsa Actions kırmızı olur ve hangi dosyanın hangi satırında sorun olduğunu gösterir; yayındaki veri bozulmaz.

---

## 2. Android Studio'da proje ve AAB

### 2.1 Projeyi oluştur
1. Android Studio → **New Project → No Activity** (veya *Empty Views Activity*)
   - Name: `Pio Pio Nihongo`
   - Package name: `com.piopionihongo.app` (Play'deki uygulamanın kimliği; ilk yüklemeden sonra **asla değiştirilemez**)
   - Language: **Java**
   - Minimum SDK: **API 24**
2. Oluşan projede **`app/src` klasörünü tamamen sil**, yerine bu depodaki `android/app/src` klasörünü kopyala.
3. `app/build.gradle.kts` dosyasını bu depodaki `android/app/build.gradle.kts` ile değiştir.
   - Şablonun `plugins { … }` ve `compileSdk` satırları farklı yazılmışsa (yeni Android Studio sürümlerinde olabilir) şablondakini bırak; değerlerin aynı olduğundan emin ol: `compileSdk 36`, `targetSdk 36`, `minSdk 24`, `buildConfig = true`.
   - `dependencies { … }` bloğundaki **Google Play Billing** satırını (`com.android.billingclient:billing`) **silme**; Premium satın alma bununla çalışır. Android Studio daha yeni bir sürüm önerirse kabul edebilirsin (en az 8.x olmalı).
   - Şablondan gelen diğer bağımlılıklar (appcompat, material vb.) gerekmez, silebilirsin.
4. **File → Sync Project with Gradle Files**. Android Studio güncelleme önerirse (AGP, Gradle) kabul et.
5. Telefonu bağlayıp ▶ ile çalıştır ve dene.

> İpucu: Android Studio projesini doğrudan bu deponun `android/` klasörüne oluşturursan `tools/build.py` asset'leri doğrudan projeye yazar, kopyalama gerekmez.

### 2.2 Sürüm numaraları
Her Play yüklemesinde `app/build.gradle.kts` içinde **iki** değer değişir:

| Aşama | versionName (kullanıcı görür) | versionCode (Play bakar, hep artar) |
|---|---|---|
| Kapalı test ilk yükleme | `0.9` | `44` |
| Testte düzeltme | `0.91`, `0.92`, `0.93` … | `45`, `46`, `47` … |
| Yayın | `1.0` | bir öncekinden büyük (ör. `50`) |
| Sonraki güncellemeler | `1.1`, `1.2` … | her seferinde +1 |

- `versionCode` **asla geri gitmez ve tekrar kullanılamaz**; Play aynı ya da küçük sayıyı reddeder.
- Veri (kelime, anlam vb.) değişince yeni sürüm yüklemek **gerekmez**; uygulama yeni veriyi GitHub'dan kendisi alır. Yeni sürüm yalnızca **kod** değiştiğinde gerekir.
- Eski uygulamanın okuyamayacağı bir veri değişikliği yaptıysan `data/config.json` → `minAppVersion`'u yeni `versionCode` yap. Eski sürümdeki kullanıcılar "Google Play'den güncelle" mesajı görür ve eski veriyle devam eder.

### 2.3 İmzalı AAB (App Bundle) al
1. **Build → Generate Signed App Bundle or APK → Android App Bundle → Next**
2. İlk seferde **Create new…** ile bir *upload key* oluştur (ör. `piopio-upload.jks`).
   - Dosyayı ve şifrelerini **güvenli bir yerde yedekle** (ör. şifre yöneticisi + USB bellek). **Depoya koyma** (`.gitignore` zaten `*.jks` dosyalarını dışarıda tutar).
   - Sonraki yüklemelerde **aynı** anahtarı seç (**Choose existing…**).
3. Build variant: **release** → **Create**
4. Çıktı: `app/release/app-release.aab` → Play Console'a bu dosya yüklenir (APK değil).

---

## 3. Google Play'e yükleme

Ayrıntılı, adım adım rehber ve mağaza metinleri: **`store/PLAY-YAYIN-REHBERI.md`** ve **`store/magaza-metinleri.md`**.

Kısaca:
1. **Create app** → ad `Pio Pio Nihongo`, varsayılan dil Türkçe, *App*, *Free* (uygulama ücretsiz, içinde satın alma var).
2. **Play App Signing**'i kabul et (varsayılan).
3. **Uygulama içeriği** formları: gizlilik politikası `https://bollattime.github.io/piopionihongo/privacy.html`, veri güvenliği, reklam yok, içerik derecelendirme, hedef kitle.
4. **Uygulama içi ürün**: *Monetize → Products → In-app products* → ürün kimliği **`premium_unlock`** (kodla birebir aynı olmalı), tek seferlik.
5. **Kapalı test** (13 Kasım 2023'ten sonra açılan kişisel hesaplar için zorunlu): en az **12 test kullanıcısı**, **14 gün** kesintisiz.
6. 14 gün dolunca **Dashboard → Apply for production** → onaydan sonra test sürümünü **Production**'a yükselt (promote) veya yeni sürüm yükle.

---

## 4. Günlük kullanım

| Yapmak istediğin | Nasıl |
|---|---|
| Bir kelimenin anlamını düzeltmek | GitHub'da `data/vocabulary/nX.json` → ilgili satırı düzenle → Commit. Gerisi otomatik. |
| Kanjiye kelime eklemek | Aynı dosyada o kanjinin satırlarının arasına yeni satır ekle. Son satırdan sonra virgül olmamalı. |
| Yerelde kontrol | `python3 tools/build.py --check` |
| Arayüz metnini değiştirmek | `data/i18n/tr.json` / `en.json` |
| Web sürümünü görmek | `docs/index.html`'i tarayıcıda aç veya Pages adresine git |

### Veri biçimi
```json
{"kanji": "日", "level": "N5", "radical": "日", "on": ["ニチ", "ジツ"], "kun": ["ひ", "か"], "easy": "ひ", "meaning": {"tr": "gün, güneş", "en": "day, sun, Japan"}},
{"kanji": "日", "word": "毎日", "reading": "まいにち", "meaning": {"tr": "her gün", "en": "every day"}},
```
- `radical`: `radicals.json` içindeki `key` değerlerinden biri.
- `easy`: favorilerde gösterilen ve sesli okunan okunuş.
- Kelimeler kartta dosyadaki sırayla görünür.

## Güncelleme mekanizması (teknik)
- `tools/build.py` veri içeriğinin SHA-256 özetini çıkarır; içerik değiştiyse `dataVersion`'u bir artırır.
- Uygulama açılıştan ~1,5 sn sonra `version.json`'u indirir. `dataVersion` elindekinden büyükse `data.json`'u indirir, SHA-256 özetini ve yapısını doğrular, cihazda saklar ve ekranı yeniler.
- Doğrulama başarısız olursa veya internet yoksa eldeki veri kullanılmaya devam eder.
- Ayarlar → Veri bölümünden sürüm görülebilir ve elle denetlenebilir.

## Lisans
- Veriler: CC BY-SA 4.0 (`KAYNAKLAR.md`, `LICENSE-DATA.md`)
- Kod: proje sahibine ait; istersen `LICENSE` dosyası olarak MIT ekleyebilirsin.
