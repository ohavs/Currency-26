# ממיר מטבעות (Currency 26)

אפליקציית Android להמרת מטבעות עם ווידג'טים למסך הבית, עדכון שערים אוטומטי ומצב לא מקוון.
נבנית ב-Kotlin + Jetpack Compose (ווידג'טים ב-Glance), שערים מ-open.er-api.com.

## התקנה ועדכונים

- **התקנה ראשונה:** הורידו את ה-APK מהגרסה האחרונה ב-[Releases](https://github.com/ohavs/Currency-26/releases/latest).
- **עדכונים:** כל דחיפה ל-`main` מפרסמת גרסה חדשה אוטומטית, והאפליקציה מציעה לעדכן בעצמה
  (באנר במסך הראשי, או הגדרות ← עדכוני אפליקציה).
- APK לבדיקה מכל ענף אחר: **Actions** ← הריצה של "Android build" ← **Artifacts**.

## בנייה מקומית

- **Android Studio:** File ← Open ← תיקיית הפרויקט, ואז Run.
- **שורת פקודה** (JDK 17 ומעלה + Android SDK):
  ```
  ./gradlew assembleDebug        # APK ב-app/build/outputs/apk/debug/
  ./gradlew testDebugUnitTest    # בדיקות יחידה
  ```

## חתימה

כל ה-APK-ים (debug ו-release) נחתמים במפתח המשותף `debug.keystore` שבריפו, כדי שכל גרסה תתעדכן מעל הקודמת.
המפתח ציבורי (הריפו פתוח), ולכן הוא לא מגן מפני APK מזויף שמישהו ישכנע אתכם להתקין ידנית -
העדכונים בתוך האפליקציה מגיעים רק מה-Releases של הריפו הזה.
אפשר לעבור למפתח פרטי (`my-upload-key.jks` או `KEYSTORE_PATH` + `STORE_PASSWORD` + `KEY_PASSWORD`),
אבל המעבר דורש התקנה מחדש פעם אחת. אל תעלו קובץ `.jks` לגיט.
