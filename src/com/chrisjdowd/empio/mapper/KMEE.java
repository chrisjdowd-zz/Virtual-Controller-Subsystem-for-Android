package com.chrisjdowd.empio.mapper;

import java.util.ArrayList;
import java.util.Locale;

import android.util.Log;

import com.chrisjdowd.empio.Root;

public class KMEE {
	//
	// Key/MotionEvent Emulator
	//
	
	public static final String TAG = "KMEE";
	public static String DEVICE = android.os.Build.DEVICE;
	public static String conType = "";
	public static Device con;
	public static Device vCon;
	public static Device vTS;
	
	int EV_ABS = 0x03;
	int REL_X = 0x00;
	int REL_Y = 0x01;
	
	// JNI native code interface
	public native static void intEnableDebug(int enable);
	private native static int CreateMapDevice(int xres, int yres);
	private native static int GetExclusiveControl(int devid);
	private native static int ReleaseExclusiveControl(int devid);
	private native static int ScanFiles(); // return number of devs
	private native static int OpenDevice(int devid);
	private native static int CloseDevice(int devid);
	private native static String getDevicePath(int devid);
	private native static String getDeviceName(int devid);
	private native static int PollDevice(int devid);
	private native static int getType();
	private native static int getCode();
	private native static int getValue();
	
	// injector
	private native static int SendEvent(int devid, int type, int code, int value);
    static {
        System.loadLibrary("EventEmulator");
    }
    
    private ArrayList<Device> devs = new ArrayList<Device>();
    
    public KMEE(int width, int height){
    	intEnableDebug(0);
    	int created = CreateMapDevice(width, height);
		if(created != 0 && Root.isRooted()){
			Root.runAsRoot(new String[] {"chmod 666 /dev/uinput"});
			CreateMapDevice(width, height);
		}
    	int numDevices = ScanFiles();
    	for(int i=0; i<numDevices; i++){
    		devs.add(new Device(i, getDevicePath(i)));
    	}
    }
        
