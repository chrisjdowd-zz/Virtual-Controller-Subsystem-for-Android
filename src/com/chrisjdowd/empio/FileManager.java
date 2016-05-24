package com.chrisjdowd.empio;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileManager extends ListActivity{
	
	private String directory = Environment.getExternalStorageDirectory().getPath();
	private static File[] list;
	private static FolderAdapter adapter;
	private boolean refresh = false;
	
	
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
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				log(ex.getStackTrace()[0].toString());
			}
			
		});
		File sd = new File(directory);		
		list = sd.listFiles();
		adapter = new FolderAdapter(this, R.layout.filebrowser_layout, list);
		adapter.setNotifyOnChange(true);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int pos, long id){
		File o = (File)l.getItemAtPosition(pos);
		if(o.getName().contains(".apk") & o.isFile()){
			PackageInstaller pi = new PackageInstaller();
			pi.installPackage(o.getAbsolutePath(), this);
			refresh = true;
			pi = null;
		}
		else if(o.isDirectory()){
			String path = o.getAbsolutePath();
			o = new File(path);
			list = o.listFiles();
			if(list.length == 0){
				Toast.makeText(getApplicationContext(), "Empty folder", Toast.LENGTH_SHORT).show();
				path = o.getParentFile().getAbsolutePath();
				list = o.listFiles();
			}
			else{
				adapter = new FolderAdapter(this, R.layout.filebrowser_layout, list);
				setListAdapter(adapter);
				adapter.notifyDataSetChanged();
			}
			directory = path;
		}
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, KeyEvent event){
		boolean handled = false;
		switch(keyCode){
			case KeyEvent.KEYCODE_BACK:
			case 82://OuyaController.BUTTON_MENU:
				finish();
				handled = true;
				break;
			case 97://OuyaController.BUTTON_A:
				if(directory.equals(Environment.getExternalStorageDirectory().getAbsolutePath())){
					finish();
				}
				else{
					directory = directory.substring(0, directory.lastIndexOf("/"));
					list = new File(directory).listFiles();
					adapter = new FolderAdapter(this, R.layout.filebrowser_layout, list);
					setListAdapter(adapter); 
					adapter.notifyDataSetChanged();
				}
				handled = true;
				break;
		}
		
		return handled;
	}
	
	@Override
	public void finish(){
		if(refresh) setResult(1);
		else setResult(0);		
		super.finish();
	}
	
	private class FolderAdapter extends ArrayAdapter<File>{
		
		public FolderAdapter(Context context, int textViewResourceId, File[] objects) {
			super(context, textViewResourceId, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			if(convertView == null){
				convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.filebrowser_layout, null);
			}

			ImageView icon = (ImageView)convertView.findViewById(R.id.fileimage);
			TextView name = (TextView)convertView.findViewById(R.id.filetextname);
		
			File o = getItem(position);
	
			if(o.isDirectory()){
				//change to folder image
				icon.setImageResource(android.R.drawable.ic_delete);
			}
			else{
				if(o.getAbsolutePath().contains(".apk")){
					icon.setImageResource(android.R.drawable.ic_input_add);
				}
				else{
					icon.setImageResource(android.R.drawable.ic_delete);
				}
			}
			name.setText(o.getName());
			return convertView;
		}
	}
}