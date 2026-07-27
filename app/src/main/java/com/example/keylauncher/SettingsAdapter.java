package com.example.keylauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

    private final Context context;
    private final List<SettingItem> items;
    private final SettingsManager settings;

    public SettingsAdapter(Context context,
                           List<SettingItem> items,
                           SettingsManager settings) {

        this.context = context;
        this.items = items;
        this.settings = settings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_setting, parent, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SettingItem item = items.get(position);

        holder.title.setText(item.getTitle());
        holder.summary.setText(item.getSummary());

        holder.itemView.setOnClickListener(v -> {

            switch (position) {

                case 0:
                    SettingsDialogs.showDisplayDialog(context, settings);
                    break;

                case 1:
                    SettingsDialogs.showWidgetsDialog(context, settings);
                    break;

                case 2:
                    SettingsDialogs.showAppsDialog(context, settings);
                    break;

                case 3:
                    SettingsDialogs.showFoldersDialog(context, settings);
                    break;

                case 4:
                    SettingsDialogs.showKeysDialog(context, settings);
                    break;

                case 5:
                    SettingsDialogs.showMouseDialog(context, settings);
                    break;

                case 6:
                    SettingsDialogs.showBackupDialog(context, settings);
                    break;

                case 7:
                    SettingsDialogs.showAboutDialog(context, settings);
                    break;

            }

        });

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView summary;

        ViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.settingTitle);
            summary = itemView.findViewById(R.id.settingSummary);
        }

    }

}
