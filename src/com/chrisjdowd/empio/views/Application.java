package com.chrisjdowd.empio.views;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

public class Application {

	public CharSequence title;
	public Intent launchIntent;
	public Drawable icon;
	public boolean filtered;
	public String packageName;
	
	public final void setPackageName(String pname){
		this.packageName = pname;
	}
	
	public final void setActivity(ComponentName className, int launchFlags) {
        launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(className);
        launchIntent.setFlags(launchFlags);
    }
}
