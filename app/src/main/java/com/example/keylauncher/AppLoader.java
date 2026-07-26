package com.example.keylauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * טוען את רשימת האפליקציות המותקנות במכשיר כ-LauncherItem,
 * ממויינות לפי א"ב עברי, עם כותרות מותאמות אישית וסימון הסתרה
 * לפי ההגדרות (SettingsManager).
 */
public class AppLoader {

    private final PackageManager packageManager;
    private final SettingsManager settings;

    public AppLoader(PackageManager packageManager,
                      SettingsManager settings) {

        this.packageManager = packageManager;
        this.settings = settings;
    }

    /**
     * טוען את כל האפליקציות, כולל המוסתרות (מסומנות עם isHidden=true).
     */
    public List<LauncherItem> loadApplications() {

        List<LauncherItem> result = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps =
                packageManager.queryIntentActivities(intent, 0);

        final Collator collator = Collator.getInstance(new Locale("he"));

        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return collator.compare(
                        a.loadLabel(packageManager).toString(),
                        b.loadLabel(packageManager).toString()
                );
            }
        });

        long id = 0;

        for (ResolveInfo info : apps) {

            String packageName = info.activityInfo.packageName;
            boolean hidden = settings.apps.isHidden(packageName);

            LauncherItem item = new LauncherItem();

            item.setId(id++);
            item.setType(LauncherItem.TYPE_APP);

            String customTitle = settings.apps.getCustomTitle(packageName);

            item.setTitle(
                    customTitle != null
                            ? customTitle
                            : info.loadLabel(packageManager).toString()
            );

            item.setPackageName(packageName);
            item.setClassName(info.activityInfo.name);
            item.setIcon(info.loadIcon(packageManager));
            item.setHidden(hidden);

            result.add(item);

        }

        return result;
    }

    /**
     * טוען את האפליקציות, עם אפשרות לכלול או להסתיר את המוסתרות.
     */
    public List<LauncherItem> loadApplications(boolean includeHidden) {

        List<LauncherItem> all = loadApplications();

        if (includeHidden) {
            return all;
        }

        List<LauncherItem> visible = new ArrayList<>();

        for (LauncherItem item : all) {

            if (!item.isHidden()) {
                visible.add(item);
            }

        }

        return visible;
    }

}
