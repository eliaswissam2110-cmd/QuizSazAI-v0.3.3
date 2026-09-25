# آزمون‌ساز هوشمند — نسخه 0.3 (بازسازی‌شده)

این نسخه یک پروژه کامل Android Studio است که ساختار استاندارد ماژول `app` و Workflow آماده برای GitHub Actions دارد.

## ساختار

- `app/` — ماژول اصلی اندروید
- `app/src/main/java/ir/quizzesaz/ai/MainActivity.kt` — رابط و منطق فعلی برنامه
- `app/src/main/res/values/styles.xml` — تم اندروید
- `app/src/main/AndroidManifest.xml` — Manifest
- `.github/workflows/android.yml` — Build خودکار APK در GitHub Actions
- `build.gradle.kts` — نسخه پلاگین‌های ریشه
- `app/build.gradle.kts` — تنظیمات و وابستگی‌های ماژول
- `settings.gradle.kts` — تنظیمات پروژه

## نکات مربوط به CI

Workflow از JDK 17 و Gradle 8.9 استفاده می‌کند و Android SDK برای API 35 و Build Tools 35.0.0 را نصب می‌کند.

برای ساخت APK دیباگ، Workflow دستور زیر را اجرا می‌کند:

```text
gradle clean assembleDebug --stacktrace
```

فایل خروجی به‌عنوان Artifact با نام `QuizSazAI-debug-apk` منتشر می‌شود.

## قابلیت‌های فعلی

- داشبورد
- افزودن و حذف جزوه
- ذخیره محلی جزوه و نتایج
- ساخت آزمون
- تصادفی‌سازی ترتیب سؤال‌ها
- آزمون چهارگزینه‌ای
- ثبت صحیح/غلط/بی‌پاسخ
- درصد و زمان آزمون
- تاریخچه نتایج
- حالت روشن/تاریک
- رابط راست‌به‌چپ فارسی
- محل تنظیم API Key

این نسخه تولید واقعی سؤال با AI یا استخراج متن PDF/Word/OCR را انجام نمی‌دهد؛ این موارد در سورس اولیه نیز به‌عنوان مرحله بعد تعریف شده بودند.
