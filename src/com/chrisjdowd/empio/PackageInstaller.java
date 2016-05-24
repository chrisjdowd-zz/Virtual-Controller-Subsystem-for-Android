package com.chrisjdowd.empio;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

public class PackageInstaller {

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

	public void installPackage(String packagePath, Context ctx){
		Intent intent = new Intent(Intent.ACTION_VIEW);
	    intent.setDataAndType(
	    		Uri.fromFile(new File(packagePath)), "application/vnd.android.package-archive");
	    ctx.startActivity(intent);
	    log("Installed package "+packagePath);
	}
}
