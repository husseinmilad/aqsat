package ir.aqsat.app

import android.app.*
import android.content.*
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

object Notifier {
    private const val CH = "aqsat"

    fun notifyNow(c: Context, id: Int, title: String, text: String) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(NotificationChannel(CH, "سررسید اقساط", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(c, CH) else Notification.Builder(c)
        nm.notify(id, b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title)
            .setContentText(text).setContentIntent(open).setAutoCancel(true).build())
    }

    /** list = JSON array of {id, ts(ms), title, text}; replaces all previously scheduled alarms. */
    fun schedule(c: Context, json: String) {
        val sp = c.getSharedPreferences("n", Context.MODE_PRIVATE)
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val old = JSONArray(sp.getString("list", "[]"))
        for (i in 0 until old.length()) am.cancel(pi(c, old.getJSONObject(i)))
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("ts") > System.currentTimeMillis())
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, o.getLong("ts"), pi(c, o))
        }
        sp.edit().putString("list", json).apply()
    }

    private fun pi(c: Context, o: JSONObject) = PendingIntent.getBroadcast(
        c, o.getInt("id"),
        Intent(c, AlarmReceiver::class.java).putExtra("id", o.getInt("id"))
            .putExtra("t", o.getString("title")).putExtra("x", o.getString("text")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) =
        Notifier.notifyNow(c, i.getIntExtra("id", 0), i.getStringExtra("t") ?: "", i.getStringExtra("x") ?: "")
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val j = c.getSharedPreferences("n", Context.MODE_PRIVATE).getString("list", null) ?: return
        Notifier.schedule(c, j)
    }
}
