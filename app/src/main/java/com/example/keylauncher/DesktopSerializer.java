package com.example.keylauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * אחראי על המרת פריטי מסך הבית (LauncherItem) ל-JSON ובחזרה,
 * ועל שמירתם וטעינתם דרך DesktopSettings.
 *
 * לתשומת לב: האייקון (Drawable) אינו נשמר - הוא נטען מחדש
 * לפי packageName בכל פעם שהאפליקציה עולה.
 */
public class DesktopSerializer {

    private final DesktopSettings desktopSettings;

    public DesktopSerializer(DesktopSettings desktopSettings) {
        this.desktopSettings = desktopSettings;
    }

    public String serialize(List<LauncherItem> items) {

        JSONArray array = new JSONArray();

        if (items != null) {

            try {

                for (LauncherItem item : items) {

                    JSONObject obj = new JSONObject();

                    obj.put("id", item.getId());
                    obj.put("type", item.getType());
                    obj.put("title", item.getTitle());
                    obj.put("packageName", item.getPackageName());
                    obj.put("className", item.getClassName());
                    obj.put("appWidgetId", item.getAppWidgetId());
                    obj.put("cellX", item.getCellX());
                    obj.put("cellY", item.getCellY());
                    obj.put("spanX", item.getSpanX());
                    obj.put("spanY", item.getSpanY());
                    obj.put("hidden", item.isHidden());
                    obj.put("movable", item.isMovable());
                    obj.put("customData", item.getCustomData());

                    array.put(obj);

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return array.toString();

    }

    public List<LauncherItem> deserialize(String json) {

        List<LauncherItem> result = new ArrayList<>();

        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        try {

            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {

                JSONObject obj = array.getJSONObject(i);

                LauncherItem item = new LauncherItem();

                item.setId(obj.optLong("id", 0));
                item.setType(obj.optInt("type", LauncherItem.TYPE_APP));
                item.setTitle(obj.optString("title", ""));
                item.setPackageName(obj.optString("packageName", ""));
                item.setClassName(obj.optString("className", ""));
                item.setAppWidgetId(obj.optInt("appWidgetId", -1));
                item.setCellX(obj.optInt("cellX", 0));
                item.setCellY(obj.optInt("cellY", 0));
                item.setSpanX(obj.optInt("spanX", 1));
                item.setSpanY(obj.optInt("spanY", 1));
                item.setHidden(obj.optBoolean("hidden", false));
                item.setMovable(obj.optBoolean("movable", true));
                item.setCustomData(obj.optString("customData", ""));

                result.add(item);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;

    }

    public void save(List<LauncherItem> items) {

        desktopSettings.setLayout(serialize(items));

    }

    public List<LauncherItem> load() {

        return deserialize(desktopSettings.getLayout());

    }

    public void clear() {

        desktopSettings.setLayout("");

    }

}
