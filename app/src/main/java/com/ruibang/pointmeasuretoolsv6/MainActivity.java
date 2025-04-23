package com.ruibang.pointmeasuretoolsv6;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
    private MaterialToolbar topAppBar;
    // 新增成员变量，用于保存 Menu 对象
    private Menu mainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用数据绑定库来创建视图
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        topAppBar = binding.topAppBar; // 通过数据绑定获取顶部栏
        backButton = topAppBar.findViewById(R.id.backButton);
        // 设置 MaterialToolbar 作为动作栏
        setSupportActionBar(topAppBar);

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
                    //setTopAppBarTitle("文档"); // 设置顶部栏标题
                    initFirstFragment();
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
        backButton = findViewById(R.id.backButton);
        if (backButton == null) {
            Log.e("MainActivity", "返回按钮未找到，请检查布局文件");
            Toast.makeText(this, "返回按钮未找到，请检查布局文件", Toast.LENGTH_LONG).show();
        } else {
            Log.d("MainActivity", "返回按钮初始化成功");
            backButton.setOnClickListener(v -> {
                // 处理返回逻辑
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.FirstFragment);
                initFirstFragment();
//                if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main) instanceof FirstFragment) {
//                    FirstFragment firstFragment = (FirstFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
//                    firstFragment.showFileList();
//                    initFirstFragment();
//                }
                //showBackButton(false);
            });
        }

    }

    public void initFirstFragment() {
        showBackButton(false);
        setTopAppBarTitle("文档"); // 设置顶部栏标题
        setTopAppBarMenuVisibility(mainMenu, true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单资源
        getMenuInflater().inflate(R.menu.top_appbar_menu, menu);
        // 保存 Menu 对象
        this.mainMenu = menu;
        return true;
    }

    /**
     * 获取当前的 Menu 对象
     * @return Menu 对象
     */
    public Menu getMainMenu() {
        return mainMenu;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理菜单项点击事件
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.data) {
            // 处理“数据”按钮点击事件
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_content_main);

            if (currentFragment instanceof androidx.navigation.fragment.NavHostFragment) {
                androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) currentFragment;
                FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
                currentFragment = childFragmentManager.getFragments().isEmpty() ? null : childFragmentManager.getFragments().get(0);
            }

            if (currentFragment instanceof FirstFragment) {
                FirstFragment firstFragment = (FirstFragment) currentFragment;
                String[] pointNames = firstFragment.getPointNames();
                List<PointInfo> pointInfoList = firstFragment.readPointInfoFromLocal(firstFragment.getCurrentPDF());

                if (pointNames != null && pointNames.length > 0) {
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_point_names, null);
                    ListView listView = dialogView.findViewById(R.id.listViewPointNames);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pointNames);
                    listView.setAdapter(adapter);

                    View titleView = getLayoutInflater().inflate(R.layout.dialog_title, null);
                    TextView titleTextView = (TextView) titleView;
                    titleTextView.setText("定位点名称列表");

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setCustomTitle(titleView);
                    builder.setView(dialogView);
                    AlertDialog pointListDialog = builder.create();
                    pointListDialog.show();

                    listView.setOnItemClickListener((parent, view, position, id1) -> {
                        PointInfo oldPoint = pointInfoList.get(position);
                        View infoDialogView = getLayoutInflater().inflate(R.layout.dialog_position_info, null);

                        EditText nameEditText = infoDialogView.findViewById(R.id.nameEditText);
                        EditText latitudeEditText = infoDialogView.findViewById(R.id.latitudeEditText);
                        EditText longitudeEditText = infoDialogView.findViewById(R.id.longitudeEditText);
                        EditText pdfXEditText = infoDialogView.findViewById(R.id.pdfXEditText);
                        EditText pdfYEditText = infoDialogView.findViewById(R.id.pdfYEditText);
                        EditText remarkEditText = infoDialogView.findViewById(R.id.remarkEditText);

                        // 填充定位点信息
                        nameEditText.setText(oldPoint.name);
                        latitudeEditText.setText(oldPoint.latitude);
                        longitudeEditText.setText(oldPoint.longitude);
                        pdfXEditText.setText(oldPoint.pdfX);
                        pdfYEditText.setText(oldPoint.pdfY);
                        remarkEditText.setText(oldPoint.remark);

                        AlertDialog.Builder infoBuilder = new AlertDialog.Builder(this);
                        infoBuilder.setView(infoDialogView);
                        infoBuilder.setPositiveButton("保存", (dialog, which) -> {
                            String newName = nameEditText.getText().toString();
                            String newLatitude = latitudeEditText.getText().toString();
                            String newLongitude = longitudeEditText.getText().toString();
                            String newPdfX = pdfXEditText.getText().toString();
                            String newPdfY = pdfYEditText.getText().toString();
                            String newRemark = remarkEditText.getText().toString();

                            PointInfo newPoint = new PointInfo(newName, oldPoint.type, newLatitude, newLongitude, newPdfX, newPdfY, newRemark);
                            firstFragment.updatePointInfo(oldPoint, newPoint);
                        });
                        infoBuilder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                        infoBuilder.show();
                    });
                }
            }
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

    public void showBackButton(boolean show) {
        if (backButton != null) {
            backButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 控制顶部栏菜单项的显示与隐藏
     * @param menu 菜单对象
     * @param show 是否显示菜单项
     */
    public void setTopAppBarMenuVisibility(Menu menu, boolean show) {
        if (menu != null) {
            MenuItem searchItem = menu.findItem(R.id.search);
            MenuItem editItem = menu.findItem(R.id.edit);
            MenuItem dataItem = menu.findItem(R.id.data);
            if (searchItem != null) {
                searchItem.setVisible(show);
            }
            if (editItem != null) {
                editItem.setVisible(show);
            }
            if(dataItem != null){
                dataItem.setVisible(!show);
            }
        }
    }
    /**
     * 修改顶部栏标题
     * @param title 新的标题
     */
    public void setTopAppBarTitle(String title) {
        if (topAppBar != null) {
            topAppBar.setTitle(title);
        }
    }




}