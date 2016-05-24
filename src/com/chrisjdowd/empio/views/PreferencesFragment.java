package com.chrisjdowd.empio.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.chrisjdowd.empio.R;

public class PreferencesFragment extends PreferenceFragment{
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		PreferenceScreen copyscreen = (PreferenceScreen) findPreference("pref_copymap");
		copyscreen.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference preference){
				String dir = "OUYA Mappings";
				String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
				sd+="/"+dir+"/";
				String ouyaMap = "OUYA.ini";
				
				File ouyaFile = new File(sd, ouyaMap);				
				if(ouyaFile.exists())ouyaFile.delete();
				try{
					AssetManager am = getActivity().getAssets();
					InputStream in = am.open("OUYA.ini");
					OutputStream out = new FileOutputStream(ouyaFile);
					int read;
					while((read=in.read())!=-1){
						out.write(read);
					}
					out.flush();
					out.close();
					in.close();		
					Toast.makeText(getActivity(), "Copied Ouya map.", Toast.LENGTH_SHORT).show();
				}
				catch(Exception e){
					e.printStackTrace();
					Toast.makeText(getActivity(), "Could not copy map.", Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});
		PreferenceScreen buyscreen = (PreferenceScreen) findPreference("pref_purchase");
		buyscreen.setEnabled(false);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==2){
		}
	}
}