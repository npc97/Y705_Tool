package com.github.npc97.android.y705_tool.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.npc97.android.y705_tool.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AppPickerActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PACKAGES = "selected_packages";
    public static final String RESULT_SELECTED_PACKAGES = "result_selected_packages";

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private List<AppInfo> appList;
    private MaterialToolbar topBar;

    private List<String> oldSelectedPackages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        topBar = findViewById(R.id.topBar);
        recyclerView = findViewById(R.id.recyclerView);

        setSupportActionBar(topBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        appList = loadApps();

        adapter = new AppAdapter(appList);

        var selectedPackages = getIntent().getStringArrayListExtra(EXTRA_SELECTED_PACKAGES);
        if (selectedPackages != null) {
            oldSelectedPackages = List.copyOf(selectedPackages);
            adapter.setSelectedPackages(selectedPackages);
        }

        recyclerView.setAdapter(adapter);
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmDiscardChanges();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_picker, menu);

        var searchItem = menu.findItem(R.id.action_search);
        var searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("搜索应用");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            var resultIntent = new Intent();
            resultIntent.putStringArrayListExtra(RESULT_SELECTED_PACKAGES, adapter.getSelectedPackageNames());

            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private List<AppInfo> loadApps() {
        var list = new ArrayList<AppInfo>();
        var pm = getPackageManager();
        var packages = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : packages) {
            var appInfo = packageInfo.applicationInfo;
            if (appInfo == null) {
                continue;
            }
            var appName = pm.getApplicationLabel(appInfo).toString();
            var packageName = packageInfo.packageName;
            list.add(new AppInfo(
                    appName,
                    packageName,
                    appInfo.loadIcon(pm)
            ));
        }

        return list;
    }

    private void confirmDiscardChanges() {
        if (this.oldSelectedPackages.equals(this.adapter.getSelectedPackageNames())) {
            setResult(RESULT_OK, null);
            finish();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setMessage("变更尚未保存，是否舍弃？")
                .setNegativeButton("否", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("是", (dialog, which) -> {
                    setResult(RESULT_OK, null);
                    finish();
                })
                .show();
    }
}