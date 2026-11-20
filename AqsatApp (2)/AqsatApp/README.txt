مدیریت جامع اقساط و بدهی‌ها — پروژهٔ Android Studio
1) Android Studio (Koala یا جدیدتر) > File > Open > پوشهٔ AqsatApp
2) صبر کنید Gradle Sync تمام شود (اگر پیام ساخت Gradle Wrapper آمد، Yes بزنید)
3) Run ▶ روی گوشی/شبیه‌ساز  یا  Build > Build APK(s)
- رابط برنامه: app/src/main/assets/index.html
- اعلان‌ها: Notifier.kt (AlarmManager)؛ خروجی فایل‌ها: MainActivity.kt (saveFile)
- ساخت تصویر/PDF/Excel به اینترنت نیاز دارد (کتابخانه‌ها از cdnjs). برای کار کامل آفلاین،
  html2canvas.min.js و xlsx.full.min.js را در assets بگذارید و آدرس script را محلی کنید.
