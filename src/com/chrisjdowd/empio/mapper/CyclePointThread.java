package com.chrisjdowd.empio.mapper;

import com.chrisjdowd.empio.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
//import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class CyclePointThread extends Thread {
	
	private boolean running = true;
	private WindowManager wm;
	private int timeout = -1;
	private ImageView point;
	private boolean shown = false;
	private SharedPreferences prefs;
	private long prevTime = 0;
	
	WindowManager.LayoutParams pointParams = new WindowManager.LayoutParams(
			50,//WindowManager.LayoutParams.WRAP_CONTENT,
			50,//WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_TOAST,
			0,
			PixelFormat.TRANSLUCENT
			);
	
	public CyclePointThread(Context c){
		wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
		point = new ImageView(c);
		point.setImageResource(R.drawable.ouya_o);
		point.setScaleType(ScaleType.CENTER_INSIDE);
		point.setVisibility(View.INVISIBLE);
		pointParams.gravity = Gravity.TOP | Gravity.LEFT;
		prefs = (SharedPreferences)PreferenceManager.getDefaultSharedPreferences(c);
		timeout = Integer.parseInt(prefs.getString("pref_cycletimeout", "1000").replaceAll("\\D+", ""));
	}
	
	@Override
	public void run(){
//		Log.d("POINT", "called run");
//		
//			if(shown){
//				Log.d("POINT","shown");
//				long time = System.currentTimeMillis();
//				//fix this
//				if(time-prevTime>timeout){
//					Log.d("thread", "removing point");
//					this.removePoint();
//					prevTime = time;
//				}
//			}
//		removePoint();
	}

	public void shutdown(){
		running = false;
		if(shown)wm.removeView(point);
		shown = false;
	}
	
	public void showCyclePoint(boolean show, int[] margins){
		if(show && !shown){
			if(margins[0] == -1 || margins[1] == -1){
				point.setVisibility(View.INVISIBLE);
			}
			else{
				point.setVisibility(View.VISIBLE);
			}
			pointParams.x = margins[0]-(point.getWidth()/2);
			pointParams.y = margins[1]-(point.getHeight()/2);
			wm.addView(point, pointParams);
			shown = true;
		}
		else if(!show && shown){
			removePoint();
		}
		else updateCyclePoint(margins);
	}
	
	public void updateCyclePoint(int[] margins){
		if(margins[0] == -1 || margins[1] == -1){
			point.setVisibility(View.INVISIBLE);
		}
		else{
			point.setVisibility(View.VISIBLE);
		}
		pointParams.x = margins[0]-(point.getWidth()/2);
		pointParams.y = margins[1]-(point.getHeight()/2);
		wm.updateViewLayout(point, pointParams);
	}
	
	private void removePoint(){
		if(shown){
			shown = false;
			point.setVisibility(View.INVISIBLE);
			wm.removeView(point);
		}
	}
}
