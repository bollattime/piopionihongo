# Pio Pio Nihongo – Google Play'e yükleme rehberi (adım adım)

Sıra: **AAB al → uygulamayı oluştur → formları doldur → mağaza sayfası → Premium ürünü → kapalı test (12 kişi, 14 gün) → üretim başvurusu → 1.0 yayın**

---

## 0. Hazırlık (bir kez)
- [x] **Destek e-postası:** `support@piopionihongo.com` (uygulamaya ve gizlilik politikasına eklendi; `data/config.json` → `"supportEmail"`).
- [ ] **E-posta yönlendirmesini kur ve dene:** alan adına gelen postanın gerçekten ulaştığını kendine bir deneme e-postası atarak kontrol et (Play bu adrese doğrulama e-postası gönderebilir).
- [ ] Depoyu GitHub'a gönder (push). GitHub Pages açık olmalı: https://bollattime.github.io/piopionihongo/privacy.html açılıyor mu, kontrol et.
- [ ] **Ödeme profili (merchant account):** Premium satmak için Play Console → *Setup → Payments profile* doldurulmalı (Japonya adresi ve banka hesabı).

## 1. İmzalı AAB (Android Studio)
Ayrıntı: `GELISTIRICI.md` → 2.1 ve 2.3.
1. `android/app/build.gradle.kts`: `versionCode = 44`, `versionName = "0.9"` (zaten ayarlı).
2. **Build → Generate Signed App Bundle or APK → Android App Bundle**.
3. İlk seferde **Create new…** → `piopio-upload.jks`. Dosyayı ve şifreleri yedekle, **GitHub'a koyma**.
4. **release** → **Create** → `app/release/app-release.aab`.

## 2. Uygulamayı oluştur
Play Console → **Create app**
- App name: `Pio Pio Nihongo: Japonca`
- Default language: **Turkish – tr-TR**
- App or game: **App** · Free or paid: **Free** (içinde satın alma olabilir, sorun değil)
- Beyanları işaretle → **Create app**

