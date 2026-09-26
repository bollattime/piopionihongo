# Kaynaklar ve Atıflar / Sources and Attribution

## Veri kaynakları

| Veri | Kaynak | Lisans |
|---|---|---|
| Kanji listesi ve JLPT seviyeleri | [kanji-data](https://github.com/davidluzgouveia/kanji-data) – David Gouveia. Seviyeler [Jonathan Waller'ın JLPT listelerine](http://www.tanos.co.uk/jlpt/) dayanır. | MIT (kanji-data) |
| On/kun okunuşları, İngilizce kanji anlamları, klasik (Kangxi) radikaller | [KANJIDIC2](https://www.edrdg.org/wiki/index.php/KANJIDIC_Project) – Electronic Dictionary Research and Development Group (EDRDG) | CC BY-SA 4.0 |
| Örnek kelimeler, okunuşları, İngilizce anlamları | [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) – EDRDG; JSON sürümü [jmdict-simplified](https://github.com/scriptin/jmdict-simplified) (scriptin), sürüm 3.6.2, sözlük tarihi 2026-09-14 | CC BY-SA 4.0 |
| Sözlük katmanı: JLPT dışı 8.172 kanjinin okunuş ve İngilizce anlamları | [KANJIDIC2](https://www.edrdg.org/wiki/index.php/KANJIDIC_Project) – EDRDG | CC BY-SA 4.0 |
| Sözlük katmanı örnek kelimeleri | [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) – EDRDG | CC BY-SA 4.0 |
| Örnek cümleler (Japonca cümle + İngilizce çeviri) | [Tatoeba](https://tatoeba.org) / Tanaka derlemesi, [tatoeba-json](https://github.com/mwhirls/tatoeba-json) sürüm v0.0.52 | CC BY 2.0 FR |
| Yazma çalışması: çizgi sırası, çizgi şekilleri ve kanji parçaları (parçaya göre arama) | [KanjiVG](https://kanjivg.tagaini.net) – Ulrich Apel, sürüm r20250816 ([GitHub](https://github.com/KanjiVG/kanjivg)) | CC BY-SA 3.0 |
| Türkçe kanji ve kelime anlamları, radikal adları, N5/N4 kelime seçimi | Bu proje için hazırlandı (Claude yardımıyla) | CC BY-SA 4.0 (türetilmiş veri) |
| Gramer konuları, günlük konuşmalar ve ifadeler, günlük kelime listesi, örnek cümlelerin Türkçe çevirileri | Bu proje için hazırlandı (Claude yardımıyla) | CC BY-SA 4.0 |
| Yazı tipleri: Noto Serif JP, Zen Kaku Gothic New, Manrope | Google Fonts | SIL Open Font License 1.1 |

### Notlar

- JLPT 2010'dan beri resmî bir kanji listesi yayımlamıyor. Seviyeler yaygın kabul gören ama gayriresmî listelere dayanır. Kaynak listede yer almayan 分 N5'e eklendi; kaynak 耳'ı N3 olarak verdiği için o seviyede duruyor.
- N3–N1 örnek kelimeleri JMdict'teki yaygın kelimelerden otomatik seçildi, sonra elle ayıklandı. N5–N4 kelimeleri elle seçildi.
- Sözlük katmanındaki 8.172 kanjinin İngilizce anlamları KANJIDIC2'den gelir; Türkçe anlamları bu proje için hazırlanmıştır (171 jōyō kanjisi tek tek, kalanlar terim terim çevrilerek).
- Parçaya göre arama, KanjiVG'deki `kvg:element` bilgisinden üretilmiştir.
- Örnek cümlelerin Japoncası ve İngilizcesi Tatoeba'dan gelir (CC BY 2.0 FR, atıf zorunlu). Türkçe çevirileri bu proje için hazırlanmıştır (`tr` alanı).
- EDRDG lisansı, mobil uygulamalarda kaynağın menüden ulaşılan ayrı bir ekranda belirtilmesini ister. Uygulamada bu, **Ayarlar → Kaynaklar ve lisans** ekranındadır.
- Jonathan Waller'ın listelerinin kullanım koşullarını tanos.co.uk üzerinden ayrıca kontrol etmeni öneririm.

## Zorunlu EDRDG ibaresi

> This application uses the JMdict/EDICT and KANJIDIC dictionary files. These files are the property of the Electronic Dictionary Research and Development Group, and are used in conformance with the Group's licence (https://www.edrdg.org/edrdg/licence.html).

## Telif

- KANJIDIC2 ve JMdict © James William Breen ve Electronic Dictionary Research and Development Group.
- KanjiVG © 2009–2025 Ulrich Apel, Creative Commons Attribution-Share Alike 3.0 (http://kanjivg.tagaini.net). `data/strokes/strokes.json` bu veriden sayılar yuvarlanarak türetilmiştir.
- kanji-data © 2019 David Gouveia (MIT License).
- Türkçe çeviriler ve derleme © 2026 Pio Pio Nihongo, CC BY-SA 4.0 ile.

Bu dosyadaki metni uygulama mağazası açıklamasına ve web sitesine de ekleyebilirsin.
