package cn.myflv.noactive;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.myflv.noactive.adapter.AppItemAdapter;
import cn.myflv.noactive.entity.AppItem;
import cn.myflv.noactive.utils.PackageUtils;

public class MainActivity extends AppCompatActivity {
    private final List<AppItem> appItemList = new ArrayList<>();
    private final Handler handler = new Handler();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppItemAdapter appItemAdapter;
    private String text = null;
    private int type = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean rootAccess = Shell.getShell().isRoot();
        if (!rootAccess) {
            Toast.makeText(this, "Root权限获取失败", Toast.LENGTH_LONG).show();
        }
        initEditView();
        initSpinnerView();
        initListView();
        initMenu();
    }

    private void initMenu() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        int nightMode = uiModeManager.getNightMode();

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (nightMode == 2) {
            toolbar.inflateMenu(R.menu.main_night);
        } else {
            toolbar.inflateMenu(R.menu.main);
        }
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    private void initSpinnerView() {
        Spinner spinner = findViewById(R.id.app_scope);
        ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this, R.array.app_filter, R.layout.spinner_layout);
        spinner.setBackgroundColor(0x0);
        spinner.setAdapter(adapter);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = position + 1;
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void initEditView() {
        EditText editText = findViewById(R.id.search_txt);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                text = s.toString();
                refresh();
            }
        });
    }

    public void initListView() {
        ListView listView = findViewById(R.id.app_list);
        appItemAdapter = new AppItemAdapter(MainActivity.this, R.layout.app_item, appItemList);
        listView.setAdapter(appItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView appName = view.findViewById(R.id.app_name);
                TextView packageName = view.findViewById(R.id.package_name);
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SubActivity.class);
                intent.putExtra("appName", appName.getText());
                intent.putExtra("packageName", packageName.getText());
                startActivity(intent);
            }
        });
    }


    public void refresh() {
        executorService.submit(() -> {
            List<AppItem> filter = PackageUtils.filter(this, type, text);
            appItemList.clear();
            appItemList.addAll(filter);
            handler.post(() -> {
                synchronized (appItemList) {
                    appItemAdapter.notifyDataSetChanged();
                }
            });
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}