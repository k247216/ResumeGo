package com.resumego.zhida.mobile;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.resumego.zhida.mobile.calendar.CalendarIntentPlugin;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 必须早于 super.onCreate()：BridgeActivity.load() 在其内部执行，
        // 而自动注册只读 assets/capacitor.plugins.json（只含 npm 包插件），app 模块自写的类要手动登记。
        registerPlugin(CalendarIntentPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
