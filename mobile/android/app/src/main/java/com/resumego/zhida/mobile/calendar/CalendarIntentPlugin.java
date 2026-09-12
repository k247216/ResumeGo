package com.resumego.zhida.mobile.calendar;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.CalendarContract;
import androidx.annotation.Nullable;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.TimeZone;

/**
 * 拉起系统日历的「新建事件」预填页，由日历 App 自己写入。
 *
 * 走 ACTION_INSERT 而不是分享 .ics：AOSP 与国产日历的 manifest 都没有 text/calendar 的 intent-filter，
 * 分享面板里压根没有日历 App。ACTION_INSERT 由日历 App 主动接收，且官方文档确认无需 READ/WRITE_CALENDAR 权限。
 */
@CapacitorPlugin(name = "CalendarIntent")
public class CalendarIntentPlugin extends Plugin {

    @PluginMethod
    public void openCreateEvent(PluginCall call) {
        String title = call.getString("title");
        Long beginTime = call.getLong("beginTime");
        Long endTime = call.getLong("endTime");
        if (title == null || title.isEmpty() || beginTime == null || endTime == null) {
            call.reject("BAD_ARGUMENTS", "BAD_ARGUMENTS");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, title);
        putTextExtra(call, intent, CalendarContract.Events.DESCRIPTION, "description");
        putTextExtra(call, intent, CalendarContract.Events.EVENT_LOCATION, "location");

        // 时间由 JS 侧换算成 UTC 毫秒后传入，这里绝不解析 ISO 字符串，避免时区二次偏移。
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.longValue());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.longValue());
        Boolean allDay = call.getBoolean("allDay");
        if (allDay != null && allDay.booleanValue()) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
        }
        intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        // 不放 REMINDERS_* 相关 extras：官方只保证直插 ContentProvider 时生效，走 Intent 不保证，
        // 所以文案上也不向用户承诺「日历里也会提醒」。

        try {
            getActivity().startActivity(intent);
            call.resolve(new JSObject());
        } catch (ActivityNotFoundException e) {
            // 不做 intent.resolveActivity() 前置探活：manifest 没有 <queries>，
            // Android 11+ 的包可见性会让它在实际能拉起的机型上误报 null。
            call.reject("NO_CALENDAR_APP", "NO_CALENDAR_APP");
        } catch (SecurityException e) {
            call.reject("CALENDAR_INTENT_FAILED", "CALENDAR_INTENT_FAILED");
        } catch (Exception e) {
            call.reject("CALENDAR_INTENT_FAILED", "CALENDAR_INTENT_FAILED");
        }
    }

    private void putTextExtra(PluginCall call, Intent intent, String extraKey, String argKey) {
        @Nullable
        String value = call.getString(argKey);
        if (value != null && !value.isEmpty()) {
            intent.putExtra(extraKey, value);
        }
    }
}
