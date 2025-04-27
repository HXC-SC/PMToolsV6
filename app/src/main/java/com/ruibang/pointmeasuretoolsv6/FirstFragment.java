package com.ruibang.pointmeasuretoolsv6;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ruibang.pointmeasuretoolsv6.databinding.FragmentFirstBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FirstFragment extends Fragment implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private GestureDetectorCompat gestureDetector;
    private FragmentFirstBinding binding;
    //private Button backToListButton;
    private ProgressDialog progressDialog;
    private PDFView pdfView;
    private ListView pdfListView;
    private List<File> pdfFiles;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;

    private static final int PERMISSION_REQUEST_CODE = 1;
    // 权限请求码
    private static final int REQUEST_CODE_READ_STORAGE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private static float mPageWidth;    // 页面宽度
    private static float mPageHeight;   // 页面高度
    private static float mScaleFactor;  // 缩放因子

    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private File currentPDF;//当前PDF

    private long downTime;
    private long upTime;

    private float screenX;
    private float screenY;

    private Menu mainMenu; //

    private SearchView searchView;
    private List<File> allPdfFiles; // 备份所有文件列表
    private boolean isEditing = false;
    private boolean isSearching  = false;
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        // 初始化进度对话框
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(" 正在加载PDF文件...");
        progressDialog.setCancelable(false);

        // 使用数据绑定库来创建视图
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        // 初始化PDF列表视图
        pdfListView = rootView.findViewById(R.id.pdfListView);

        // 初始化PDF视图
        pdfView = rootView.findViewById(R.id.pdfView);

        // 设置返回按钮可见性
        setHasOptionsMenu(true);

        // 初始化搜索视图
        searchView = rootView.findViewById(R.id.searchView);
        if (searchView != null) { // 添加空值检查
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterFiles(newText);
                    return true;
                }
            });
        }else{
            Log.e("FirstFragment", "SearchView is null");
            Toast.makeText(getContext(), "SearchView is null", Toast.LENGTH_SHORT).show();
        }

        //检查权限
        checkReadStoragePermission();//读取外部存储权限
        checkLocationPermission();  //定位权限位置权限
        // 检查并请求读取外部存储的权限
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(requireActivity(),
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    PERMISSION_REQUEST_CODE);
//        } else {
//            loadPDFs();
//        }

        // 初始化返回列表按钮并设置点击事件
//        backToListButton = binding.backToListButton;
//        backToListButton.setOnClickListener(new  View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pdfView.setVisibility(View.GONE);
//                //pdfOperationHint.setVisibility(View.GONE);
//                backToListButton.setVisibility(View.GONE);
//                pdfListView.setVisibility(View.VISIBLE);
//
//                // 隐藏返回按钮
//                if (getActivity() instanceof MainActivity) {
//                    ((MainActivity) getActivity()).showBackButton(false);
//                }
//            }
//        });


        // 设置PDF列表项长按事件
