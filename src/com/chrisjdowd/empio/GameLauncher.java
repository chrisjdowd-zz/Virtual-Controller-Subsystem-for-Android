package com.chrisjdowd.empio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.chrisjdowd.empio.mapper.MapperService;
import com.chrisjdowd.empio.views.Application;
import com.chrisjdowd.empio.views.PurchaseDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameLauncher extends Activity {
		
	private static ArrayList<Application> mApplications;
	private ApplicationsAdapter adapter;
	private GridView grid;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main_activity);
        grid = (GridView)findViewById(R.id.appGrid);

		if(!android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
			LinearLayout root = (LinearLayout)findViewById(R.id.mainroot);
			root.removeView(findViewById(R.id.bottombar));
			
		}
        loadApplications();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				pDialog = new ProgressDialog(parent.getContext());
				pDialog.setMessage("Checking license...");
				pDialog.setTitle("EMPIO");
				pDialog.show();
				Thread t = new Thread(new Runnable(){
					public void run(){
						progressDialogHandler.sendEmptyMessage(0);
//						Log.d("thread","getting receipts");
//						OUYALicenseManager.getReceipt(progressDialogHandler);
//						Log.d("thread","after receipts");
					}
				});				
				Application app = (Application)parent.getItemAtPosition(pos);
				appIntent = app.launchIntent;
				appPackageName = app.packageName;
				Log.d("app",appPackageName);
				Log.d("app",appIntent.toString());
				t.start();
			}
        });
        setReceiver();
        grid.setSelection(0);
        serviceIntent = new Intent(this.getApplicationContext(), MapperService.class);
        
        boolean firstTime = prefs.getBoolean("introshown", false);
        if(!firstTime){		
        	startActivityForResult(new Intent(this, FirstTimeActivity.class),1);
        }
	}
	
	private void loadApplications(){
		if(adapter!=null)adapter.notifyDataSetInvalidated();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PackageManager pm = getPackageManager();
	    final List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
	    Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
	    if(apps!=null){
	    	mApplications = new ArrayList<Application>(apps.size());
	    	adapter = new ApplicationsAdapter(this, mApplications);
	    	adapter.clear();
	    	mApplications.clear();
	    	for(int i=0;i<apps.size();i++){
	    		Application app = new Application();
	    		ResolveInfo info = apps.get(i);
	    		app.title = info.loadLabel(pm);
	    		app.setActivity(new ComponentName(
	                    info.activityInfo.applicationInfo.packageName,
	                    info.activityInfo.name),
	                    Intent.FLAG_ACTIVITY_NEW_TASK
	                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
	    		app.setPackageName(info.activityInfo.applicationInfo.packageName);
	            app.icon = info.activityInfo.loadIcon(pm);
	            mApplications.add(app);
	    	}
	    }
	    adapter.notifyDataSetChanged();
	    grid.setSelection(0);
	}
	
	
	public void onBackPressed(){
		askFinish();
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, KeyEvent event){
		boolean handled = false;
		switch(keyCode){
			case 82://OuyaController.BUTTON_MENU:
				if(android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
					askFinish();
					handled = true;
				}
				break;
			case 99://OuyaController.BUTTON_U:
				Intent launchIntent = new Intent(this, Preferences.class);
				startActivity(launchIntent);
				handled = true;
				break;
			case 100://OuyaController.BUTTON_Y:
				launchIntent = new Intent(this, FileManager.class);
				startActivity(launchIntent);
				handled = true;
				break;
			case KeyEvent.KEYCODE_BACK:
			case 97://OuyaController.BUTTON_A:
				askFinish();
				handled = true;
				break;
		}
		return handled;
	}
	
	public void askFinish(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?")
		.setNegativeButton("No", finishDialogListener)
		.setPositiveButton("Yes", finishDialogListener)
		.show();
	}
	
	private void setReceiver(){
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addDataScheme("package");
		registerReceiver(packageInstalledReceiver, intentFilter);	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.launchermenu, menu);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		boolean handled = false;
		switch(item.getItemId()){
			case R.id.option_menu:
				Intent launchIntent = new Intent(this, Preferences.class);
				startActivity(launchIntent);
				handled = true;
				break;
			case R.id.install_menu:
				launchIntent = new Intent(this, FileManager.class);
				startActivity(launchIntent);
				handled = true;
				break;
		}
		return handled;
	}
	
	@Override
	public void onResume(){
		setReceiver();
		super.onResume();
	}
	
	@Override
	public void finish(){
		//stopService(new Intent(this, MapperService.class));
		//unregisterReceiver(packageInstalledReceiver);
		Editor e = prefs.edit();
		if(prefs.getBoolean("firsttime", true)){
			e.putBoolean("askedtobuy", false);
		}
		e.putBoolean("firsttime", false);
		e.commit();
		super.finish();
	}
	
	@Override
	public void onDestroy(){
		unregisterReceiver(packageInstalledReceiver);
		super.onDestroy();
	}
	
	DialogInterface.OnClickListener finishDialogListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	finish();
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            dialog.dismiss();
	            break;
	        }
	    }
	};
	
	BroadcastReceiver packageInstalledReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, "Package installed", Toast.LENGTH_SHORT).show();
			loadApplications();
		}
	};
	
	private class ApplicationsAdapter extends ArrayAdapter<Application> {
        private Rect mOldBounds = new Rect();

        public ApplicationsAdapter(Context context, ArrayList<Application> apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Application info = mApplications.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_layout, parent, false);
            }

            Drawable icon = info.icon;

            if (!info.filtered) {
//                final Resources resources = getContext().getResources();
                int width = 300;//(int)resources.getDimension(android.R.dimen.app_icon_size);
                int height= 300;//(int)resources.getDimension(android.R.dimen.app_icon_size);

                final int iconWidth = icon.getIntrinsicWidth();
                final int iconHeight = icon.getIntrinsicHeight();
                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                }

                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config c =
                        icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(width, height, c);
                final Canvas canvas = new Canvas(thumb);
                canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, 0));
                // Copy the old bounds to restore them later
                // If we were to do oldBounds = icon.getBounds(),
                // the call to setBounds() that follows would
                // change the same instance and we would lose the
                // old bounds
                mOldBounds.set(icon.getBounds());
                icon.setBounds(0, 0, width, height);
                icon.draw(canvas);
                icon.setBounds(mOldBounds);
                icon = info.icon = new BitmapDrawable(thumb);
                info.filtered = true;
                
            }
            final GridView grid = (GridView) parent;
            final TextView textView = (TextView) convertView.findViewById(R.id.app_layout_name);
            if(!android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
            	//other
            	int gridCol = 4;
            	int gridRow = 5;
            	grid.setNumColumns(gridCol);
                convertView.setLayoutParams(new AbsListView.LayoutParams(grid.getWidth()/gridCol, grid.getHeight()/gridRow));
            	LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)textView.getLayoutParams();
            	params.setMargins(0,0,0,0);
            	textView.setLayoutParams(params);
            	textView.setTextAppearance(getContext(), android.R.style.TextAppearance_Small);
            }
            else{
            	//ouya
                grid.setNumColumns(3);
                convertView.setLayoutParams(new AbsListView.LayoutParams(grid.getWidth()/3, grid.getHeight()/2));
            }
            textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
            textView.setText(info.title);
            return convertView;
        }
    }
	
	DialogInterface.OnClickListener rootDialogListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	root();
		        	dialog.dismiss();
		            break;
	
		        case DialogInterface.BUTTON_NEGATIVE:
		            dialog.dismiss();
		            finish();
		            break;
		        
		        case DialogInterface.BUTTON_NEUTRAL:
		        	dialog.dismiss();
		        	break;
		        }
	    }
	};
	
	private void showRootDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Your Ouya isn't rooted!");
		builder.setMessage("EMPIO needs root to continue. " +
				"Root is used to create the touchscreen and read controller input. " +
				"If you don't know how to root your OUYA, email empiohelp@gmail.com")
		.setNeutralButton("Okay", rootDialogListener)
		.show();
	}
	
	private static Intent serviceIntent;
	private static Intent appIntent;
	private static String appPackageName;
	
	private static ProgressDialog pDialog;
	private Handler progressDialogHandler = new Handler(){

		public void handleMessage(Message param){
			if(param.what==0){
				pDialog.dismiss();
				if(!checkPurchased()){
					createFiles(appPackageName);
					launch(appPackageName, serviceIntent, appIntent);
				}
			}
			else if(param.what==2){
				launch(appPackageName, serviceIntent, appIntent);
			}
			else if(param.what==3){
				//create alert saying it failed
			}
			else if(param.what==4){
				//completed purchase
				createFiles(appPackageName);
				launch(appPackageName, serviceIntent, appIntent);
			}
		}
	};
	
	private boolean checkPurchased(){
		return false;
	}
	
	private void launch(String packagename, Intent service, Intent app){
		if(!Root.isRooted()){
			showRootDialog();
		}
		else{	
			startService(service);
			startActivity(app);
		}
	}
	
	private void createFiles(String packagename){
		
		try{
			String dir = "OUYA Mappings";
			String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
			sd+="/"+dir+"/";
			String map = packagename+".ini";
			String ouyaMap = "OUYA.ini";
			
			File mapDir = new File(sd);
			File mapFile = new File(sd, map);
			File ouyaFile = new File(sd, ouyaMap);
			
			if(!mapDir.exists()){
				boolean b = mapDir.mkdirs();
				if(!b)return;
			}
			
			if(!ouyaFile.exists()){
				AssetManager am = getAssets();
				InputStream in = am.open("OUYA.ini");
				OutputStream out = new FileOutputStream(ouyaFile);
				int read;
				while((read=in.read())!=-1){
					out.write(read);
				}
				out.flush();
				out.close();
				in.close();							
			}
			
			boolean autocreate = prefs.getBoolean("pref_createfiles", true);
			if(!mapFile.exists() && autocreate){
				mapFile.createNewFile();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private Handler dialogHandler = new Handler(){
		public void handleMessage(Message m){
			Editor e = prefs.edit();
			e.putBoolean("askedtobuy", true);
			e.commit();

			if(m.what==1){
				
			}
			else if(m.what==0){
				progressDialogHandler.sendEmptyMessage(4);
			}
		}
	};
	
	private void purchase(Intent i){
		startActivityForResult(i, 2);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==2){
		}
	}
	
	private void root(){
		//if(Root.isRooted())return;
//		Log.d("root","showing spinner");
//		pDialog = new ProgressDialog(this);
//		pDialog.setMessage("Rooting console...");
//		pDialog.setTitle("EMPIO");
//		pDialog.show();
//		try{
//			File apk = new File(Environment.getExternalStorageDirectory(), "su");
//			Log.d("root",apk.getAbsolutePath());
//			if(!apk.exists()){
//				AssetManager am = getAssets();
//				Log.d("root","opening apk");
//				InputStream in = am.open("su");
//				Log.d("root","creating file");
//				OutputStream out = new FileOutputStream(apk);
//				byte[] buffer = new byte[1024];
//				int read;
//				while((read=in.read(buffer))!=-1){
//					out.write(buffer, 0, read);
//				}
//				out.flush();
//				out.close();
//				out = null;
//				in.close();
//				in = null;
//				Log.d("root","finished writing su");
//			}
//			Log.d("root","calling root console");
//			Thread t = new Thread(new Runnable(){
//				public void run(){
//					Root.rootConsole(progressDialogHandler);
//				}
//			});
//			t.start();
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
	}
}