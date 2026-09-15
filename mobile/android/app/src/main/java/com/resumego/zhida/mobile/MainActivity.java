package com.resumego.zhida.mobile;

import android.os.Bundle;
import androidx.core.splashscreen.SplashScreen;
import com.getcapacitor.BridgeActivity;
import com.resumego.zhida.mobile.calendar.CalendarIntentPlugin;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 必须最早调用，且早于 super.onCreate()：installSplashScreen 负责注入启动图标
        // 并在首帧后按 postSplashScreenTheme 切回应用主题。少了这一行，主题里的
        // windowSplashScreenAnimatedIcon 只是一份不生效的声明，启动时仍是纯色闪屏。
        SplashScreen.installSplashScreen(this);
        // 必须早于 super.onCreate()：BridgeActivity.load() 在其内部执行，
        // 而自动注册只读 assets/capacitor.plugins.json（只含 npm 包插件），app 模块自写的类要手动登记。
        registerPlugin(CalendarIntentPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
