package com.chrisjdowd.empio;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Environment;
import android.os.Handler;

public class Root {

	public static final String TAG = "Root";
	private static String rootShell;
	private static String cShell;
	private static String userShell = "/system/bin/sh";
	
	//uid pattern when testing "id"
	private static final Pattern UID_PATTERN = Pattern.compile("^uid=(\\d+).*?");
	private static final String EXIT = "exit\n";
	enum STD {
		STDOUT,
		STDERR,
		BOTH
	}
	
	
//	//TEST AND GET CORRECT THEN REMOVE
//	private static final String[] SU_COMMANDS = new String[]{
//		"su",
//		"/system/xbin/su",
//		"/system/bin/su"
//	};
//	
//	private static final String[] TEST_COMMANDS = new String[]{
//		"id",
//		"/system/xbin/id",
//		"/system/bin/id"
//	};
	  
	  
	public static synchronized boolean isRooted(){
		if(rootShell == null){
			checkRoot();
		}
		return (rootShell != null);
	}
	
	private static boolean checkRoot(){
		cShell = "su";
		if(isRoot()){
			rootShell = cShell;
			return true;
		}
		cShell = rootShell = null;
		return false;
	}
	
	private static boolean isRoot(){
		String pOut = getProcessOutput("id");
		if(pOut==null || pOut.length()==0)return false;
		Matcher m = UID_PATTERN.matcher(pOut);
		if(m.matches()){
			if("0".equals(m.group(1))){
				return true;
			}
		}
		return false;
	}
	
	public static String getProcessOutput(String command){
		try{
			return _run(new String[] {command}, STD.STDERR);
		}
		catch(IOException e){
			return null;
		}
	}
	
	public static boolean runAsRoot(String[] cmd) {
		try{
			setShell(rootShell);
			_run(cmd, STD.BOTH);
			return true;
		}
		catch(IOException e){
			return false;
		}
	}
	
	public static boolean runAsUser(String[] arrayOfString1){
		try{
			setShell(userShell);
			_run(arrayOfString1, STD.STDERR);
			setShell(rootShell);
			return true;
		}
		catch(IOException e){
			setShell(rootShell);
			return false;
		}
	}
	
	private static synchronized void setShell(String sh){
		Root.cShell = sh;
	}
	
	private static Process shell;
	
	private static String _run(String[] command, STD o) throws IOException{
		DataOutputStream outputStream = null;
		try{
			shell = Runtime.getRuntime().exec(cShell);
			outputStream = new DataOutputStream(shell.getOutputStream());
			PISHandler handler = sinkProcessOutput(shell, o);
			
			for(int i=0;i<command.length;i++){
				outputStream.writeBytes(command[i]+'\n');
			}
			outputStream.writeBytes(EXIT);
			outputStream.flush();
			shell.waitFor();
			if(handler != null){
				String output = handler.getOutput();
				return output;
			}
			else{
				return null;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		finally{
			try{
				if(outputStream != null){
					outputStream.close();
				}
				if(shell != null){
					shell.destroy();
				}
			}
			catch(Exception e){}
		}
	}
	
	public static PISHandler sinkProcessOutput(Process p, STD o){
		PISHandler output = null;
		switch(o){
		case STDOUT:
			output = new PISHandler(p.getErrorStream(), false);
			new PISHandler(p.getInputStream(), true);
			break;
		case STDERR:
			output = new PISHandler(p.getInputStream(), false);
			new PISHandler(p.getErrorStream(), true);
			break;
		case BOTH:
			new PISHandler(p.getInputStream(), true);
			new PISHandler(p.getErrorStream(), true);
			break;
		}
		return output;
	}
	
	private static class PISHandler extends Thread{
		private final InputStream stream;
		private final boolean sink;
		StringBuffer out;
		
		PISHandler(InputStream s, boolean sink){
			this.stream = s;
			this.sink = sink;
			start();
		}
		
		public String getOutput(){
			return out.toString();
		}
		
		@Override
		public void run(){
			try{
				if(sink){
					String i;
					BufferedReader br = new BufferedReader(new InputStreamReader(stream));
					while((i=br.readLine()) != null){}
				}
				else{
					out = new StringBuffer();
					BufferedReader br = new BufferedReader(new InputStreamReader(stream));
					String s;
					while((s=br.readLine()) != null){
						if(s.toLowerCase(Locale.US).contains("operation not permitted")
								|| s.toLowerCase(Locale.US).contains("permission denied")){	
							continue;
						}
						out.append(s);
					}
				}
			}
			catch(IOException e){}
		}
	}
	
	public static boolean rootConsole(Handler handler) {
		String[] arrayOfString1 = new String[13];
		arrayOfString1[0] = "setprop service.adb.tcp.port 5555";
		arrayOfString1[1] = "stop adbd";
		arrayOfString1[2] = "start adbd";
		arrayOfString1[3] = "adb kill-server";
		arrayOfString1[4] = "adb devices";
		arrayOfString1[5] = "adb shell";
		arrayOfString1[6] = "su";
		arrayOfString1[7] = "mount -orw,remount /system";
		arrayOfString1[8] = ("cat " + new File(Environment.getExternalStorageDirectory(), "su").getAbsolutePath() + " > /system/xbin/su");
		arrayOfString1[9] = "chown 0.0 /system/xbin/su";
		arrayOfString1[10] = "chmod 6755 /system/xbin/su";
		arrayOfString1[11] = "mount -oro,remount /system";
		arrayOfString1[12] = "exit";
//		arrayOfString1[13] = "exit";
		try{
//			Process p = Runtime.getRuntime().exec("/system/bin/sh");
//			DataOutputStream outputstream = new DataOutputStream(p.getOutputStream());
//			for(int i=0; i<arrayOfString1.length;i++){
//				outputstream.writeBytes(arrayOfString1[i]);
//			}
//			outputstream.flush();
//			outputstream.close();
//			p.waitFor();
			runAsUser(arrayOfString1);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return isRooted();
	}
}
