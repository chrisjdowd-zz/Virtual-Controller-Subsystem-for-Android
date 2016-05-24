package com.chrisjdowd.empio;

import java.io.File;
import java.io.FileOutputStream;

import com.chrisjdowd.empio.R.color;
import com.chrisjdowd.empio.mapper.KMEE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

public class OverscanActivity extends Activity {
	
	private WindowManager wm;
	private TextView statusBox;
	WindowManager.LayoutParams statusParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_TOAST,
			0,
			PixelFormat.TRANSLUCENT
			);
	SharedPreferences prefs;

	private final String TAG = this.getClass().toString().substring(0, this.getClass().toString().indexOf("."));
	private void log(String log){
		try{
			//if alert
			String l = TAG+" - "+log;
			File logFile = new File(Environment.getExternalStorageDirectory()+"/OUYA Mappings/", "empio_log.txt");
			FileOutputStream fos = new FileOutputStream(logFile);
			fos.write(l.getBytes());
			fos.flush();
			fos.close();
		}
		catch(Exception e){
			
		}
	}
	
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		setContentView(R.layout.overscanlayout);
		wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		statusParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
		if(statusBox==null){
			statusBox = new TextView(this);
			statusBox.setText("Current Set: Overscan, Current Cycle, Overscan");
			statusBox.setBackgroundColor(color.black_overlay);
			statusParams.x = prefs.getInt("statusOverscanX", 0);
			statusParams.y = prefs.getInt("statusOverscanY", 0);
			
		}
		wm.addView(statusBox, statusParams);
		log("Started overscan activity");
		KMEE kmee = new KMEE(1920, 1080);
		kmee.listDevices();
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event){
		switch(keycode){
		case 19:
			statusParams.y+=1;
			wm.updateViewLayout(statusBox, statusParams);
			break;
		case 20:
			statusParams.y-=1;
			wm.updateViewLayout(statusBox, statusParams);
			break;
		case 21:
			statusParams.x-=1;
			wm.updateViewLayout(statusBox, statusParams);
			break;
		case 22:
			statusParams.x+=1;
			wm.updateViewLayout(statusBox, statusParams);
			break;
		case 97:
		case 4:
			log("Exiting overscan activity");
			finish();
			break;
		case 96:
		case 23:
		case 82:
			wm.removeView(statusBox);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt("statusOverscanX", statusParams.x);
			editor.putInt("statusOverscanY", statusParams.y);
			editor.commit();
			log("Saved overscan values to "+statusParams.x+","+statusParams.y);
			finish();
		}
		if(statusParams.x < 0) statusParams.x = 0;
		if(statusParams.y < 0) statusParams.y = 0;
		return true;
	}
}
