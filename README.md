# ממיר מטבעות (Currency 26)

אפליקציית Android להמרת מטבעות עם ווידג'טים למסך הבית, עדכון שערים אוטומטי ומצב לא מקוון.
נבנית ב-Kotlin + Jetpack Compose (ווידג'טים ב-Glance), שערים מ-open.er-api.com.

## הורדת APK בלי להתקין כלום

כל דחיפה ל-GitHub בונה APK אוטומטית:
**Actions** ← הריצה האחרונה של "Android build" ← **Artifacts** ← `currency-26-debug-apk`.

## בנייה מקומית

- **Android Studio:** File ← Open ← תיקיית הפרויקט, ואז Run.
- **שורת פקודה** (JDK 17 ומעלה + Android SDK):
  ```
  ./gradlew assembleDebug        # APK ב-app/build/outputs/apk/debug/
  ./gradlew testDebugUnitTest    # בדיקות יחידה
  ```

## חתימה

- **Debug:** הקובץ `debug.keystore` נמצא בריפו בכוונה (מפתח debug סטנדרטי, לא סודי),
  כך שכל APK - מ-GitHub או מכל מחשב - מתעדכן מעל הקודם.
- **Release:** שימו את מפתח ההעלאה ב-`my-upload-key.jks` בתיקייה הראשית (או הגדירו `KEYSTORE_PATH`),
  יחד עם `STORE_PASSWORD` ו-`KEY_PASSWORD`. בלי המפתח ה-release נבנה לא חתום. אל תעלו את קובץ ה-`.jks` לגיט.
