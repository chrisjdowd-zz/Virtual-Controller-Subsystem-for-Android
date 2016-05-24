package com.chrisjdowd.empio.mapper;

import com.chrisjdowd.empio.R;
import com.chrisjdowd.empio.R.color;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MapperService extends Service{
	
	private boolean registered = false;

	protected ControllerMap map = null;
	protected String loadedPackage = "OUYA";
	protected Intent gameIntent;
	private ControllerManager manager;
	private CyclePointThread cpthread;
	
	private boolean menuOpen = false;
	private boolean mapperOpen = false;
	private boolean statusOpen = false;
	private View menu;
	private ControllerMapper mapper;
	private TextView status;
	
	private boolean overlayCycle = false;
	private SharedPreferences prefs;
	
	WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			0,
			PixelFormat.TRANSLUCENT
			);
	WindowManager.LayoutParams mapperParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			0,
			PixelFormat.TRANSLUCENT
			);
	
	WindowManager.LayoutParams statusParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_TOAST,
			0,
			PixelFormat.TRANSLUCENT
			);
	
	@Override
	public void onCreate(){
		super.onCreate();
		menuParams.setTitle("ServiceMenu");
		mapperParams.setTitle("ServiceMapper");
		statusParams.setTitle("StatusOverlay");
		statusParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		overlayCycle = prefs.getBoolean("pref_overlaycp", true);
		if(overlayCycle){
			cpthread = new CyclePointThread(this);
		}
		
		IntentFilter i = new IntentFilter("com.chrisjdowd.empio.mapper.MAPPER_MENU");
		i.addAction("com.chrisjdowd.empio.mapper.MAPPER_OPEN");
		i.addAction("com.chrisjdowd.empio.mapper.MAPPER_QUIT");
		i.addAction("com.chrisjdowd.empio.mapper.MAPPER_NEW_MAP");
		i.addAction("com.chrisjdowd.empio.mapper.SHOW_STATUS");
		i.addAction("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
		i.addAction("com.chrisjdowd.empio.mapper.SHOW_POINT");
		i.addAction("com.chrisjdowd.empio.mapper.UPDATE_POINT");
		if(!registered){
			registerReceiver(menuListener, i);
			registered = true;
		}
		Toast.makeText(this, "CREATING Controller Mapper", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startid){
		if(manager==null){
			manager = new ControllerManager(this);
			manager.start();
		}
		Toast.makeText(this, "Starting Controller Mapper", Toast.LENGTH_SHORT).show();
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy(){
		if(menu!=null && menuOpen){
			((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(menu);
			menu = null;
		}
		if(mapper!=null && mapperOpen){
			((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mapper);
			mapper = null;
		}
		if(status!=null && statusOpen){
			((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(status);
			status = null;
		}
		if(registered){
			unregisterReceiver(menuListener);
			registered = false;
		}
		if(cpthread!=null)cpthread.shutdown();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}	
	
	
	private void showMenu(boolean show){
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		if(menu==null){
			LayoutInflater inflater = LayoutInflater.from(this);
			menu = inflater.inflate(R.layout.menu, null);
			menu.setOnKeyListener(menuKeyListener);
		}
		if(show && !menuOpen){
			wm.addView(menu, menuParams);
			menu.setBackgroundColor(color.black_overlay);
			menuOpen = true;
		}
		else if(!show && menuOpen){
			wm.removeView(menu);
			menuOpen = false;
		}
	}
	
	private void showMapper(boolean show){
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		if(menuOpen){
			wm.removeView(menu);
			menuOpen = false;
			menu = null;
		}
		if(mapper == null){
			mapper = new ControllerMapper(this);
			mapper.setOnKeyListener(mapperKeyListener);
		}
		if(show && !mapperOpen){
			wm.addView(mapper, mapperParams);
			mapper.setBackgroundColor(color.black_overlay);
			mapperOpen = true;
		}
		else if(!show && mapperOpen){
			wm.removeView(mapper);
			mapperOpen = false;
		}
	}
	
	private void showStatus(boolean show, String set, String cycle){
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		if(status == null){
			status = new TextView(this);
		}
		if(show && !statusOpen){
			String statusText = "Current set: "+set;
			if(cycle.length()!=0){
				statusText+=", Current cycle: "+cycle;
			}
			status.setText(statusText);
			statusParams.x = prefs.getInt("statusOverscanX", 0);
			statusParams.y = prefs.getInt("statusOverscanY", 0);
			wm.addView(status, statusParams);
			status.setBackgroundColor(color.black_overlay);
			statusOpen = true;
		}
		else if(!show && statusOpen){
			wm.removeView(status);
			statusOpen = false;
		}
	}
	
	private void updateStatus(String set, String cycle){
		String text = "Current set: "+set;
		if(cycle.length()!=0){
			text+=", Current cycle: "+cycle;
		}
		status.setText(text);
	}
	
	private BroadcastReceiver menuListener = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Bundle extras = intent.getExtras();
			if(action.equals("com.chrisjdowd.empio.mapper.MAPPER_MENU")){
				showMenu(extras.getBoolean("show"));
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.MAPPER_OPEN")){
				showMapper(extras.getBoolean("show"));
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.MAPPER_QUIT")){
				if(menuOpen){
					showMenu(false);
				}
				if(mapperOpen){
					showMapper(false);
				}
				if(statusOpen){
					showStatus(false, "","");
				}
				stopSelf();
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.MAPPER_NEW_MAP")){
				
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.SHOW_STATUS")){
				showStatus(extras.getBoolean("show"), extras.getString("set"), extras.getString("cycle"));
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.UPDATE_STATUS")){
				updateStatus(extras.getString("set"), extras.getString("cycle"));
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.SHOW_POINT")){
				
				if(extras.getBoolean("show") && overlayCycle){
					cpthread.showCyclePoint(extras.getBoolean("show"), extras.getIntArray("coords"));
					cpthread.run();
				}
			}
			else if(action.equals("com.chrisjdowd.empio.mapper.UPDATE_POINT")){
				if(overlayCycle){
					cpthread.updateCyclePoint(extras.getIntArray("coords"));
					cpthread.run();
				}
			}
		}
	};
	
	private OnKeyListener menuKeyListener = new OnKeyListener(){
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			return false;
		}
	};
	
	private OnKeyListener mapperKeyListener = new OnKeyListener(){
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event){
			return false;
		}
	};
}
