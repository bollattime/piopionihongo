package com.piopionihongo.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * Günlük "kanji çalışma vakti" hatırlatıcısı.
 * - Her tetiklenişte bildirimi gösterir ve ertesi günün alarmını kurar (tek seferlik alarm zinciri).
 * - Telefon yeniden başladığında / uygulama güncellendiğinde alarmı yeniden kurar.
 * - Mümkünse kesin (exact) alarm kullanır; izin yoksa Doze'da da çalışan esnek alarma düşer.
 */
public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL_ID = "kanji_reminder_v2";
    static final String OLD_CHANNEL_ID = "kanji_reminder";
    static final String PREFS = "kanji_notify";
    static final String ACTION_FIRE = "com.piopionihongo.app.REMINDER_FIRE";
    private static final int ALARM_RC = 4201;
    private static final int NOTIF_ID = 4202;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_FIRE.equals(action)) {
            showReminder(context);
        }
        // Her durumda (tetiklenme, açılış, güncelleme) bir sonraki alarmı kur
        schedule(context);
    }

    /** Bildirim kanalını (yüksek önem → ekranın üstünden açılır) oluşturur, eski kanalı siler. */
    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(OLD_CHANNEL_ID) != null) nm.deleteNotificationChannel(OLD_CHANNEL_ID);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            boolean tr = isTurkish(c);
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    tr ? "Kanji hatırlatıcı" : "Kanji reminder", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription(tr ? "Günlük kanji çalışma hatırlatması ve güncellemeler"
                    : "Daily kanji study reminder and updates");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
    }

    static boolean isTurkish(Context c) {
        return !"en".equals(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("lang", "tr"));
    }

    /** Hatırlatma bildirimini hemen gösterir. */
    static void showReminder(Context c) {
        boolean tr = isTurkish(c);
        post(c, NOTIF_ID, tr ? "漢字 Kanji çalışma vakti!" : "漢字 Time for kanji!",
                tr ? "Bugünkü kanji hedefin seni bekliyor. Birkaç kart çalışalım mı?"
                   : "Your daily kanji goal is waiting. Study a few cards?");
    }

    /** Başlık/metinle bir bildirim gösterir (hatırlatma, güncelleme, test). */
    static void post(Context c, int id, String title, String text) {
        ensureChannel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Intent launch = new Intent(c, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(c, id, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(c, CHANNEL_ID) : new Notification.Builder(c);
        b.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_stat_kanji)
                .setColor(0xFFF5B400)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
        }
        nm.notify(id, b.build());
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_FIRE);
        return PendingIntent.getBroadcast(c, ALARM_RC, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Kayıtlı saate göre bir sonraki alarmı kurar (kapalıysa iptal eder). */
    static void schedule(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        if (!p.getBoolean("enabled", false)) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, p.getInt("hour", 9));
        cal.set(Calendar.MINUTE, p.getInt("minute", 0));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);
        long at = cal.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= 23) {
            if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
