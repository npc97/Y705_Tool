package com.github.npc97.android.y705_tool.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.npc97.android.y705_tool.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final List<AppInfo> originalList;
    private final List<AppInfo> appList;

    private final Set<String> selectedPackageSet = new HashSet<>();

    public AppAdapter(List<AppInfo> appList) {
        this.originalList = new ArrayList<>(appList);
        this.appList = new ArrayList<>(appList);
    }

    public void setSelectedPackages(List<String> packageNames) {
        selectedPackageSet.clear();

        if (packageNames != null) {
            selectedPackageSet.addAll(packageNames);
        }

        sortApps();

        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedPackageNames() {
        return new ArrayList<>(selectedPackageSet);
    }

    public void filter(String keyword) {
        appList.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            appList.addAll(originalList);
        } else {
            var lowerKeyword = keyword.toLowerCase(Locale.ROOT);

            for (AppInfo app : originalList) {
                if (app.getAppName().toLowerCase(Locale.ROOT).contains(lowerKeyword) || app.getPackageName().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                    appList.add(app);
                }
            }
        }

        notifyDataSetChanged();
    }

    private void sortApps() {
        Collections.sort(appList, (a, b) -> {

            boolean aSelected = selectedPackageSet.contains(a.getPackageName());

            boolean bSelected = selectedPackageSet.contains(b.getPackageName());

            // 已选优先
            if (aSelected != bSelected) {
                return aSelected ? -1 : 1;
            }

            // 同组按名称排序
            return a.getAppName().compareToIgnoreCase(b.getAppName());
        });
    }

    @NonNull
    @Override
    public AppAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AppAdapter.ViewHolder holder,
            int position
    ) {
        var app = appList.get(position);
        var packageName = app.getPackageName();

        holder.appName.setText(app.getAppName());
        holder.packageName.setText(packageName);
        holder.icon.setImageDrawable(app.getIcon());

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPackageSet.contains(packageName));

        View.OnClickListener toggleListener = v -> {
            if (selectedPackageSet.contains(packageName)) {
                selectedPackageSet.remove(packageName);
                holder.checkBox.setChecked(false);
            } else {
                selectedPackageSet.add(packageName);
                holder.checkBox.setChecked(true);
            }
        };

        holder.itemView.setOnClickListener(toggleListener);
        holder.checkBox.setOnClickListener(toggleListener);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView appName;
        TextView packageName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            appName = itemView.findViewById(R.id.appName);
            packageName = itemView.findViewById(R.id.packageName);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}