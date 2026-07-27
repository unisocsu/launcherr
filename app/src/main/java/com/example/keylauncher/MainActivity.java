package com.example.keylauncher;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.DataOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KeyLauncher_Root";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // הפעלת לוגיקת העכבר והמקשים עם הרשאות Root
        toggleMouseSupportAndEnable();
    }

    public void toggleMouseSupportAndEnable() {
        // הרצת פעולות Root בתוך Background Thread כדי לא לתקוע את ה-UI
        new Thread(new Runnable() {
            @Override
            public void run() { // 👈 כאן היה התיקון: שונה מ-.run() ל-public void run()
                Process process = null;
                DataOutputStream outputStream = null;
                try {
                    // פתיחת מעטפת Root
                    process = Runtime.getRuntime().exec("su");
                    outputStream = new DataOutputStream(process.getOutputStream());

                    // שליחת פקודת מערכת (לדוגמה: אירוע מקש 20)
                    outputStream.writeBytes("input keyevent 20\n");
                    outputStream.flush();

                    // סיום פקודות ויציאה מה-su
                    outputStream.writeBytes("exit\n");
                    outputStream.flush();

                    process.waitFor();
                    Log.d(TAG, "Root commands executed successfully!");

                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Failed to execute root command (Maybe root is missing or denied?)", e);
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (process != null) {
                            process.destroy();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
