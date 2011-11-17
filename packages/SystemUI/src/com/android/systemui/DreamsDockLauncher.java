package com.android.systemui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Slog;

public class DreamsDockLauncher extends Activity {
    private static final String TAG = "DreamsDockLauncher";

    // Launch the screen saver if started as an activity.
    @Override
    protected void onCreate (Bundle icicle) {
        super.onCreate(icicle);
        launchDream(this);
        finish();
    }

    private static void launchDream(Context context) {
        try {
            String component = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.DREAM_COMPONENT);
            if (component == null) {
                component = context.getResources().getString(
                    com.android.internal.R.string.config_defaultDreamComponent);
            }
            if (component != null) {
                ComponentName cn = ComponentName.unflattenFromString(component);
                Intent zzz = new Intent(Intent.ACTION_MAIN)
                    .setComponent(cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        );
                Slog.v(TAG, "Starting screen saver on dock event: " + component);
                context.startActivity(zzz);
            } else {
                Slog.e(TAG, "Couldn't start screen saver: none selected");
            }
        } catch (android.content.ActivityNotFoundException exc) {
            // no screensaver? give up
            Slog.e(TAG, "Couldn't start screen saver: none installed");
        }
    }

    // Trap low-level dock events and launch the screensaver.
    public static class DockEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                int state = extras
                        .getInt(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
                if (state == Intent.EXTRA_DOCK_STATE_DESK
                        || state == Intent.EXTRA_DOCK_STATE_LE_DESK
                        || state == Intent.EXTRA_DOCK_STATE_HE_DESK) {
                    launchDream(context);
                }
            }
        }
    }
}
