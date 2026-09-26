# Sürüm 2.0 / 2.1'e geçiş rehberi

Bu sürümle gelenler: **yazma çalışması**, **test (aralıklı tekrar)**, **ilerleme**, **Premium (Google Play satın alma)**, **10.384 kanjilik sözlük katmanı**, **parçaya göre arama**, KanjiVG atfı ve GitHub bağış ayarı.

> Sürüm 2.1 notu: "Destek ol" (bahşiş) ürünü kaldırıldı; Play Console'da yalnızca `premium_unlock` ürününü oluşturman yeterli.

Kısa cevap: **GitHub ayarlarını (Pages, Actions izni, config adresi) yeniden girmen gerekmez.** Sadece dosyaları değiştirip commit etmen yeterli.

---

## 1. GitHub

### Seçenek A – GitHub Desktop (önerilen)
1. Bilgisayarındaki depo klasörünü aç (ör. `Belgeler\GitHub\piopionihongo`).
2. `kanji-kartlari-v2.0.zip` dosyasını **Tümünü Ayıkla** ile ayrı bir klasöre aç.
3. Açılan `kanji-kartlari` klasörünün **içindeki her şeyi** (gizli `.github` dahil) depo klasörüne kopyala → **Değiştir** de.
   - `.git` klasörüne dokunma.
4. GitHub Desktop → özet: `Sürüm 2.0` → **Commit to main** → **Push origin**.
5. **Actions** sekmesinde "Veriyi derle" yeşil olmalı. Sonra şu adres açılmalı:
   `https://bollattime.github.io/piopionihongo/strokes.js`

### Seçenek B – Tarayıcı
**Add file → Upload files** ile klasörleri sürükle (önceki gibi). Yeni/değişen dosyalar:
```
data/strokes/strokes.json            (yeni)
data/dict/*.json                     (yeni: sözlük katmanı ve kanji parçaları)
data/config.json                     (değişti: freeLevels, playUrl, donateUrl)
data/i18n/tr.json, en.json           (değişti)
src/app.template.html                (değişti)
src/privacy.html                     (değişti)
tools/build.py                       (değişti)
android/app/build.gradle.kts         (değişti)
android/app/src/main/java/.../MainActivity.java (değişti)
.github/FUNDING.yml                  (yeni – gizli klasör, elle eklemen gerekebilir)
README.md, GELISTIRICI.md, KAYNAKLAR.md, LICENSE-DATA.md, GUNCELLEME-v2.md
```
`docs/` ve `android/app/src/main/assets/` klasörlerini Actions kendisi üretir; yine de yüklemen sorun olmaz.

### Bağış bağlantısı (isteğe bağlı)
- **GitHub "Sponsor" düğmesi:** `.github/FUNDING.yml` dosyasında kullandığın hizmetin satırındaki `#` işaretini kaldırıp kendi bilgini yaz.
- **Web sürümündeki bağış düğmesi:** `data/config.json` → `"donateUrl": "https://..."`
- Android uygulamasında dış bağlantı gösterilmez; orada destek Google Play üzerinden alınır (Play kuralları).

---

## 2. Android Studio
1. Android Studio'yu kapat.
2. `KanjiKartlar\app` içinde `src` klasörünü ve `build.gradle.kts` dosyasını sil.
3. `KanjiKartlar-app-v2.zip` içeriğini `KanjiKartlar` klasörüne ayıkla (içinde `app` klasörü var). PowerShell ile:
   ```powershell
   $zip  = "$env:USERPROFILE\Downloads\KanjiKartlar-app-v2.zip"
   $proj = "$env:USERPROFILE\AndroidStudioProjects\PioPioNihongo"
   Remove-Item "$proj\app\src","$proj\app\build" -Recurse -Force -ErrorAction SilentlyContinue
   Expand-Archive -Path $zip -DestinationPath $proj -Force
   Test-Path "$proj\app\src\main\assets\strokes.js"
   ```
   Son satır `True` vermeli.
4. Android Studio → **File → Sync Project with Gradle Files**. Gradle, satın alma kütüphanesini (`com.android.billingclient:billing:9.1.0`) kendisi indirir.
5. ▶ ile çalıştır. Satın alma düğmesi emülatörde "Google Play ödeme hizmetine ulaşılamıyor" diyebilir; bu normal (bkz. 4. bölüm).
6. **Build → Generate Signed App Bundle** → **aynı** `C:\anahtarlar\kanji-upload.jks` → release.
   Bu sürümde `versionCode = 3`, `versionName = "2.0"`.

---

## 3. Play Console
1. **Ödeme profili:** Satış yapabilmek için Play Console → **Ayarlar → Ödeme profili** üzerinden bir satıcı (ödeme) profili oluştur. Vergi bilgileri istenebilir.
2. **AAB'yi yükle:** Önce kapalı teste yeni sürümü yükle (Play, uygulama içi ürün oluşturmak için satın alma kütüphanesi içeren bir sürümün yüklenmiş olmasını ister).
3. **Ürünleri oluştur:** **Para kazanma → Ürünler → Tek seferlik ürünler** (veya Uygulama içi ürünler):
   | Ürün kimliği | Ad | Not |
   |---|---|---|
   | `premium_unlock` | Premium | Tek seferlik; uygulama bunu tüketmez, bir kez alınır |
   - Fiyatı TL veya USD olarak gir; diğer ülkeler için **Fiyatları dönüştür** de.
   - Ürünleri **Etkinleştir**. Kimlikler kodla birebir aynı olmalı.
4. **Uygulama içeriği:** Veri güvenliği formunu gözden geçir. Uygulama ödeme bilgisi görmez; satın alma işlemini Google Play yapar ve ilerleme verisi cihazda kalır. Formu Google'ın güncel yönergesine göre güncelle.
5. **Mağaza açıklaması:** Yeni özellikleri ekle. "Uygulama içi satın alma içerir" etiketi otomatik çıkar.

---

## 4. Satın almayı test etme
1. **Play Console → Ayarlar → Lisans testi** → kendi Gmail adresini ve test kullanıcılarını ekle.
2. Uygulamayı **Play Store'daki test bağlantısından** kur (Android Studio'dan kurulan sürümde satın alma genelde çalışmaz; paket adı ve imza Play'dekiyle eşleşmeli).
3. Lisans test kullanıcıları gerçek para ödemeden "test kartı" ile satın alma yapabilir.
4. Kontrol et:
   - Premium alınca N4–N1 yazma ve test açılıyor mu?
   - Uygulamayı silip yeniden kurunca **Satın alımları geri yükle** Premium'u geri getiriyor mu?
   - Sözlük (JLPT dışı kanjiler) ve parçaya göre arama ücretsiz çalışıyor mu?

### Web'de deneme
Premium özellikleri tarayıcıda denemek için adresin sonuna `#premium` ekle:
`https://bollattime.github.io/piopionihongo/#premium`
(Yalnızca web sürümünde çalışır; Android'de etkisi yoktur.)

---

## Ayarlanabilir şeyler (`data/config.json`)
```json
{
  "remoteBaseUrl": "https://bollattime.github.io/piopionihongo/",
  "minAppVersion": 1,
  "freeLevels": ["N5"],
  "playUrl": "https://play.google.com/store/apps/details?id=com.piopionihongo.app",
  "donateUrl": ""
}
```
- `freeLevels`: ücretsiz seviyeler. Örneğin `["N5", "N4"]` yaparsan N4 de ücretsiz olur; Play güncellemesi gerekmez.
- Yazma verisinde bir çizgiyi düzeltmek istersen `data/strokes/strokes.json` içindeki satırı düzenle (SVG yol verisi).
