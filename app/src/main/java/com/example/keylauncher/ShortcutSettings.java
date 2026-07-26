package com.example.keylauncher;

/**
 * מנהל את שיוך מקשי הספרות (0-9) לאפליקציות ספציפיות
 * (קיצור מקשים מותאם אישית, בסגנון "חיוג מהיר" לטלפוני-מקשים).
 *
 * לכל ספרה (0-9) אפשר לשייך packageName אחד. אם אין שיוך,
 * MainActivity נופל חזרה להתנהגות ברירת המחדל (מיקוד לפי
 * המיקום ה-N ברשת).
 */
public class ShortcutSettings {

    private final PreferenceStore prefs;

    private static final String KEY_PREFIX = "shortcuts.digit.";

    public ShortcutSettings(PreferenceStore prefs) {
        this.prefs = prefs;
    }

    /**
     * מחזיר את שם החבילה המשויכת לספרה, או null אם אין שיוך.
     */
    public String getPackageForDigit(int digit) {

        if (digit < 0 || digit > 9) {
            return null;
        }

        return prefs.getString(keyFor(digit), null);
    }

    /**
     * משייך אפליקציה (לפי packageName) לספרה מסוימת (0-9).
     */
    public void setPackageForDigit(int digit, String packageName) {

        if (digit < 0 || digit > 9) {
            return;
        }

        if (packageName == null || packageName.trim().isEmpty()) {
            clearDigit(digit);
            return;
        }

        prefs.putString(keyFor(digit), packageName);
    }

    /**
     * מבטל שיוך לספרה מסוימת.
     */
    public void clearDigit(int digit) {

        if (digit < 0 || digit > 9) {
            return;
        }

        prefs.remove(keyFor(digit));
    }

    public boolean hasShortcut(int digit) {
        return getPackageForDigit(digit) != null;
    }

    /**
     * מבטל את כל השיוכים (0-9).
     */
    public void clearAll() {

        for (int digit = 0; digit <= 9; digit++) {
            clearDigit(digit);
        }
    }

    private String keyFor(int digit) {
        return KEY_PREFIX + digit;
    }

    public void restoreDefaults() {
        clearAll();
    }

}