//        pdfListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                File selectedFile = pdfFiles.get(position);
//                showFileInfoDialog(selectedFile);
//                return true;
//            }
//        });

        return rootView;
    }

    // 检查权限状态
    private void checkReadStoragePermission() {
        int permissionStatus = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
        );

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            // 已有权限，直接读取文件
            loadPDFs();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_STORAGE
            );
        }
    }

    private void loadPDFs() {

        // 加载PDF文件列表
        pdfFiles = new ArrayList<>();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        findPDFs(externalStorageDirectory);

        List<Map<String, Object>> data = new ArrayList<>();
        for (File file : pdfFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("pdfFileName", file.getName());
            // 根据文件类型设置图标
            if (file.getName().endsWith(".pdf")) {
                item.put("pdfIcon", R.drawable.ic_pdf);
            } else {
                item.put("pdfIcon", R.drawable.ic_document);
            }
            data.add(item);
        }

        // 设置PDF列表适配器
        SimpleAdapter adapter = new SimpleAdapter(requireContext(), data,
                R.layout.list_item_pdf,
                new String[]{"pdfIcon", "pdfFileName"},
                new int[]{R.id.pdfIcon, R.id.pdfFileName});
        pdfListView.setAdapter(adapter);

        // 设置ListView的选择模式为单选
        pdfListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // 设置PDF列表项点击事件
        pdfListView.setOnItemClickListener(new  AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 高亮选中的行
                for (int i = 0; i < parent.getChildCount(); i++) {
                    parent.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                view.setBackgroundColor(Color.LTGRAY);
                File selectedFile = pdfFiles.get(position);
                showPDF(selectedFile);
            }
        });
    }

    private void findPDFs(File directory) {
        // 递归查找PDF文件
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findPDFs(file);
                } else if (file.getName().endsWith(".pdf")) {
                    pdfFiles.add(file);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 处理权限请求结果
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPDFs();
            } else {
                Toast.makeText(requireContext(), "权限被拒绝，无法读取文件", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以进行定位操作
            } else {
                Toast.makeText(requireContext(), "定位权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以继续重命名操作
            } else {
                Toast.makeText(requireContext(), "存储权限被拒绝，无法重命名文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清除绑定，防止内存泄漏
        binding = null;
    }

    private void showPDF(File file) {
        try {
            // 显示加载进度对话框
            progressDialog.show();
            // 隐藏文件列表视图
            pdfListView.setVisibility(View.GONE);
            // 显示 PDF 视图
            pdfView.setVisibility(View.VISIBLE);
            //backToListButton.setVisibility(View.VISIBLE);

            Log.d("FirstFragment", "尝试加载 PDF 文件: " + file.getAbsolutePath());

            // 显示返回按钮
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // 显示返回按钮
                mainActivity.showBackButton(true);
                // 获取 Menu 对象
                Menu menu = mainActivity.getMainMenu();
                if (menu != null) {
                    // 隐藏搜索和编辑按钮
                    mainActivity.setTopAppBarMenuVisibility(menu, false);
                    //显示数据按钮
                    mainActivity.setTopAppBarDataBtnVisibility(true);
                }
                mainActivity.setTopAppBarTitle(file.getName());
            }
            // 加载 PDF 文件
            pdfView.fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableAnnotationRendering(true)
                    .onDraw((canvas, pageWidth, pageHeight, displayedPage) -> {
                        // 保存页面尺寸和缩放比例
                        mPageWidth = pageWidth;
                        mPageHeight = pageHeight;
                        mScaleFactor = canvas.getWidth() / pageWidth; // 假设水平方向铺满
                    })
//                    .password(null) // 若有密码，设置正确密码
//                    .scrollHandle(null) // 可根据需要设置滚动处理
//                    .enableAntialiasing(true) // 启用抗锯齿
//                    .spacing(0) // 页面间距
                    .onLoad(new OnLoadCompleteListener() {
                        @Override
                        public void loadComplete(int nbPages) {
                            // 加载完成后关闭进度对话框
                            progressDialog.dismiss();
                            Log.d("FirstFragment", "PDF 文件加载完成，总页数: " + nbPages);
                            pdfView.invalidate(); // 手动刷新视图

                            // 对 currentPDF 赋值
                            currentPDF = file;
                            // 读取之前保存的定位点信息
                            List<PointInfo> pointInfoList = readPointInfoFromLocal(file);
                            for (PointInfo pointInfo : pointInfoList) {
                                // 处理读取到的定位点信息，例如显示在界面上
                                Log.d("FirstFragment", "读取到的定位点信息: " + pointInfo.toString());
                            }
                        }
                    })
                    .onError(throwable -> {
                        // 加载出错时关闭进度对话框并显示错误信息
                        progressDialog.dismiss();
                        Log.e("FirstFragment", "PDF 文件加载出错: " + throwable.getMessage());
                    })
                    .load();
            // 初始化手势检测器
            gestureDetector = new GestureDetectorCompat(requireContext(), this);
            gestureDetector.setOnDoubleTapListener(this); // 设置双击监听


            // 确保触摸事件能传递到长按监听器
            pdfView.setOnTouchListener((v, event) -> {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        screenX = event.getX();
                        screenY = event.getY();
                        //downTime = event.getDownTime();
                        //longPressHandler.postDelayed(longPressRunnable, 6000); // 设置长按阈值（例如800ms）
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        // 检测移动距离，若超过阈值则取消长按
//                        if (Math.abs(event.getX() - downX) > touchSlop ||
//                                Math.abs(event.getY() - downY) > touchSlop) {
//                            longPressHandler.removeCallbacks(longPressRunnable);
//                        }
                        break;
                    case MotionEvent.ACTION_UP:
//                        upTime = event.getEventTime();
//                        if(upTime - downTime > 1200){
//                            float screenX = event.getX();
//                            float screenY = event.getY();
//                            PointF pdfCoordinates = convertToPdfCoordinates(screenX, screenY);
//                            showPositionDialog(pdfCoordinates);
//                        }
                        //gestureDetector.onTouchEvent(event);

                        //longPressHandler.removeCallbacks(longPressRunnable);
                        break;
                }

                return gestureDetector.onTouchEvent(event);
            });
            // 添加长按监听器
            pdfView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PointF pdfCoordinates = convertToPdfCoordinates(screenX, screenY);
                    // 获取文档上的位置，这里简单假设屏幕位置和文档位置一致，实际可能需要转换
                    // 可根据 pdfView 的缩放、滚动等状态进行精确转换
                    showPositionDialog(pdfCoordinates);
                    return true;
                }
            });
        } catch (Exception e) {
            // 捕获异常并关闭进度对话框
            progressDialog.dismiss();
            Log.e("FirstFragment", "显示 PDF 文件时出错: " + e.getMessage());
        }
    }

    private PointF convertToPdfCoordinates(float screenX, float screenY) {
        // 计算相对于 PDF 页面的坐标
        float pdfX = screenX / mScaleFactor;
        float pdfY = (mPageHeight - (screenY / mScaleFactor)); // 翻转 Y 轴
        return new PointF(pdfX, pdfY);
    }
    private void showPositionDialog(PointF pdfPoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("定位点信息");

        // 加载布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_position_info, null);
        builder.setView(dialogView);

        // 初始化视图
        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        EditText latitudeEditText = dialogView.findViewById(R.id.latitudeEditText);
        EditText longitudeEditText = dialogView.findViewById(R.id.longitudeEditText);
        EditText pdfXEditText = dialogView.findViewById(R.id.pdfXEditText);
        EditText pdfYEditText = dialogView.findViewById(R.id.pdfYEditText);

        // 设置文件坐标
        pdfXEditText.setText(String.valueOf(pdfPoint.x));
        pdfYEditText.setText(String.valueOf(pdfPoint.y));

        // 设置对话框按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 获取输入的数据
            String name = nameEditText.getText().toString();
            String type = (String) typeSpinner.getSelectedItem();
            String latitude = latitudeEditText.getText().toString();
            String longitude = longitudeEditText.getText().toString();
            String pdfX = pdfXEditText.getText().toString();
            String pdfY = pdfYEditText.getText().toString();

            // 保存数据到本地
            savePointInfoToLocal(name, type, latitude, longitude, pdfX, pdfY);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void savePointInfoToLocal(String name, String type, String latitude, String longitude, String pdfX, String pdfY) {
        try {
            // 获取当前打开的 PDF 文件的唯一标识
            String pdfFileId = getFileId(currentPDF);

            // 获取存储目录
            File storageDir = requireContext().getFilesDir();
            File jsonFile = new File(storageDir, pdfFileId + ".json");

            JSONArray jsonArray;
            if (jsonFile.exists()) {
                // 读取已有的数据
                String jsonContent = readFile(jsonFile);
                jsonArray = new JSONArray(jsonContent);
            } else {
                jsonArray = new JSONArray();
            }

            // 创建新的定位点信息对象
            JSONObject pointInfo = new JSONObject();
            pointInfo.put("name", name);
            pointInfo.put("type", type);
            pointInfo.put("latitude", latitude);
            pointInfo.put("longitude", longitude);
            pointInfo.put("pdfX", pdfX);
            pointInfo.put("pdfY", pdfY);

            // 添加到数组中
            jsonArray.put(pointInfo);

            // 写入文件
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(jsonArray.toString());
            writer.close();
        } catch (JSONException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String getFileId(File file) throws NoSuchAlgorithmException {
        String filePath = file.getAbsolutePath();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(filePath.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String readFile(File file) throws IOException {
        java.util.Scanner scanner = new java.util.Scanner(file).useDelimiter("\\Z");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private void showFileInfoDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("文件信息");

        // 设置对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_file_info, null);
        builder.setView(dialogView);

        // 初始化对话框中的视图
        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        EditText latitudeEditText = dialogView.findViewById(R.id.latitudeEditText);
        EditText longitudeEditText = dialogView.findViewById(R.id.longitudeEditText);

        // 设置对话框按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            // 处理保存逻辑
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    public void showFileList() {
        pdfView.setVisibility(View.GONE);
        //backToListButton.setVisibility(View.GONE);
        pdfListView.setVisibility(View.VISIBLE);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBackButton(false);
        }
    }

    public List<PointInfo> readPointInfoFromLocal(File file) {
        List<PointInfo> pointInfoList = new ArrayList<>();
        try {
            // 获取当前打开的 PDF 文件的唯一标识
            String pdfFileId = getFileId(file);

            // 获取存储目录
            File storageDir = requireContext().getFilesDir();
            File jsonFile = new File(storageDir, pdfFileId + ".json");

            if (jsonFile.exists()) {
                // 读取文件内容
                String jsonContent = readFile(jsonFile);
                JSONArray jsonArray = new JSONArray(jsonContent);

                // 解析 JSON 数据
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject pointInfoObject = jsonArray.getJSONObject(i);
                    String name = pointInfoObject.getString("name");
                    String type = pointInfoObject.getString("type");
                    String latitude = pointInfoObject.getString("latitude");
                    String longitude = pointInfoObject.getString("longitude");
                    String pdfX = pointInfoObject.getString("pdfX");
                    String pdfY = pointInfoObject.getString("pdfY");

                    PointInfo pointInfo = new PointInfo(name, type, latitude, longitude, pdfX, pdfY);
                    pointInfoList.add(pointInfo);
                }
            }
        } catch (JSONException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return pointInfoList;
    }

    public String[] getPointNames() {
        List<PointInfo> pointInfoList = readPointInfoFromLocal(currentPDF);
        List<String> pointNames = new ArrayList<>();
        for (PointInfo pointInfo : pointInfoList) {
            pointNames.add(pointInfo.name);
        }
        return pointNames.toArray(new String[0]);
    }

    /**
     * 获取当前的 PDF 文件
     */
    public File getCurrentPDF() {
        return currentPDF;
    }

    public void updatePointInfo(PointInfo oldPoint, PointInfo newPoint) {
        try {
            // 获取当前打开的 PDF 文件的唯一标识
            String pdfFileId = getFileId(currentPDF);

            // 获取存储目录
            File storageDir = requireContext().getFilesDir();
            File jsonFile = new File(storageDir, pdfFileId + ".json");

            if (jsonFile.exists()) {
                // 读取已有的数据
                String jsonContent = readFile(jsonFile);
                JSONArray jsonArray = new JSONArray(jsonContent);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject pointInfoObject = jsonArray.getJSONObject(i);
                    if (pointInfoObject.getString("name").equals(oldPoint.name) &&
                            pointInfoObject.getString("latitude").equals(oldPoint.latitude) &&
                            pointInfoObject.getString("longitude").equals(oldPoint.longitude) &&
                            pointInfoObject.getString("pdfX").equals(oldPoint.pdfX) &&
                            pointInfoObject.getString("pdfY").equals(oldPoint.pdfY)) {
                        // 创建新的定位点信息对象
                        JSONObject newPointInfo = new JSONObject();
                        newPointInfo.put("name", newPoint.name);
                        newPointInfo.put("type", newPoint.type);
                        newPointInfo.put("latitude", newPoint.latitude);
                        newPointInfo.put("longitude", newPoint.longitude);
                        newPointInfo.put("pdfX", newPoint.pdfX);
                        newPointInfo.put("pdfY", newPoint.pdfY);
                        newPointInfo.put("isNormal", newPoint.isNormal);
                        newPointInfo.put("remark", newPoint.remark);

                        // 替换原有的对象
                        jsonArray.put(i, newPointInfo);
                        break;
                    }
                }

                // 写入文件
                FileWriter writer = new FileWriter(jsonFile);
                writer.write(jsonArray.toString());
                writer.close();
            }
        } catch (JSONException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // 实现 OnGestureListener 接口的方法
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    //用户按下屏幕后，100ms 内未移动或抬起手指
    @Override
    public void onShowPress(MotionEvent e) {
    }

    // 用户轻触屏幕后快速抬起手指（未触发滑动或长按）
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Toast.makeText(requireContext(), "onSingleTapUp", Toast.LENGTH_SHORT).show();
        return false;
    }

    // 用户在屏幕上滑动，e1 为第一次按下的事件，e2 为当前滑动的事件，distanceX 和 distanceY 分别为水平和垂直方向上的滑动距离
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {

        float screenX = e.getX();
        float screenY = e.getY();
        PointF pdfCoordinates = convertToPdfCoordinates(screenX, screenY);
        showPositionDialog(pdfCoordinates);
    }

    // 用户在屏幕上滑动，e1 为第一次按下的事件，e2 为当前滑动的事件，velocityX 和 velocityY 分别为水平和垂直方向上的滑动速度
    // 注意：滑动事件会在 onScroll 事件之后触发，因此如果需要处理滑动事件，需要在 onScroll 事件中判断是否为滑动事件
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Toast.makeText(requireContext(), "onFling", Toast.LENGTH_SHORT).show();
        return false;
    }

    // 实现 OnDoubleTapListener 接口的方法
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // 处理单击确认事件
        Toast.makeText(requireContext(), "单击确认事件触发", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // 处理双击事件
        Toast.makeText(requireContext(), "双击事件触发", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // 处理双击事件
        Toast.makeText(requireContext(), "双击事件触发", Toast.LENGTH_SHORT).show();
        return false;
    }

//    @Override
//    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.top_appbar_menu, menu);
//        this.mainMenu = menu;
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.search) {
            // 处理搜索按钮点击事件
            if (!isSearching){
                enableSearchMode();
                isSearching = true;
            }
            return true;
        }
        if (item.getItemId() == R.id.edit) {
            if (!isEditing) {
                enableEditMode();
                isEditing = true;
            }
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            // 处理返回按钮点击事件
//            getActivity().onBackPressed();
            if (isEditing) {
                disableEditMode();
                isEditing = false;
            }
            if (isSearching) {
                disableSearchMode();
                isSearching = false;
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void flashEditableList() {
        // 加载PDF文件列表
        pdfFiles = new ArrayList<>();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        findPDFs(externalStorageDirectory);
        // 刷新列表文件
        List<Map<String, Object>> data = new ArrayList<>();
        for (File file : pdfFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("pdfFileName", file.getName());
            if (file.getName().endsWith(".pdf")) {
                item.put("pdfIcon", R.drawable.ic_pdf);
            } else {
                item.put("pdfIcon", R.drawable.ic_document);
            }
            data.add(item);
        }

        // 使用自定义适配器
        EditableFileAdapter adapter = new EditableFileAdapter(data);
        pdfListView.setAdapter(adapter);
    }
    private void enableEditMode() {
        // 隐藏搜索和编辑按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            Menu menu = mainActivity.getMainMenu();
            if (menu != null) {
                mainActivity.setTopAppBarMenuVisibility(menu, false);
            }

            BottomNavigationView bottomNavigationView = mainActivity.findViewById(R.id.bottomNavigationView);

            // 设置重命名按钮的点击事件
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == R.id.rename) {
                    handleRename();
                    return true;
                }else if (item.getItemId() == R.id.delete) {
                    handleDelete();
                    return true;
                }else if (item.getItemId() == R.id.copy) {
                    handleCopy();
                    return true;
                }
                // 可以添加其他按钮的处理逻辑
                return false;
            });

        }
        // 显示返回按钮
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            //获取顶部栏
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                // 显示返回按钮
                //用户点击该按钮，系统会调用 Activity 的 onOptionsItemSelected() 方法
                //其中 item.getItemId() 为 android.R.id.home
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        // 替换列表项布局
        // 重新设置适配器，使用 list_item_pdf_editable.xml
        List<Map<String, Object>> data = new ArrayList<>();
        for (File file : pdfFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("pdfFileName", file.getName());
            if (file.getName().endsWith(".pdf")) {
                item.put("pdfIcon", R.drawable.ic_pdf);
            } else {
                item.put("pdfIcon", R.drawable.ic_document);
            }
            data.add(item);
        }

        // 使用自定义适配器
        EditableFileAdapter adapter = new EditableFileAdapter(data);
        pdfListView.setAdapter(adapter);

        // 获取父布局
        ViewGroup parentLayout = (ViewGroup) pdfListView.getParent();

        // 添加“全选”按钮
        if (parentLayout instanceof ConstraintLayout) {
            TextView selectAllTextView = new TextView(requireContext());
            selectAllTextView.setText("全选");
            selectAllTextView.setOnClickListener(v -> {
                boolean allChecked = true;
                for (int i = 0; i < pdfListView.getChildCount(); i++) {
                    CheckBox checkBox = pdfListView.getChildAt(i).findViewById(R.id.checkBox);
                    if (checkBox != null && !checkBox.isChecked()) {
                        allChecked = false;
                        break;
                    }
                }

                for (int i = 0; i < pdfListView.getChildCount(); i++) {
                    CheckBox checkBox = pdfListView.getChildAt(i).findViewById(R.id.checkBox);
                    if (checkBox != null) {
                        checkBox.setChecked(!allChecked);
                    }
                }
            });

            // 将“全选”按钮添加到父布局中
            parentLayout.addView(selectAllTextView, 0);
        }
        // 替换底部导航栏按钮
        // 可以动态替换菜单
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.replaceBottomNavigationMenu(R.menu.bottom_nav_menu_editable);
        }

        pdfListView.setOnItemClickListener(new  AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                android.widget.CheckBox checkBox = view.findViewById(R.id.checkBox);
                if (checkBox != null) {
                    checkBox.setChecked(!checkBox.isChecked());
                    updateBottomNavigationViewState();
                }
            }
        });
    }

    private void handleDelete() {
        List<File> checkedFiles = getCheckedFiles();
        if (checkedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "请选择要删除的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除选中的文件吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    for (File file : checkedFiles) {
                        if (file.delete()) {
                            pdfFiles.remove(file);
                            Log.d("FirstFragment", "文件删除成功: " + file.getAbsolutePath());
                        } else {
                            Log.e("FirstFragment", "文件删除失败: " + file.getAbsolutePath());
                        }
                    }
                    // 刷新文件列表
                    flashEditableList();
                })
                .setNegativeButton("取消", null)
                .show();
    }

