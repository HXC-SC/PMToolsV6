package com.ruibang.pointmeasuretoolsv6;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ruibang.pointmeasuretoolsv6.databinding.ActivityMainBinding;
import com.ruibang.pointmeasuretoolsv6.databinding.ContentMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    //private TextView pdfOperationHint;
//    private Button backToListButton;
    private ProgressDialog progressDialog;
    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView; // 添加底部导航栏变量声明
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ListView pdfListView;
    private PDFView pdfView;
    private List<File> pdfFiles;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.content_main);
        // 使用数据绑定库来创建视图
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 检查是否有外部存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                }
        // 检查是否有外部存储权限

//        val navController = findNavController(R.id.fragment_container);
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation);
//        bottomNav.setupWithNavController(navController);
        // 初始化底部导航栏
        bottomNavigationView = binding.bottomNavigationView; // 使用数据绑定初始化
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            if (navController == null) {
                Log.e("MainActivity", "导航控制器未找到，请检查布局文件");
                Toast.makeText(this, "导航控制器未找到，请检查布局文件", Toast.LENGTH_SHORT).show();
                return false;
            }
            int itemId = item.getItemId(); // 获取菜单项ID
            if (itemId == R.id.nav_document) {
                // 处理“文档”功能
                try {
                    navController.navigate(R.id.FirstFragment);
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "导航到 FirstFragment 失败: " + e.getMessage());
                    Toast.makeText(this, "无法切换到文档页面，请检查导航配置", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_cloud) {
                // 切换到“云”界面
                try {
                    navController.navigate(R.id.cloudFragment);
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "导航到 cloudFragment 失败: " + e.getMessage());
                    Toast.makeText(this, "无法切换到云页面，请检查导航配置", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_app) {
                // 切换到“应用”界面
                try {
                    navController.navigate(R.id.AppFragment);
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "导航到 AppFragment 失败: " + e.getMessage());
                    Toast.makeText(this, "无法切换到应用页面，请检查导航配置", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_me) {
                // 切换到“我”界面
                try {
                    navController.navigate(R.id.MeFragment);
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "导航到 MeFragment 失败: " + e.getMessage());
                    Toast.makeText(this, "无法切换到'我'页面，请检查导航配置", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // 确保导航控制器正确初始化
        View navHostFragment = findViewById(R.id.bottomNavigationView);
        if (navHostFragment != null) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
            //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        } else {
            // 添加日志或调试信息，帮助定位问题
            Log.e("MainActivity", "导航宿主视图未找到，请检查 activity_main.xml 中的 NavHostFragment 配置");
            Toast.makeText(this, "导航宿主视图未找到，请检查布局文件", Toast.LENGTH_LONG).show();
        }

        // 初始化返回按钮
        backButton = binding.topAppBar.findViewById(R.id.backButton);
        if (backButton == null) {
            Log.e("MainActivity", "返回按钮未找到，请检查布局文件");
            Toast.makeText(this, "返回按钮未找到，请检查布局文件", Toast.LENGTH_LONG).show();
        }else{
            backButton.setOnClickListener(v -> {
                // 处理返回逻辑
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.FirstFragment); // 返回到文件列表
            });
        }

//        pdfListView = findViewById(R.id.pdfListView);
//        pdfView = findViewById(R.id.pdfView);
        //pdfOperationHint = findViewById(R.id.pdfOperationHint);

        // 初始化PDF列表视图和PDF视图
//        pdfListView = binding.pdfListView;
//        pdfView = binding.pdfView;
//        pdfOperationHint = binding.pdfOperationHint; // 使用数据绑定初始化


        // 初始化进度对话框
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage(" 正在加载PDF文件...");
//        progressDialog.setCancelable(false);
//
//        // 检查并请求读取外部存储的权限
//        if (ContextCompat.checkSelfPermission(this,  Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    PERMISSION_REQUEST_CODE);
//        }  else {
//            //loadPDFs();
//        }

//        // 初始化返回列表按钮并设置点击事件
//        backToListButton = binding.backToListButton;
//        backToListButton.setOnClickListener(new  View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pdfView.setVisibility(View.GONE);
//                //pdfOperationHint.setVisibility(View.GONE);
//                backToListButton.setVisibility(View.GONE);
//                pdfListView.setVisibility(View.VISIBLE);
//            }
//        });

//        // 设置PDF列表项长按事件
//        pdfListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                File selectedFile = pdfFiles.get(position);
//                showFileInfoDialog(selectedFile);
//                return true;
//            }
//        });
    }

//    private void showFileInfoDialog(File file) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("文件信息");
//
//        // 设置对话框布局
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_file_info, null);
//        builder.setView(dialogView);
//
//        // 初始化对话框中的视图
//        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
//        EditText latitudeEditText = dialogView.findViewById(R.id.latitudeEditText);
//        EditText longitudeEditText = dialogView.findViewById(R.id.longitudeEditText);
//        CheckBox isNormalCheckBox = dialogView.findViewById(R.id.isNormalCheckBox);
//        EditText remarkEditText = dialogView.findViewById(R.id.remarkEditText);
//
//        // 设置对话框按钮
//        builder.setPositiveButton("保存", (dialog, which) -> {
//            // 处理保存逻辑
//        });
//        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
//
//        builder.create().show();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单资源
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理菜单项点击事件
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // 处理导航返回事件
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,  permissions, grantResults);
        // 处理权限请求结果
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length  > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //loadPDFs();
            } else {
                Toast.makeText(this,  "权限被拒绝，无法读取文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void loadPDFs() {
//        // 加载PDF文件列表
//        pdfFiles = new ArrayList<>();
//        File externalStorageDirectory = Environment.getExternalStorageDirectory();
//        findPDFs(externalStorageDirectory);
//
//        List<Map<String, Object>> data = new ArrayList<>();
//        for (File file : pdfFiles) {
//            Map<String, Object> item = new HashMap<>();
//            item.put("pdfFileName",  file.getName());
//            // 根据文件类型设置图标
//            if (file.getName().endsWith(".pdf")) {
//                item.put("pdfIcon", R.drawable.ic_pdf);
//            } else {
//                item.put("pdfIcon", R.drawable.ic_document);
//            }
//            data.add(item);
//        }
//
//        // 设置PDF列表适配器
//        SimpleAdapter adapter = new SimpleAdapter(this, data,
//                R.layout.list_item_pdf,
//                new String[]{"pdfIcon", "pdfFileName"},
//                new int[]{R.id.pdfIcon,  R.id.pdfFileName});
//        pdfListView.setAdapter(adapter);
//
//        // 设置ListView的选择模式为单选
//        pdfListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//
//        // 设置PDF列表项点击事件
//        pdfListView.setOnItemClickListener(new  AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                // 高亮选中的行
//                for (int i = 0; i < parent.getChildCount(); i++) {
//                    parent.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
//                }
//                view.setBackgroundColor(Color.LTGRAY);
//
//                File selectedFile = pdfFiles.get(position);
//                //showPDF(selectedFile);
//            }
//        });
//    }

    private void findPDFs(File directory) {
        // 递归查找PDF文件
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())  {
                    findPDFs(file);
                } else if (file.getName().endsWith(".pdf"))  {
                    pdfFiles.add(file);
                }
            }
        }
    }
    public void showBackButton(boolean show) {
        if (backButton != null) {
            backButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


}