    public void listDevices(){
    	for(Device d : devs){
    		if(!d.getOpen()) d.openDevice();
    		Log.d("dev name",d.getName());
    	}
    }
    public boolean getController(){
    	for(Device d : devs){
    		if(!d.getOpen())d.openDevice();
    		Log.d("dev",d.getName());
    		if(DEVICE.toLowerCase(Locale.US).contains("ouya")){
	    		if(d.getName().equals("OUYA Game Controller")){
	    			con = d;
	    			conType = "ouya";
	    			return true;
	    		}
//	    		else if(d.getName().contains("X-Box")){
//	    			con = d;
//	    			conType = "360";
//	    		}
	    		else if(d.getName().contains("PLAYSTATION")){
	    			con = d;
	    			conType = "ps3";
	    			return true;
	    		}
    		}
    		//PHONE
    		if(!android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya"))
    		if(d.getName().toLowerCase(Locale.US).contains("bluetooth hid")){
    			Log.d("GETCONTROLLER", "Found Generic BT Gamepad");
    			con = d;
    			conType = "ouya";
    			return true;
    		}
    	}
//    	int num = ScanFiles();
//    	if(num > devs.size()){
//    		devs.clear();
//    		for(int i=0;i<num;i++){
//    			devs.add(new Device(i, getDevicePath(i)));
//    		}
//    	}
//    	return getController();
    	return false;
    }
    
    public boolean getTS(){
        if(android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
	    	for(Device d: devs){
	    		if(!d.getOpen())d.openDevice();
	    		if(d.getName().equals("MAPPER TS")){
	    			vTS = d;
	    			return true;
	    		}
	    	}
        }
        else{
        	for(Device d: devs){
        		if(!d.getOpen()) d.openDevice();
        		String name = d.getName().toLowerCase(Locale.US);
        		if(name.contains("ts") || name.contains("touchscreen") || name.contains("screen")){
        			vTS = d;
        			return true;
        		}
        	}
        }
//    	int num = ScanFiles();
//    	if(num > devs.size()){
//    		devs.clear();
//    		for(int i=0; i<num; i++){
//    			devs.add(new Device(i, getDevicePath(i)));
//    		}
//    	}
//    	return getTS();
    	return false;
    }
    public boolean getMapper(){
    	for(Device d : devs){
    		if(!d.getOpen())d.openDevice();
    		if(d.getName().equals("MAPPER GAMEPAD")){
    			vCon = d;
    			return true;
    		}
    	}
//    	int num = ScanFiles();
//    	if(num > devs.size()){
//    		devs.clear();
//    		for(int i=0; i<num; i++){
//    			devs.add(new Device(i, getDevicePath(i)));
//    		}
//    	}
//    	return getMapper();
    	return false;
    }
    
    public class Device{
    	
    	private int _id;
    	private String _path, _name;
    	private boolean _open;
    	
    	public Device(int id, String path){
    		_id = id;
    		_path = path;
    	}
    	
    	public int pollDevice(){
    		return PollDevice(_id);
    	}
    	
    	public boolean getOpen(){
    		return _open;
    	}
    	
    	public int getPollingType(){
    		return getType();
    	}
    	
    	public int getPollingCode(){
    		return getCode();
    	}
    	
    	public int getPollingValue(){
    		return getValue();
    	}
    	
    	public int getId(){
    		return _id;
    	}
    	
    	public String getPath(){
    		return _path;
    	}
    	
    	public String getName(){
    		return _name;
    	}
   
    	public int getExclusive(){
    		int ret = GetExclusiveControl(_id);
    		return ret;
    	}
    	
    	public int releaseExclusive() {
			return ReleaseExclusiveControl(_id);
		}   
    	
    	public boolean openDevice(){
    		int opened = OpenDevice(_id);
    		if(opened != 0 && Root.isRooted()){
    			//try setting new permissions
    			Root.runAsRoot(new String[] {"chmod 666 "+_path});
    			//try to reopen
    			opened = OpenDevice(_id);
    		}
    		_name = getDeviceName(_id);
    		_open = (opened == 0);
    		return _open;
    	}
    	
    	public void closeDevice(){
    		_open = false;
    		ReleaseExclusiveControl(_id);
    		CloseDevice(_id);
    	}
    	
    	public int sendKeyEvent(int code, int value){
    		int res = SendEvent(_id, 0x01, code, value);
    		sendSyn();
    		return res;
    	}
    	
    	public void sendAnalogEvent(int code, int value){
    		SendEvent(_id, EV_ABS, code, value);
    		sendSyn();
    	}
    	public void sendTouchEvent(int x, int y){
    		SendEvent(_id, EV_ABS, REL_X, x); //set x coord
			SendEvent(_id, EV_ABS, REL_Y, y); //set y coord
			SendEvent(_id, EV_ABS, 24,100); //100 pressure
			SendEvent(_id, EV_ABS, 28,1); //tool width of 1
			SendEvent(_id, 1, 330, 1); // touch down
			SendEvent(_id, EV_ABS, 53,x);
			SendEvent(_id, EV_ABS, 54,y);
			SendEvent(_id, EV_ABS, 48,100);
			SendEvent(_id, EV_ABS, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
			SendEvent(_id, EV_ABS, 24,0); //0 pressure
			SendEvent(_id, EV_ABS, 28,0); //tool width of 0
			SendEvent(_id, 1, 330,0); //touch up
			SendEvent(_id, EV_ABS, 53,0);
			SendEvent(_id, EV_ABS, 54,0);
			SendEvent(_id, EV_ABS, 48,0);
			SendEvent(_id, EV_ABS, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
    	}
    	
    	public void sendTouchEventStart(int x, int y){
    		int EV_ABS = 0x03;
    		int REL_X = 0x00;
    		int REL_Y = 0x01;

    		SendEvent(_id, EV_ABS, REL_X, x); //set x coord
			SendEvent(_id, EV_ABS, REL_Y, y); //set y coord
			SendEvent(_id, EV_ABS, 24,100); //100 pressure
			SendEvent(_id, EV_ABS, 28,1); //tool width of 1
			SendEvent(_id, 1, 330, 1); // touch down
			SendEvent(_id, EV_ABS, 53,x);
			SendEvent(_id, EV_ABS, 54,y);
			SendEvent(_id, EV_ABS, 48,100);
			SendEvent(_id, EV_ABS, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
    	}
    	
    	public void sendTouchEventEnd(int x, int y){
    		int EV_ABS = 0x03;
    		SendEvent(_id, EV_ABS, 24,0); //0 pressure
			SendEvent(_id, EV_ABS, 28,0); //tool width of 0
			SendEvent(_id, 1, 330,0); //touch up
			SendEvent(_id, EV_ABS, 53,0);
			SendEvent(_id, EV_ABS, 54,0);
			SendEvent(_id, EV_ABS, 48,0);
			SendEvent(_id, EV_ABS, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
    	}
    	
    	public void beginDragEvent(int x, int y){
    		SendEvent(_id, 3, 0, x);//set x
    		SendEvent(_id, 3, 1, y);//set y
    		SendEvent(_id, 3, 24, 100);//pressure
    		SendEvent(_id, 3, 28, 1);
    		SendEvent(_id, 1, 330, 1);
    		sendDragEvent(x, y);
    	}
    	
    	public void sendDragEvent(int x, int y){
    		SendEvent(_id, 3, 0, x);
    		SendEvent(_id, 3, 1, y);
    		SendEvent(_id, 3, 53,x);
			SendEvent(_id, 3, 54,y);
			SendEvent(_id, 3, 48,100);
			SendEvent(_id, 3, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
    	}
    	
    	public void endDragEvent(){
    		SendEvent(_id, 3, 24,0); //0 pressure
			SendEvent(_id, 3, 28,0); //tool width of 0
			SendEvent(_id, 1, 330,0); //touch up
			SendEvent(_id, 3, 53,0);
			SendEvent(_id, 3, 54,0);
			SendEvent(_id, 3, 48,0);
			SendEvent(_id, 3, 50,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 2,0);
			SendEvent(_id, 0, 0,0);
    	}
    	
    	public void sendMouseEvent(int code, int value){
    		if(code == 0){
        		SendEvent(_id, 2, 0x00, value);
        		SendEvent(_id, 2, 0x01, 0);
    		}
    		else{
    			SendEvent(_id, 2, 0x01, value);
    			SendEvent(_id, 2, 0x00, 0);
    		}
    		sendSyn();
    	}
    	
    	private void sendSyn(){
    		SendEvent(_id, 0x00, 0x00, 0);
    	}
    }
}