// ... 已有代码 ...



    private void handleCopy() {
        List<File> checkedFiles = getCheckedFiles();
        if (checkedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "请选择要复制的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        for (File sourceFile : checkedFiles) {
            String originalName = sourceFile.getName();
            String fileNameWithoutExtension = originalName;
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex != -1) {
                fileNameWithoutExtension = originalName.substring(0, dotIndex);
                extension = originalName.substring(dotIndex);
            }

            File parentDir = sourceFile.getParentFile();
            int n = 1;
            File destFile;
            do {
                String newName;
                if (n == 1) {
                    newName = fileNameWithoutExtension + "(1)" + extension;
                } else {
                    // 提取已有的 (n) 部分并替换
                    Pattern pattern = Pattern.compile("\\(\\d+\\)$");
                    Matcher matcher = pattern.matcher(fileNameWithoutExtension);
                    if (matcher.find()) {
                        fileNameWithoutExtension = fileNameWithoutExtension.replace(matcher.group(), "");
                    }
                    newName = fileNameWithoutExtension.trim() + "(" + n + ")" + extension;
                }
                destFile = new File(parentDir, newName);
                n++;
            } while (destFile.exists());

            try {
                copyFile(sourceFile, destFile);
                pdfFiles.add(destFile);
                Log.d("FirstFragment", "文件复制成功: " + sourceFile.getAbsolutePath() + " -> " + destFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e("FirstFragment", "文件复制失败: " + sourceFile.getAbsolutePath(), e);
                Toast.makeText(requireContext(), "文件复制失败: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
            }
        }
        flashEditableList();
        //loadPDFs();
    }


    private void copyFile(File source, File destination) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
    private void disableEditMode() {
        // 隐藏返回按钮
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        }
        // 恢复底部导航栏按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.replaceBottomNavigationMenu(R.menu.bottom_nav_menu);

            BottomNavigationView bottomNavigationView = mainActivity.findViewById(R.id.bottomNavigationView);
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

        }

        // 恢复列表项布局
        // 重新设置适配器，使用原来的布局
        List<Map<String, Object>> data = new ArrayList<>();
        for (File file : pdfFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("pdfFileName", file.getName());
            if (file.getName().endsWith(".pdf")) {
                item.put("pdfIcon", R.drawable.ic_pdf);
            } else {
                item.put("pdfIcon", R.drawable.ic_document);
            }
            data.add(item);
        }
        SimpleAdapter adapter = new SimpleAdapter(requireContext(), data,
                R.layout.list_item_pdf,
                new String[]{"pdfIcon", "pdfFileName"},
                new int[]{R.id.pdfIcon, R.id.pdfFileName});
        pdfListView.setAdapter(adapter);

        // 修改类型转换为 ViewGroup
        ViewGroup parentLayout = (ViewGroup) pdfListView.getParent();
        TextView selectAllTextView = null;
        for (int i = 0; i < parentLayout.getChildCount(); i++) {
            View child = parentLayout.getChildAt(i);
            if (child instanceof TextView && ((TextView) child).getText().equals("全选")) {
                selectAllTextView = (TextView) child;
                break;
            }
        }
        if (selectAllTextView != null) {
            parentLayout.removeView(selectAllTextView);
        }

        // 恢复底部导航栏按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.replaceBottomNavigationMenu(R.menu.bottom_nav_menu);
        }
        //恢复顶部导航栏按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setTopAppBarMenuVisibility(mainActivity.getMainMenu(), true);
        }
    }

    private void filterFiles(String query) {
        List<File> filteredFiles = new ArrayList<>();
        if (query.isEmpty()) {
            filteredFiles.addAll(allPdfFiles);
        } else {
            for (File file : allPdfFiles) {
                if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredFiles.add(file);
                }
            }
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (File file : filteredFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("pdfFileName", file.getName());
            if (file.getName().endsWith(".pdf")) {
                item.put("pdfIcon", R.drawable.ic_pdf);
            } else {
                item.put("pdfIcon", R.drawable.ic_document);
            }
            data.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(requireContext(), data,
                R.layout.list_item_pdf,
                new String[]{"pdfIcon", "pdfFileName"},
                new int[]{R.id.pdfIcon, R.id.pdfFileName});
        pdfListView.setAdapter(adapter);
    }

    private void enableSearchMode() {
        // 备份所有文件列表
        allPdfFiles = new ArrayList<>(pdfFiles);

        // 显示搜索输入框
        searchView.setVisibility(View.VISIBLE);

        // 显示顶部栏的返回按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            ActionBar actionBar = mainActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void disableSearchMode() {
        // 隐藏搜索输入框
        searchView.setVisibility(View.GONE);
        searchView.setQuery("", false);
        searchView.clearFocus();

        // 恢复原始文件列表
        pdfFiles = new ArrayList<>(allPdfFiles);
        loadPDFs();

        // 隐藏顶部栏的返回按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            ActionBar actionBar = mainActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    private void updateBottomNavigationViewState() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            BottomNavigationView bottomNavigationView = mainActivity.findViewById(R.id.bottomNavigationView);

            // 清除默认选中项
            bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
            bottomNavigationView.setSelectedItemId(-1);

            // 统计勾选的文件数量
            int checkedCount = 0;
            for (int i = 0; i < pdfListView.getChildCount(); i++) {
                android.widget.CheckBox checkBox = pdfListView.getChildAt(i).findViewById(R.id.checkBox);
                if (checkBox != null && checkBox.isChecked()) {
                    checkedCount++;
                }
            }

            // 获取底部导航栏的菜单项
            MenuItem renameItem = bottomNavigationView.getMenu().findItem(R.id.rename);
            MenuItem deleteItem = bottomNavigationView.getMenu().findItem(R.id.delete);
            MenuItem copyItem = bottomNavigationView.getMenu().findItem(R.id.copy);
            MenuItem uploadItem = bottomNavigationView.getMenu().findItem(R.id.upload);

            // 根据勾选数量启用或禁用菜单项
            if (checkedCount == 0) {
                renameItem.setEnabled(false);
                deleteItem.setEnabled(false);
                copyItem.setEnabled(false);
                uploadItem.setEnabled(false);
            } else if (checkedCount == 1) {
                renameItem.setEnabled(true);
                deleteItem.setEnabled(true);
                copyItem.setEnabled(true);
                uploadItem.setEnabled(true);
            } else {
                renameItem.setEnabled(false);
                deleteItem.setEnabled(true);
                copyItem.setEnabled(true);
                uploadItem.setEnabled(true);
            }
        }
    }

    private void handleRename() {
        List<File> checkedFiles = getCheckedFiles();
        if (checkedFiles.size() == 1) {
            File fileToRename = checkedFiles.get(0);
            showRenameDialog(fileToRename);
        }
    }

    private List<File> getCheckedFiles() {
        List<File> checkedFiles = new ArrayList<>();
        for (int i = 0; i < pdfListView.getChildCount(); i++) {
            CheckBox checkBox = pdfListView.getChildAt(i).findViewById(R.id.checkBox);
            if (checkBox != null && checkBox.isChecked()) {
                checkedFiles.add(pdfFiles.get(i));
            }
        }
        return checkedFiles;
    }

    private void showRenameDialog(File fileToRename) {
        if (!checkStoragePermission()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("重命名文件");

        // 创建输入框
        final EditText input = new EditText(requireContext());
        input.setText(fileToRename.getName());
        builder.setView(input);

        // 设置确定和取消按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    File parentDir = fileToRename.getParentFile();
                    File newFile = new File(parentDir, newName);
                    if (newFile.exists()) {
                        android.widget.Toast.makeText(requireContext(), "该文件名已存在，请重新输入", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (fileToRename.renameTo(newFile)) {
                        Log.d("FirstFragment", "文件重命名成功");
                        Toast.makeText(requireContext(), "文件重命名成功", Toast.LENGTH_SHORT).show();
                        // 重命名成功，更新文件列表
                        pdfFiles.remove(fileToRename);
                        pdfFiles.add(newFile);
                        loadPDFs();
                    } else {
                        // 重命名失败，提示用户
                        android.widget.Toast.makeText(requireContext(), "重命名失败", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    private class EditableFileAdapter extends BaseAdapter {
        private List<Map<String, Object>> data;

        // 在 EditableFileAdapter 类中添加一个列表来保存 CheckBox 的状态
        private List<Boolean> checkBoxStates;

        public EditableFileAdapter(List<Map<String, Object>> data) {
            this.data = data;
            checkBoxStates = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                checkBoxStates.add(false);
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_pdf_editable, parent, false);
                holder = new ViewHolder();
                holder.checkBox = convertView.findViewById(R.id.checkBox);
                holder.pdfIcon = convertView.findViewById(R.id.pdfIcon);
                holder.pdfFileName = convertView.findViewById(R.id.pdfFileName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Map<String, Object> item = data.get(position);
            holder.pdfIcon.setImageResource((int) item.get("pdfIcon"));
            holder.pdfFileName.setText((String) item.get("pdfFileName"));

            holder.checkBox.setChecked(checkBoxStates.get(position));

            // 给 CheckBox 设置点击监听器
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBoxStates.set(position, holder.checkBox.isChecked());
                    if (isEditing) {
                        updateBottomNavigationViewState();
                    }
                }
            });

            return convertView;
        }

        private class ViewHolder {
            CheckBox checkBox;
            ImageView pdfIcon;
            TextView pdfFileName;
        }
    }
}

// 定义定位点信息类
class PointInfo {
    String name;
    String type;
    String latitude;
    String longitude;
    String pdfX;
    String pdfY;
    boolean isNormal;
    String remark;

    // 新增 8 个参数的构造函数
    public PointInfo(String name, String type, String latitude, String longitude, String pdfX, String pdfY) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pdfX = pdfX;
        this.pdfY = pdfY;
        this.remark = "";
    }
    public PointInfo(String name, String type, String latitude, String longitude, String pdfX, String pdfY, String remark) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pdfX = pdfX;
        this.pdfY = pdfY;
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "PointInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", pdfX='" + pdfX + '\'' +
                ", pdfY='" + pdfY + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }


}