## 3. Uygulama içeriği (Policy → App content)
| Form | Cevap |
|---|---|
| **Privacy policy** | `https://bollattime.github.io/piopionihongo/privacy.html` |
| **App access** | *All or some functionality is restricted* → **Add instructions**. Name: `Premium levels`. Kod alanına `pio-hediye-inceleme-kodu.txt` içindeki **inceleme kodunu** yaz (GitHub'a koyma). Talimat (İngilizce yaz): `N4–N1 levels are Premium. To unlock them without paying: open the menu (top left) → "Premium & support" → type the code in the "Gift code" box → tap "Redeem code". No login is needed.` |
| **Ads** | **No**, reklam yok |
| **Content rating** | Kategori: *Reference, News, or Educational*. Şiddet, cinsellik, küfür, kumar, uyuşturucu: hepsi **Hayır**. Kullanıcılar birbiriyle iletişim kuruyor mu: **Hayır**. Konum paylaşımı: **Hayır**. Dijital ürün satın alma: **Evet**. Sonuç büyük ihtimalle "3+ / Everyone" olur. |
| **Target audience** | **13–15, 16–17, 18 ve üzeri**. 13 yaş altını seçme; seçersen Aile politikası ek şartlar getirir. "Çocukların ilgisini çekebilir mi?" → Hayır. |
| **News app** | No |
| **Data safety** | Aşağıya bak |
| **Government app** | No |
| **Financial features** | *My app doesn't provide any financial features* |
| **Health** | Hiçbiri |
| **Advertising ID** | **No**, reklam kimliği kullanılmıyor |

### Data safety (Veri güvenliği)
- *Does your app collect or share any of the required user data types?* → **No**
  - İlerleme, notlar ve ayarlar yalnızca cihazda kalıyor (cihazdan çıkmayan veri beyan edilmez).
  - Ödemeyi Google Play topluyor; uygulama ödeme bilgisine erişmiyor. Bu durumda beyan gerekmiyor.
  - "Hata bildir", kullanıcının kendi e-posta uygulamasını açıyor. Kullanıcının kendi başlattığı bu gönderim "toplama" sayılmıyor.
- *Is all of the user data collected by your app encrypted in transit?* → Veri toplanmadığı için sorulmaz.
- *Account deletion* → Hesap yok.

## 4. Mağaza sayfası (Grow users → Store presence → Main store listing)
- Metinler: `store/magaza-metinleri.md` (TR ve EN).
- Simge: `store/icon-512.png`
- Öne çıkan görsel: `store/feature-graphic-1024x500-tr.png` (TR listeye) ve `-en.png` (EN listeye)
- Telefon ekran görüntüleri: tanıtım paketindeki 8 Google Play görseli (1080×1920)
- **Store settings:** Category **Education**, iletişim e-postası = `support@piopionihongo.com`

## 5. Premium ürünü (Monetize → Products → In-app products)
1. Önce ödeme profili tamamlanmış olmalı (0. adım).
2. **Create product**
   - Product ID: **`premium_unlock`** (koddaki kimlik; birebir aynı olmalı, sonradan değiştirilemez)
   - Name: `Pio Pio Nihongo Premium` · Description: `N4–N1 seviyelerini açar. Tek seferlik.`
   - Fiyat: sen belirle (ör. 199 JPY / 49,99 TRY). Play diğer ülkelere çevirir.
3. **Activate**.
4. **Lisans testi:** *Setup → License testing* → kendi Gmail'ini ve test kullanıcılarını ekle. Bu hesaplar Premium'u **ücret ödemeden** (test kartıyla) alabilir. Kendi telefonunda da Premium'u böyle açarsın.

> Not: Play'e hiç yüklenmemiş bir uygulamada ürün oluşturma ekranı açılmayabilir. Önce 6. adımdaki test sürümünü yükle, sonra ürünü oluştur.

## 6. Kapalı test (Test and release → Testing → Closed testing)
1. **Create track** (veya hazır *Closed testing – Alpha*).
2. **Testers:** Google Grubu ekle (ör. `piopio-test@googlegroups.com`) ya da e-posta listesi oluştur. 15–20 kişi ekle; 12 kişinin **14 gün kesintisiz** kayıtlı kalması şart.
3. **Countries/regions:** Türkiye, Japonya (ya da hepsi).
4. **Create new release** → `app-release.aab` (0.9 / 44) yükle → Release name `0.9` → sürüm notları (`magaza-metinleri.md`) → **Next → Save → Send for review**.
5. Onaydan sonra **"Join on the web" linkini** kopyala ve test kullanıcılarına gönder. Onlar linkten "Test kullanıcısı ol" der ve uygulamayı Play'den indirir.
6. Testte düzeltme gerekirse versionName `0.91` / versionCode `45`, sonra `0.92` / `46`… ile yeni AAB yükle. **14 günlük süre sıfırlanmaz**; önemli olan test kullanıcılarının testte kalması.

## 7. Üretim başvurusu ve 1.0
1. 14 gün dolunca **Dashboard → Apply for production**. Soruları dürüstçe cevapla: kaç kişi test etti, geri bildirim nasıl toplandı ("Hata bildir" formu, mesajlar), neyi düzelttin.
2. Onay genelde 7 gün içinde gelir.
3. `versionName = "1.0"`, versionCode bir öncekinden büyük (ör. `50`) → yeni AAB → **Production → Create new release** → yükle → yayınla.

---

## Önemli notlar
- **Telefonundaki test APK'sı** farklı bir anahtarla imzalı. Play'den kurmadan önce: uygulamada **Ayarlar → Bütün ilerlemeyi yedekle** → test uygulamasını kaldır → Play'den kur → **Yedeği geri yükle**.
- **Yükleme anahtarını (`.jks`) kaybetme.** Kaybolursa Play Console'dan sıfırlatılabilir ama birkaç gün sürer.
- **Hediye kodu anahtarı uygulamanın içinde.** Ücretsiz GitHub Pages için depo herkese açık (public) kalmalı; bu yüzden kodu okuyabilen biri hediye kodu üretebilir. Kötüye kullanım görürsen anahtarı değiştirip yeni sürüm çıkarırız; ileride uygulama kodunu ayrı, gizli bir depoya da taşıyabiliriz.
- Veri düzeltmeleri (anlam, cümle) için yeni sürüm gerekmez; GitHub'a gönderdiğinde uygulamalar kendisi günceller.
