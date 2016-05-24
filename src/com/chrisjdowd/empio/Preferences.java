package com.chrisjdowd.empio;

import com.chrisjdowd.empio.views.PreferencesFragment;

import android.app.Activity;
import android.os.Bundle;

public class Preferences extends Activity {
	
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		getFragmentManager().beginTransaction()
    	.replace(android.R.id.content, new PreferencesFragment())
    	.commit();
	}
}
