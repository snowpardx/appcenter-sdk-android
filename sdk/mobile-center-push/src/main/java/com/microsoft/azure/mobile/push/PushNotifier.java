package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

class PushNotifier {

    /**
     * Default channel identifier on Android 8.
     */
    private static final String CHANNEL_ID = "app_center_push";

    /**
     * Default channel name on Android 8.
     */
    private static final String CHANNEL_NAME = "Push";

    /**
     * Builds a push notification using the given context and intent.
     *
     * @param context    The current context.
     * @param pushIntent The intent that is associated with the push.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    static void handleNotification(Context context, Intent pushIntent) throws RuntimeException {
        context = context.getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        /* Generate notification identifier using the hash of the Google message id. */
        String messageId = PushIntentUtils.getGoogleMessageId(pushIntent);
        if (messageId == null) {
            MobileCenterLog.error(Push.getInstance().getLoggerTag(), "Push notification did not" +
                    "contain Google message ID; aborting notification processing.");
            return;
        }
        int notificationId = messageId.hashCode();

        /* Click action. */
        PackageManager packageManager = context.getPackageManager();
        Intent actionIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Map<String, String> customData = PushIntentUtils.getCustomData(pushIntent);
        for (String key : customData.keySet()) {
            actionIntent.putExtra(key, customData.get(key));
        }

        /* Set the message ID in the intent. */
        PushIntentUtils.setGoogleMessageId(messageId, actionIntent);

        /* Get text. */
        String notificationTitle = PushIntentUtils.getTitle(pushIntent);
        String notificationMessage = PushIntentUtils.getMessage(pushIntent);
        Notification.Builder builder = new Notification.Builder(context);

        /* Set color. */
        setColor(pushIntent, builder);

        /* Set icon. */
        setIcon(context, pushIntent, builder);

        /* Set sound. */
        setSound(context, pushIntent, builder);

        /* Texts */
        builder.setContentTitle(notificationTitle).
                setContentText(notificationMessage).
                setWhen(System.currentTimeMillis());

        /* Click action. Reuse notification id for simplicity. */
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId,
                actionIntent, 0);
        builder.setContentIntent(contentIntent);

        /* Manage notification channel on Android O. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O) {

            /* Get channel. */
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            /* Create or update channel. */
            //noinspection ConstantConditions
            notificationManager.createNotificationChannel(channel);

            /* And associate to notification. */
            builder.setChannelId(channel.getId());
        }
        Notification notification;

        /* Build method depends on versions. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //noinspection ConstantConditions
        notificationManager.notify(notificationId, notification);
    }

    /**
     * Sets the color in the notification builder if the property is set in the intent.
     *
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static void setColor(Intent pushIntent, Notification.Builder builder) {
        String colorString = PushIntentUtils.getColor(pushIntent);
        if (colorString != null) {
            builder.setColor(Color.parseColor(colorString));
        }
    }


    /**
     * Sets the sound in the notification builder if the property is set in the intent.
     *
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static void setSound(Context context, Intent pushIntent, Notification.Builder builder) {
        if (!PushIntentUtils.useAnySound(pushIntent)) {
            return;
        }
        String customSound = PushIntentUtils.getCustomSound(pushIntent);
        if (customSound == null) {
            builder.setDefaults(Notification.DEFAULT_SOUND);
            return;
        }
        try {
            Resources resources = context.getResources();
            int id = resources.getIdentifier(customSound, "raw", context.getPackageName());
            Uri soundUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(resources.getResourcePackageName(id))
                    .appendPath(resources.getResourceTypeName(id))
                    .appendPath(resources.getResourceEntryName(id))
                    .build();
            builder.setSound(soundUri);
        } catch (Resources.NotFoundException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(),
                    "Sound file '" + customSound + "' not found; falling back to default.");
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }
    }


    /**
     * Sets the icon for the notification builder if the property is set in the intent, if no custom
     * icon is provided as an extra, the app icon is used.
     *
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static void setIcon(Context context, Intent pushIntent, Notification.Builder builder) {
        String iconString = PushIntentUtils.getIcon(pushIntent);
        if (iconString != null) {
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            int iconResourceId = resources.getIdentifier(iconString, "drawable", packageName);
            if (iconResourceId != 0) {
                MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                        "Found icon resource in 'drawable'.");
                builder.setSmallIcon(iconResourceId);
                return;
            }
            iconResourceId = resources.getIdentifier(iconString, "mipmap", packageName);
            if (iconResourceId != 0) {
                MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                        "Found icon resource in 'mipmap'.");
                builder.setSmallIcon(iconResourceId);
                return;
            }
        }
        MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                "Using application icon as notification icon.");
        builder.setSmallIcon(context.getApplicationInfo().icon);
    }
}


