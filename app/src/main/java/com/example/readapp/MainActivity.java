package com.example.readapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView navView;
    private NavController navController;
    private static final int AUTHED_MENU_RES = R.menu.bottom_nav_menu;
    private static final int GUEST_MENU_RES = R.menu.guest_nav_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupNavController();
        configureDynamicNavigation();
    }

    private void initViews() {
        navView = findViewById(R.id.nav_view);
    }

    /**
     * 设置导航控制器基础配置
     */
    private void setupNavController() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
    }

    /**
     * 配置动态导航菜单系统
     */
    private void configureDynamicNavigation() {
        // 首次加载时刷新菜单
        refreshNavigationMenu();

        // 设置自定义导航监听器
        navView.setOnNavigationItemSelectedListener(item -> {
            // 拦截游客访问受限页面
            if (isRestrictedItem(item.getItemId())) {
                handleRestrictedAccess();
                return false;
            }

            // 执行默认导航行为
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    /**
     * 刷新导航菜单（根据登录状态动态加载）
     */
    private void refreshNavigationMenu() {
        // 清空当前菜单项
        navView.getMenu().clear();

        // 根据用户状态加载对应菜单
        int menuRes = isLoggedIn() ? AUTHED_MENU_RES : GUEST_MENU_RES;
        navView.inflateMenu(menuRes);

        // 重新绑定导航控制器
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * 检查是否为受限菜单项（游客不可访问）
     * @param itemId 菜单项ID
     * @return 如果是受限项返回true，否则返回false
     */
    private boolean isRestrictedItem(int itemId) {
        return !isLoggedIn() && (itemId == R.id.navigation_dashboard
                || itemId == R.id.navigation_notifications);
    }

    /**
     * 处理受限访问的情况
     */
    private void handleRestrictedAccess() {
        // 显示提示信息
        Toast.makeText(this, "请登录后使用完整功能", Toast.LENGTH_LONG).show();

        // 跳转到登录页面
        startActivity(new Intent(this, LoginActivity.class));
    }

    /**
     * 检查用户是否已登录
     * @return 如果已登录返回true，否则返回false
     */
    private boolean isLoggedIn() {
        // 从SharedPreferences获取登录状态
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        return prefs.contains("username");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回界面时刷新菜单状态
        refreshNavigationMenu();
    }
}