package com.example.keylauncher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * המסך הראשי של הלאנצ'ר.
 *
 * אחראי על:
 * - טעינת והצגת רשימת האפליקציות המותקנות
 * - תפריט פעולות (חיפוש, הצגת/הסתרת אפליקציות מוסתרות, רענון, הגדרות)
 * - הזזת אייקונים (גרירה) ושמירת הסדר
 * - שחזור, הוספה והצגה של ווידג'טים
 * - טיפול במקשי חומרה לפי ההגדרות
 */
public class MainActivity extends AppCompatActivity
        implements LauncherAdapter.OnItemMoveListener {

    public static final int APPWIDGET_HOST_ID = 1024;

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

        initializeManagers();

        initializeRecyclerView();

        loadApplications();

        restoreWidgets();
    }

    /* ===========================
       אתחול
       =========================== */

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

        gridLayoutManager = new GridLayoutManager(
                this,
                settings.display.getGridColumns());

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

                if (from < 0 || to < 0 || from >= launcherItems.size()
                        || to >= launcherItems.size()) {
                    return false;
                }

                Collections.swap(launcherItems, from, to);
                launcherAdapter.setItems(launcherItems);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                  int direction) {
                // אין תמיכה בהחלקה, רק בגרירה.
            }

            @Override
            public void clearView(@NonNull RecyclerView rv,
                                   @NonNull RecyclerView.ViewHolder viewHolder) {

                super.clearView(rv, viewHolder);

                // גרירה הסתיימה - שומרים את הסדר החדש.
                desktopLayout.setItems(launcherItems);
                desktopLayout.save();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // הגרירה מופעלת ידנית מתוך תפריט "הזז" (ראו onMoveRequested).
                return false;
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /* ===========================
       טעינת אפליקציות
       =========================== */

    private void loadApplications() {

        launcherItems.clear();
        launcherItems.addAll(appLoader.loadApplications(showHiddenApps));

        desktopLayout.setItems(launcherItems);

        launcherAdapter.setItems(launcherItems);

        requestInitialFocus();
    }

    /**
     * במכשיר מקשים בלבד אין דרך "לגעת" כדי להתחיל למקד אייקון,
     * לכן ממקדים אוטומטית את האייקון הראשון אחרי שהרשימה נטענת.
     */
    private void requestInitialFocus() {

        recyclerView.post(() -> {

            if (recyclerView.getChildCount() == 0) {
                return;
            }

            View first = recyclerView.getChildAt(0);

            if (first != null && !hasAnyFocusedChild()) {
                first.requestFocus();
            }

        });
    }

    private boolean hasAnyFocusedChild() {

        for (int i = 0; i < recyclerView.getChildCount(); i++) {

            if (recyclerView.getChildAt(i).isFocused()) {
                return true;
            }

        }

        return false;
    }

    /* ===========================
       תפריט
       =========================== */

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
            Toast.makeText(this, "רשימת האפליקציות רועננה", Toast.LENGTH_SHORT).show();
            return true;

        } else if (itemId == R.id.action_settings) {

            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* ===========================
       חיפוש אפליקציות
       =========================== */

    private void showSearchDialog() {

        currentSearchDialog =
                new AppSearchDialog(this, launcherItems, settings);

        currentSearchDialog.show();
    }

    /* ===========================
       הזזת אייקונים
       =========================== */

    @Override
    public void onMoveRequested(LauncherItem item) {

        int position = launcherItems.indexOf(item);

        if (position < 0) {
            return;
        }

        RecyclerView.ViewHolder viewHolder =
                recyclerView.findViewHolderForAdapterPosition(position);

        if (viewHolder != null) {

            Toast.makeText(this,
                    "גררו את האייקון למיקום הרצוי",
                    Toast.LENGTH_SHORT).show();

            itemTouchHelper.startDrag(viewHolder);
        }
    }

    /* ===========================
       ווידג'טים
       =========================== */

    private void restoreWidgets() {

        appWidgetHost.startListening();

        if (!settings.widgets.isWidgetsEnabled()) {
            return;
        }

        int lastWidgetId = settings.widgets.getLastWidgetId();

        if (lastWidgetId == -1) {
            return;
        }

        AppWidgetProviderInfo info =
                appWidgetManager.getAppWidgetInfo(lastWidgetId);

        if (info == null) {
            // הווידג'ט כבר לא קיים (הוסר/הוסרה האפליקציה שלו).
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

            if (resultCode != RESULT_OK || data == null) {
                return;
            }

            int appWidgetId = data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

            if (appWidgetId == -1) {
                return;
            }

            AppWidgetProviderInfo info =
                    appWidgetManager.getAppWidgetInfo(appWidgetId);

            if (info == null) {
                return;
            }

            if (info.configure != null) {

                pendingWidgetId = appWidgetId;

                Intent configureIntent =
                        new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);

                configureIntent.setComponent(info.configure);
                configureIntent.putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

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

            AppWidgetProviderInfo info =
                    appWidgetManager.getAppWidgetInfo(pendingWidgetId);

            if (info != null) {
                showWidget(pendingWidgetId, info);
                persistWidgetId(pendingWidgetId);
            }

            pendingWidgetId = -1;
        }
    }

    private void showWidget(int appWidgetId, AppWidgetProviderInfo info) {

        AppWidgetHostView hostView =
                appWidgetHost.createView(this, appWidgetId, info);

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
       מקשי חומרה
       =========================== */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (settings.keys.isHardwareKeysEnabled()
                && keyCode == KeyEvent.KEYCODE_CALL) {

            handleKeyAction(settings.keys.getCallShortPressAction());
            return true;
        }

        int digit = digitFromKeyCode(keyCode);

        if (digit >= 0 && !searchDialogIsShowing()) {

            focusItemAtDigit(digit);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * ממיר קוד מקש למספר 0-9 (תומך גם במקלדת ראשית וגם ב-Numpad),
     * או מחזיר -1- אם זה לא מקש ספרה.
     */
    private int digitFromKeyCode(int keyCode) {

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return keyCode - KeyEvent.KEYCODE_0;
        }

        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return keyCode - KeyEvent.KEYCODE_NUMPAD_0;
        }

        return -1;
    }

    /**
     * קיצור מקשים בסגנון טלפון-מקשים: מקש "1" ממקד את האייקון הראשון,
     * "2" את השני וכן הלאה, "0" את העשירי. גלילה אוטומטית אם צריך.
     */
    private void focusItemAtDigit(int digit) {

        int position = (digit == 0) ? 9 : digit - 1;

        if (position < 0 || position >= launcherItems.size()) {
            return;
        }

        recyclerView.scrollToPosition(position);

        recyclerView.post(() -> {

            RecyclerView.ViewHolder holder =
                    recyclerView.findViewHolderForAdapterPosition(position);

            if (holder != null) {
                holder.itemView.requestFocus();
            }

        });
    }

    private boolean searchDialogIsShowing() {
        return currentSearchDialog != null && currentSearchDialog.isShowing();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {

        if (settings.keys.isHardwareKeysEnabled()
                && keyCode == KeyEvent.KEYCODE_CALL) {

            handleKeyAction(settings.keys.getCallLongPressAction());
            return true;
        }

        return super.onKeyLongPress(keyCode, event);
    }

    private void handleKeyAction(int action) {

        switch (action) {

            case KeySettings.ACTION_TOGGLE_MOUSE:
                settings.mouse.setEnabled(!settings.mouse.isEnabled());
                Toast.makeText(this,
                        settings.mouse.isEnabled() ? "מצב עכבר הופעל" : "מצב עכבר כובה",
                        Toast.LENGTH_SHORT).show();
                break;

            case KeySettings.ACTION_OPEN_DIALER:
                startActivity(new Intent(Intent.ACTION_DIAL));
                break;

            case KeySettings.ACTION_OPEN_SEARCH:
                showSearchDialog();
                break;

            case KeySettings.ACTION_OPEN_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case KeySettings.ACTION_SHOW_APPS:
                recyclerView.scrollToPosition(0);
                break;

            case KeySettings.ACTION_NONE:
            default:
                break;
        }
    }

    /* ===========================
       ניהול מחזור החיים
       =========================== */

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
    public void onBackPressed() {
        // זהו לאנצ'ר (HOME) - לא יוצאים ממנו בלחיצת "חזרה".
        // (אם רוצים לאפשר יציאה בכל זאת, יש להחליף לשורה: super.onBackPressed();)
    }
}
