package com.ruibang.pointmeasuretoolsv6;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.ruibang.pointmeasuretoolsv6.databinding.FragmentFirstBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;



public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private Button backToListButton;
    private ProgressDialog progressDialog;
    private PDFView pdfView;
    private ListView pdfListView;
    private List<File> pdfFiles;
    private static final int PERMISSION_REQUEST_CODE = 1;
    // 权限请求码
    private static final int REQUEST_CODE_READ_STORAGE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private static float mPageWidth;    // 页面宽度
    private static float mPageHeight;   // 页面高度
    private static float mScaleFactor;  // 缩放因子

    private LocationManager locationManager;
    private static final long MIN_TIME_BW_UPDATES = 1000;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

    private File currentPDF;//当前PDF
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
        backToListButton = binding.backToListButton;
        backToListButton.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdfView.setVisibility(View.GONE);
                //pdfOperationHint.setVisibility(View.GONE);
                backToListButton.setVisibility(View.GONE);
                pdfListView.setVisibility(View.VISIBLE);

                // 隐藏返回按钮
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showBackButton(false);
                }
            }
        });


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
            backToListButton.setVisibility(View.VISIBLE);

            Log.d("FirstFragment", "尝试加载 PDF 文件: " + file.getAbsolutePath());

            // 显示返回按钮
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // 显示返回按钮
                mainActivity.showBackButton(true);
                // 为返回按钮设置点击监听器
//                mainActivity.findViewById(R.id.backButton).setOnClickListener(v -> {
//                    //showFileList();
//                    mainActivity.invalidateOptionsMenu();
//                    // 隐藏返回按钮
//                    //mainActivity.showBackButton(false);
//                });
                // 获取 Menu 对象
                Menu menu = mainActivity.getMainMenu();
                if (menu != null) {
                    // 隐藏搜索和编辑按钮
                    mainActivity.setTopAppBarMenuVisibility(menu, false);
                }                // 隐藏搜索和编辑按钮
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

            // 创建 GestureDetector 来处理长按事件
            GestureDetector gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
                // 处理单击事件
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    // 在这里添加单击事件的处理逻辑
                    Toast.makeText(requireContext(), "单击事件触发", Toast.LENGTH_SHORT).show();
                    return true;
                }
                @Override
                public void onLongPress(MotionEvent e) {
                    float screenX = e.getX();
                    float screenY = e.getY();
                    PointF pdfCoordinates = convertToPdfCoordinates(screenX, screenY);
                    showPositionDialog(pdfCoordinates);
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    // 处理双击事件
                    return true;
                }

            });


//            pdfView.setOnLongClickListener((v, event) -> {
//                // 返回 gestureDetector.onTouchEvent(event) 的结果，让其处理触摸事件
//                return gestureDetector.onTouchEvent(event);
//            });
            // 确保触摸事件能传递到长按监听器
            pdfView.setOnTouchListener((v, event) -> {

                // 返回 gestureDetector.onTouchEvent(event) 的结果，让其处理触摸事件
                return gestureDetector.onTouchEvent(event);

            });
            // 添加长按监听器
//            pdfView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    float x = v.getX();
//                    float y = v.getY();
//                    // 获取文档上的位置，这里简单假设屏幕位置和文档位置一致，实际可能需要转换
//                    // 可根据 pdfView 的缩放、滚动等状态进行精确转换
//                    showPositionDialog(x, y);
//                    return true;
//                }
//            });
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
        EditText remarkEditText = dialogView.findViewById(R.id.remarkEditText);

        // 设置对话框按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            // 处理保存逻辑
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    public void showFileList() {
        pdfView.setVisibility(View.GONE);
        backToListButton.setVisibility(View.GONE);
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