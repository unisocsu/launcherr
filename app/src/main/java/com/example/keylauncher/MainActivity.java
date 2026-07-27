package com.example.keylauncher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * המסך הראשי של הלאנצ'ר. 📱✨
 */
public class MainActivity extends AppCompatActivity
        implements LauncherAdapter.OnItemMoveListener {

    public static final int APPWIDGET_HOST_ID = 1024;
    private static final String TAG = "MainActivity_Root";

    private static final int REQUEST_PICK_APPWIDGET = 1001;
    private static final int REQUEST_CREATE_APPWIDGET = 1002;

    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private LauncherAdapter launcherAdapter;
    private ItemTouchHelper itemTouchHelper;
    private FrameLayout widgetContainer;
    private Toolbar toolbar;
    private SettingsManager settings;
    private AppLoader appLoader;
    private DesktopLayoutManager desktopLayout;
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;

    private final List<LauncherItem> launcherItems = new ArrayList<>();
    private int pendingWidgetId = -1;
    private boolean showHiddenApps = false;
    private AppSearchDialog currentSearchDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // הפעלת הרשאות Root ברקע ישירות במיין אקטיביטי 👑🚀
        executeRootCommands();

        // הגדרת הטפט המערכתי כרקע 🖼️
        setSystemWallpaperAsBackground();

        initializeManagers();
        initializeRecyclerView();
        loadApplications();
        restoreWidgets();
    }

    private void executeRootCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                DataOutputStream outputStream = null;
                try {
                    process = Runtime.getRuntime().exec("su");
                    outputStream = new DataOutputStream(process.getOutputStream());
                    outputStream.writeBytes("input keyevent 20\n");
                    outputStream.flush();
                    outputStream.writeBytes("exit\n");
                    outputStream.flush();
                    process.waitFor();
                    Log.d(TAG, "Root commands executed successfully! 🚀");
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Failed to execute root command", e);
                } finally {
                    try {
                        if (outputStream != null) outputStream.close();
                        if (process != null) process.destroy();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void setSystemWallpaperAsBackground() {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            
            if (wallpaperDrawable != null) {
                View rootView = findViewById(R.id.rootLayout); 
                if (rootView != null) {
                    rootView.setBackground(wallpaperDrawable);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeManagers() {
        settings = new SettingsManager(this);
        appLoader = new AppLoader(getPackageManager(), settings);

        desktopLayout = new DesktopLayoutManager(settings);
        desktopLayout.load();

        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);

        widgetContainer = findViewById(R.id.widgetContainer);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initializeRecyclerView() {
        recyclerView = findViewById(R.id.appsRecycler);
        gridLayoutManager = new GridLayoutManager(this, settings.display.getGridColumns());
        recyclerView.setLayoutManager(gridLayoutManager);

        launcherAdapter = new LauncherAdapter(this, settings);
        launcherAdapter.setOnItemMoveListener(this);
        recyclerView.setAdapter(launcherAdapter);

        setupDragToReorder();
    }

    private void setupDragToReorder() {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN
                                | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                        0) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                   @NonNull RecyclerView.ViewHolder source,
                                   @NonNull RecyclerView.ViewHolder target) {
                int from = source.getAdapterPosition();
                int to = target.getAdapterPosition();

                if (from < 0 || to < 0 || from >= launcherItems.size() || to >= launcherItems.size()) {
                    return false;
                }

                Collections.swap(launcherItems, from, to);
                launcherAdapter.setItems(launcherItems);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(rv, viewHolder);
                desktopLayout.setItems(launcherItems);
                desktopLayout.save();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void loadApplications() {
        launcherItems.clear();
        launcherItems.addAll(appLoader.loadApplications(showHiddenApps));
        desktopLayout.setItems(launcherItems);
        launcherAdapter.setItems(launcherItems);
        requestInitialFocus();
    }

    private void requestInitialFocus() {
        recyclerView.post(() -> {
            if (recyclerView.getChildCount() == 0) return;
            View first = recyclerView.getChildAt(0);
            if (first != null && !hasAnyFocusedChild()) {
                first.requestFocus();
            }
        });
    }

    private boolean hasAnyFocusedChild() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            if (recyclerView.getChildAt(i).isFocused()) return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem showHiddenItem = menu.findItem(R.id.action_show_hidden);
        if (showHiddenItem != null) {
            showHiddenItem.setChecked(showHiddenApps);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_search) {
            showSearchDialog();
            return true;
        } else if (itemId == R.id.action_add_widget) {
            startAddWidgetFlow();
            return true;
        } else if (itemId == R.id.action_show_hidden) {
            showHiddenApps = !showHiddenApps;
            invalidateOptionsMenu();
            loadApplications();
            return true;
        } else if (itemId == R.id.action_reload) {
            loadApplications();
            Toast.makeText(this, "רשימת האפליקציות רועננה 🔄", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSearchDialog() {
        currentSearchDialog = new AppSearchDialog(this, launcherItems, settings);
        currentSearchDialog.show();
    }

    @Override
    public void onMoveRequested(LauncherItem item) {
        int position = launcherItems.indexOf(item);
        if (position < 0) return;

        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
        if (viewHolder != null) {
            Toast.makeText(this, "גררו את האייקון למיקום הרצוי 🖱️", Toast.LENGTH_SHORT).show();
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    private void restoreWidgets() {
        appWidgetHost.startListening();
        if (!settings.widgets.isWidgetsEnabled()) return;

        int lastWidgetId = settings.widgets.getLastWidgetId();
        if (lastWidgetId == -1) return;

        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(lastWidgetId);
        if (info == null) {
            settings.widgets.setLastWidgetId(-1);
            return;
        }

        showWidget(lastWidgetId, info);
    }

    private void startAddWidgetFlow() {
        int appWidgetId = appWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_APPWIDGET) {
            if (resultCode != RESULT_OK || data == null) return;

            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId == -1) return;

            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if (info == null) return;

            if (info.configure != null) {
                pendingWidgetId = appWidgetId;
                Intent configureIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                configureIntent.setComponent(info.configure);
                configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                startActivityForResult(configureIntent, REQUEST_CREATE_APPWIDGET);
            } else {
                showWidget(appWidgetId, info);
                persistWidgetId(appWidgetId);
            }
        } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
            if (resultCode != RESULT_OK || pendingWidgetId == -1) {
                if (pendingWidgetId != -1) {
                    appWidgetHost.deleteAppWidgetId(pendingWidgetId);
                }
                pendingWidgetId = -1;
                return;
            }

            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(pendingWidgetId);
            if (info != null) {
                showWidget(pendingWidgetId, info);
                persistWidgetId(pendingWidgetId);
            }
            pendingWidgetId = -1;
        }
    }

    private void showWidget(int appWidgetId, AppWidgetProviderInfo info) {
        AppWidgetHostView hostView = appWidgetHost.createView(this, appWidgetId, info);
        hostView.setAppWidget(appWidgetId, info);

        widgetContainer.removeAllViews();
        widgetContainer.addView(hostView);
        widgetContainer.setVisibility(View.VISIBLE);
    }

    private void persistWidgetId(int appWidgetId) {
        settings.widgets.setLastWidgetId(appWidgetId);
        settings.widgets.setWidgetCount(1);
        settings.widgets.setWidgetsEnabled(true);
    }

    /* ===========================
       מקשי חומרה (ניהול עכבר וירטואלי וחייגן) 📞🖱️
       =========================== */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (settings.keys.isHardwareKeysEnabled() && keyCode == KeyEvent.KEYCODE_CALL) {
            if (event.getRepeatCount() == 0) {
                event.startTracking(); // חובה כדי לאפשר זיהוי לחיצה ארוכה
            }
            return true;
        }

        int digit = digitFromKeyCode(keyCode);
        if (digit >= 0 && !searchDialogIsShowing()) {
            focusItemAtDigit(digit);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (settings.keys.isHardwareKeysEnabled() && keyCode == KeyEvent.KEYCODE_CALL) {
            // לחיצה ארוכה על מקש חיוג - פתיחת החייגן המובנה של המכשיר 📱
            startActivity(new Intent(Intent.ACTION_DIAL));
            return true;
        }

        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (settings.keys.isHardwareKeysEnabled() && keyCode == KeyEvent.KEYCODE_CALL) {
            if (event.isTracking() && !event.isCanceled()) {
                // לחיצה קצרה על מקש חיוג - הפעלת מצב עכבר ועדכון הגדרות המערכת 🖱️
                toggleMouseSupportAndEnable();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void toggleMouseSupportAndEnable() {
        // 1. הפעלת מצב עכבר בהגדרות הפנימיות של הלאנצ'ר
        settings.mouse.setEnabled(true);

        // 2. עדכון רשימת החביפות המורשות ב־Settings.Global עבור העכבר הווירטואלי
        try {
            ContentResolver resolver = getContentResolver();
            String currentList = Settings.Global.getString(resolver, "mouse_support_list");
            String packageName = getPackageName(); // com.example.keylauncher

            if (currentList == null) {
                currentList = "";
            }

            List<String> packages = new ArrayList<>(Arrays.asList(currentList.split(",")));
            packages.removeIf(String::isEmpty);

            if (!packages.contains(packageName)) {
                packages.add(packageName);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < packages.size(); i++) {
                sb.append(packages.get(i));
                if (i < packages.size() - 1) {
                    sb.append(",");
                }
            }

            Settings.Global.putString(resolver, "mouse_support_list", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "מצב עכבר הופעל עבור הלאנצ'ר 🖱️", Toast.LENGTH_SHORT).show();
    }

    private int digitFromKeyCode(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return keyCode - KeyEvent.KEYCODE_0;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return keyCode - KeyEvent.KEYCODE_NUMPAD_0;
        }
        return -1;
    }

    private void focusItemAtDigit(int digit) {
        String assignedPackage = settings.shortcuts.getPackageForDigit(digit);
        if (assignedPackage != null) {
            launchPackage(assignedPackage);
            return;
        }

        int position = (digit == 0) ? 9 : digit - 1;
        if (position < 0 || position >= launcherItems.size()) return;

        recyclerView.scrollToPosition(position);
        recyclerView.post(() -> {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                holder.itemView.requestFocus();
            }
        });
    }

    private void launchPackage(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean searchDialogIsShowing() {
        return currentSearchDialog != null && currentSearchDialog.isShowing();
    }

    @Override
    protected void onStart() {
        super.onStart();
        appWidgetHost.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appWidgetHost.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appWidgetHost.stopListening();
    }

    @Override
    public void onBackPressed() {}
}
