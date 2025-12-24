package com.example.readapp.ui.notifications;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.readapp.*;

import java.io.File;

public class NotificationsFragment extends Fragment {

    private static final int REQUEST_CODE_PERMISSION = 1001;
    private ImageView ivAvatar;
    private View rootView;
    private TextView tvUsername;
    private TextView tvUser;
    private TextView tvAge;
    private SharedPreferences sharedPreferences;
    private MyDBUtils dbHelper;
    private int currentUserId;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private String nowage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        // 初始化组件
        initViews(root);
        initDatabase();
        initUserInfo();
        setupImagePicker();
        setupClickListeners(root);
        loadCurrentAvatar();

        return root;
    }


    private void initViews(View root) {
        tvUsername = root.findViewById(R.id.tv_username);
        ivAvatar = root.findViewById(R.id.iv_avatar);
        tvUser = root.findViewById(R.id.tv_user);
        tvAge = root.findViewById(R.id.tv_age);
    }

    private void initDatabase() {
        dbHelper = new MyDBUtils(requireContext());
    }

    //初始化用户信息
    private void initUserInfo() {
        sharedPreferences = requireActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("Uid", -1);
        String username = sharedPreferences.getString("username", "");
        tvUsername.setText(username);
        tvUser.setText("用户");
        tvAge.setText("年龄：" + dbHelper.getUserById(currentUserId).getAge());
        nowage =  dbHelper.getUserById(currentUserId).getAge();
        if(currentUserId==1)
        {
            tvUser.setText("管理员");
            //点击该文本，进入管理员界面
            tvUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(requireActivity(), AdminActivity.class);
                    intent.putExtra("flag", "Xieesizhu1120");
                    startActivity(intent);
                }
            });
        }
        if (isGuestUser())
        {
            tvUser.setText("游客");
        }
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        handleImageSelection(selectedImageUri);
                    }
                }
        );
    }

    private void setupClickListeners(View root) {
        LinearLayout menuChangePassword = root.findViewById(R.id.menu_change_password);
        LinearLayout menuReadingHistory = root.findViewById(R.id.menu_reading_history);
        LinearLayout menuLogout = root.findViewById(R.id.menu_logout);
        LinearLayout menuEditUsername = root.findViewById(R.id.menu_edit_username);
        LinearLayout menuEditAge = root.findViewById(R.id.menu_edit_age);

        ivAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        menuChangePassword.setOnClickListener(v -> openChangePassword());
        menuLogout.setOnClickListener(v -> logout());
        menuReadingHistory.setOnClickListener(v -> {
            if (isGuestUser()) {
                Toast.makeText(requireContext(), "游客无法查看阅读记录", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(requireActivity(), ReadingHistoryActivity.class));
        });
        menuEditUsername.setOnClickListener(v -> openEditDialog("username"));
        menuEditAge.setOnClickListener(v -> openEditDialog("age"));
    }

    private void openEditDialog(String fieldType) {
        if (isGuestUser()) {
            Toast.makeText(requireContext(), "游客无法修改信息", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_info, null);
        EditText etInput = view.findViewById(R.id.et_input);
        TextView tvTitle = view.findViewById(R.id.tv_title);

        String title = "";
        String currentValue = "";
        InputFilter[] filters = new InputFilter[0];

        switch (fieldType) {
            case "username":
                title = "修改用户名";
                currentValue = sharedPreferences.getString("username", "");
                filters = new InputFilter[]{new InputFilter.LengthFilter(20)};
                break;
            case "age":
                title = "修改年龄";
                currentValue = nowage;
                filters = new InputFilter[]{new InputFilter.LengthFilter(3), (source, start, end, dest, dstart, dend) -> {
                    if (source.length() > 0 && !Character.isDigit(source.charAt(0))) {
                        return "";
                    }
                    return null;
                }};
                break;
        }

        tvTitle.setText(title);
        etInput.setText(currentValue);
        etInput.setFilters(filters);

        builder.setView(view)
                .setPositiveButton("确认", (dialog, which) -> {
                    String newValue = etInput.getText().toString().trim();
                    if (validateInput(fieldType, newValue)) {
                        updateUserInfo(fieldType, newValue);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean validateInput(String fieldType, String value) {
        if (value.isEmpty()) {
            Toast.makeText(requireContext(), "输入不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fieldType.equals("age")) {
            try {
                int age = Integer.parseInt(value);
                if (age < 0 || age > 150) {
                    Toast.makeText(requireContext(), "请输入有效年龄（0-150）", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "年龄必须为数字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void updateUserInfo(String fieldType, String newValue) {
        new Thread(() -> {
            boolean success = false;
            MyDBUtils db = new MyDBUtils(requireContext());

            switch (fieldType) {
                case "username":
                    success = db.updateUserName(currentUserId, newValue);
                    break;
                case "age":
                    success = db.updateUserAge(currentUserId, newValue);
                    break;
            }

            if (success) {
                updateLocalPreferences(fieldType, newValue);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show();
                    refreshUserInfoDisplay();
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "修改失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateLocalPreferences(String field, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (field) {
            case "username":
                editor.putString("username", value);
                break;
            case "age":
                editor.putString("age", value);
                break;
        }
        editor.apply();
    }

    private void refreshUserInfoDisplay() {
        String username = sharedPreferences.getString("username", "");
        String age = sharedPreferences.getString("age", "未设置");

        tvUsername.setText(username);
        tvAge.setText("年龄：" + age);
    }

    //加载头像
    private void loadCurrentAvatar() {
        new Thread(() -> {
            String avatarPath = dbHelper.getUserAvatar(currentUserId);
            requireActivity().runOnUiThread(() -> {
                if (avatarPath != null && !avatarPath.isEmpty()) {
                    File avatarFile = new File(requireContext().getFilesDir(), avatarPath);
                    Glide.with(requireContext())
                            .load(avatarFile)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivAvatar);
                }
            });
        }).start();
    }

    //申请权限
    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            openImagePicker();
        }
    }

    //跳转到图像选择器
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }


    //图像选择器
    private void handleImageSelection(Uri selectedImageUri) {
        new Thread(() -> {
            String savedPath = FileUtils.saveAvatarToPrivateStorage(requireContext(), selectedImageUri);

            if (savedPath != null) {
                boolean success = dbHelper.updateUserAvatar(currentUserId, savedPath);

                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        updateAvatarDisplay(savedPath);
                        Toast.makeText(requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "头像更新失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void updateAvatarDisplay(String savedPath) {
        File avatarFile = new File(requireContext().getFilesDir(), savedPath);
        Glide.with(requireContext())
                .load(avatarFile)
                .circleCrop()
                .into(ivAvatar);
    }

    //申请权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "需要存储权限来选择头像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //修改密码跳转
    private void openChangePassword() {
        Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
        intent.putExtra("username", sharedPreferences.getString("username", ""));
        startActivity(intent);
    }







    //登出功能
    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认退出")
                .setMessage("确定要退出当前账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearUserData();
                    navigateToLogin();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    //登出后清理用户数据
    private void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username")
                .remove("Uid")
                .apply();

        SharedPreferences dataPrefs = requireActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = dataPrefs.edit();
        editor.remove("userName")
                .remove("is")
                .remove("psw")
                .remove("Uid")
                .apply();
    }

    //登出跳转
    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 检查游客身份
        if (isGuestUser()) {
            showGuestWarning();
            // 延迟返回，确保 Fragment 已正确附加
            view.postDelayed(() -> navigateBackSafely(), 100);
        }
    }

    // 判断是否为游客
    private boolean isGuestUser() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user_info", Context.MODE_PRIVATE);
        return !prefs.contains("username");
    }

    // 显示提示并安全返回
    private void showGuestWarning() {
        Toast.makeText(requireContext(),
                "游客身份无法访问，仅可访问主页及阅读功能",
                Toast.LENGTH_LONG).show();
    }

    // 安全返回上一页
    private void navigateBackSafely() {
        if (isAdded() && !requireActivity().isFinishing()) {
            requireActivity().onBackPressed();
        }
    }

}