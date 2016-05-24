package com.chrisjdowd.empio.mapper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.Log;
//import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.WindowManager;
import com.chrisjdowd.empio.mapper.KMEE.Device;

public class ControllerManager extends Thread{
	
	boolean debug = false;
	
	private KMEE emu;
	private Device con;
	private Device vCon;
	private Device vTS;
	private boolean exclusive = false;
	private boolean mapperLoaded = false;
	private boolean menuLoaded = false;
	protected ControllerMap gameMap, ouyaMap;
	private Context service;
	
	private SparseBooleanArray analogStart = new SparseBooleanArray();
	private double ANALOGMAX;
	private SparseIntArray currentAnalogState = new SparseIntArray();
		
	private boolean portrait = false; 
	
	public ControllerManager(Context c){
		service = c;
		this.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				if(con!=null){
					con.releaseExclusive();
					con.closeDevice();
					vTS.closeDevice();
					vCon.closeDevice();
					Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, ex);
				}
			}
		});
		portrait = (service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? true : false;
		Log.d("portrait",portrait+"");
		Display d = ((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point size = new Point();
		d.getSize(size);
		
		//use sizex sizey and make sure it's in correct portrait/landscape then everything will work. 
		//just can't touch system bar which is fine because of keys
		
		
		
		//CHANGE THIS IN RELOAD IF NEEDED
		if(portrait)emu = new KMEE(1080,1920);
		else emu = new KMEE(1920, 1080);
		
		
		
		
		ActivityManager am = (ActivityManager) service.getSystemService(Activity.ACTIVITY_SERVICE);
        String appName = am.getRunningTasks(1).get(0).topActivity.getPackageName();
        am = null;
        
		boolean gotcon = emu.getController();
		boolean gotvcon = emu.getMapper();
		boolean gotts = emu.getTS();
		if(!gotcon || !gotvcon || !gotts){
			return;
		}
		
		con = KMEE.con;
		vCon = KMEE.vCon;
		vTS = KMEE.vTS;
		int e = con.getExclusive();
		exclusive = (e==0) ? true : false;

		if(KMEE.conType.equals("ps3")){
			ANALOGMAX = 255.0;
		}
		else if(KMEE.conType.equals("ouya")){
			ANALOGMAX = 65535.0;
		}
		
		for(int i=0;i<10;i++){
			currentAnalogState.put(i, 0);
		}
		
		gameMap = new ControllerMap(KMEE.conType, appName);
		ouyaMap = new ControllerMap(KMEE.conType, "OUYA");
		if(!gameMap.isLoaded()){
			gameMap = ouyaMap;
		}
	}
	
	public static boolean paused = false;
	boolean land = true;
	
	@Override
	public void run(){
		boolean running = true;
		paused = false;
		
		Intent statusintent = new Intent("com.chrisjdowd.empio.mapper.SHOW_STATUS");
		statusintent.putExtra("show", true);
		statusintent.putExtra("set", gameMap.getCurrentSet());
		statusintent.putExtra("cycle", gameMap.getCurrentCycle());
		service.sendBroadcast(statusintent);
		Intent pointintent = new Intent("com.chrisjdowd.empio.mapper.SHOW_POINT");
		pointintent.putExtra("show", true);
		pointintent.putExtra("coords", gameMap.getCyclePos());
		if(gameMap.containsCycles())service.sendBroadcast(pointintent);
		while(running){
			if(con == null){
				running = false;
				break;
			}
			while(con!=null && con.pollDevice()==0 && !paused){
				/*
				 * if game map == ouya map
				 * nothing loaded, just passthrough as regular values
				 * if gamemap==ouyamap
				 * 		same mapping code here
				 * 		if not in mapping
				 * 		send code type value passthrough
				 * 		vCon
				 * end result is a controller that mimics standard functionality when there's no map
				 * 
				 */
				int code = con.getPollingCode();
				int type = con.getPollingType();
				int value = con.getPollingValue();
								
				//fuck random noise
				if(type==0&&code==0&&value==0){
					continue;
				}			
				
				//testing ps3 purposes
				if(KMEE.conType.equals("ps3") && type==3 && (code>=50 && code<=63)) continue;
				
				//fix deadzone registering dz=5
				int center = (int) (ANALOGMAX/2);
				if(type==3 && value>center-5 && value<center+5) continue;
			
				if(debug)Log.d("ControllerManager",
						"RECV: Type: "+type
						+ " Code: "+code
						+ " Value: "+value);			
				
				if(code == gameMap.getInputCode("menulp")){
					//menu held down. lets you map menu too
					if(value==1){
						pointintent = new Intent("com.chrisjdowd.empio.mapper.SHOW_POINT");
						if(mapperLoaded){
							//close the mapper if it's open
							mapperLoaded = !mapperLoaded;
							Intent mapperintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_OPEN");
							mapperintent.putExtra("show", false);
							service.sendBroadcast(mapperintent);
							pointintent.putExtra("show", true);
						}
						else if(menuLoaded){
							menuLoaded = !menuLoaded;
							Intent menuintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_MENU");
							menuintent.putExtra("show", false);
							service.sendBroadcast(menuintent);
							pointintent.putExtra("show", true);
						}
						else{
							menuLoaded = !menuLoaded;
							Intent menuintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_MENU");
							menuintent.putExtra("show", true);
							service.sendBroadcast(menuintent);
							pointintent.putExtra("show", false);
						}
						pointintent.putExtra("coords", gameMap.getCyclePos());
						service.sendBroadcast(pointintent);
					}
					continue;
				}
				
				if(menuLoaded){
					if(value==1){
						if(code==gameMap.getInputCode("o")||code==gameMap.getInputCode("menulp")){
							menuLoaded = !menuLoaded;
							Intent menuintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_MENU");
							menuintent.putExtra("show", false);
							service.sendBroadcast(menuintent);							
							pointintent = new Intent("com.chrisjdowd.empio.mapper.SHOW_POINT");
							pointintent.putExtra("show", true);
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
						}
//						else if(code==gameMap.getInputCode("u")){
//							menuLoaded = !menuLoaded;
//							service.sendBroadcast(new Intent("com.chrisjdowd.empio.mapper.MAPPER_MENU"));
//							mapperLoaded = !mapperLoaded;
//							service.sendBroadcast(new Intent("com.chrisjdowd.empio.mapper.MAPPER_OPEN"));
//						}
						else if(code==gameMap.getInputCode("y")){
							ActivityManager am = (ActivityManager) service.getSystemService(Activity.ACTIVITY_SERVICE);
					        String appName = am.getRunningTasks(1).get(0).topActivity.getPackageName();
					        am = null;
							portrait = (service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? true : false;
							gameMap.reload(appName);

							if(!gameMap.isLoaded()){
								gameMap = ouyaMap;
							}
							//need to add true/false in intent
							//also consolidate everything into single event for each
							pointintent = new Intent("com.chrisjdowd.empio.mapper.SHOW_POINT");
							pointintent.putExtra("show", true);
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
							statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
							statusintent.putExtra("set", gameMap.getCurrentSet());
							statusintent.putExtra("cycle", gameMap.getCurrentCycle());
							service.sendBroadcast(statusintent);
							Intent menuintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_MENU");
							menuintent.putExtra("show", false);
							service.sendBroadcast(menuintent);				
							menuLoaded = !menuLoaded;
						}
						else if(code==gameMap.getInputCode("a")){
							int e = con.releaseExclusive();
							exclusive = e==0 ? true : false;
							running = false;
							paused = true;
						}
						else if(code==gameMap.getInputCode("l1")){
							gameMap.runCallback(308, ControllerMap.CB_SETNEXT);
							statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
							statusintent.putExtra("set", gameMap.getCurrentSet());
							statusintent.putExtra("cycle", gameMap.getCurrentCycle());
							service.sendBroadcast(statusintent);
							pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
						}
						else if(code==gameMap.getInputCode("r1")){
							gameMap.runCallback(309, ControllerMap.CB_CYCLENEXT);
							statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
							statusintent.putExtra("set", gameMap.getCurrentSet());
							statusintent.putExtra("cycle", gameMap.getCurrentCycle());
							service.sendBroadcast(statusintent);
							pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
						}
						else if(code==gameMap.getInputCode("l2")){
							gameMap.runCallback(316, ControllerMap.CB_SETPREV);
							statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
							statusintent.putExtra("set", gameMap.getCurrentSet());
							statusintent.putExtra("cycle", gameMap.getCurrentCycle());
							service.sendBroadcast(statusintent);
							pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
						}
						else if(code==gameMap.getInputCode("r2")){
							gameMap.runCallback(316, ControllerMap.CB_CYCLEPREV);
							statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
							statusintent.putExtra("set", gameMap.getCurrentSet());
							statusintent.putExtra("cycle", gameMap.getCurrentCycle());
							service.sendBroadcast(statusintent);
							pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
							pointintent.putExtra("coords", gameMap.getCyclePos());
							service.sendBroadcast(pointintent);
						}
					}
					continue;
				}
				else if(mapperLoaded){
					if(code==gameMap.getInputCode("menulp") && value==0){
						mapperLoaded = !mapperLoaded;
						Intent mapperintent = new Intent("com.chrisjdowd.empio.mapper.MAPPER_OPEN");
						mapperintent.putExtra("show", false);
						service.sendBroadcast(mapperintent);									
					}
					
					//choppy
					else if(type==3 && (code==gameMap.getInputCode("analogleftx")||code==gameMap.getInputCode("analoglefty"))){
						int analogToPx = (int) (ANALOGMAX/(2*2));
						int normalizedValue = (int)(value/analogToPx-2);
						currentAnalogState.put(code, normalizedValue);
						int[] leftanalog = gameMap.getAnalogValues(gameMap.getInputCode("leftanalog"));
						
						vCon.sendMouseEvent(leftanalog[0], currentAnalogState.get(leftanalog[0]));
						vCon.sendMouseEvent(leftanalog[1], currentAnalogState.get(leftanalog[1]));
						
						while((currentAnalogState.get(leftanalog[0]) > 0 || 
								currentAnalogState.get(leftanalog[1]) > 0)){
							con.pollDevice();
							type = con.getPollingType();
							code = con.getPollingCode();
							value = con.getPollingValue();
							if(type!=3)continue;
							normalizedValue = (int)(value/analogToPx-2);
							currentAnalogState.put(code, normalizedValue);
							vCon.sendMouseEvent(leftanalog[0], currentAnalogState.get(leftanalog[0]));
							vCon.sendMouseEvent(leftanalog[1], currentAnalogState.get(leftanalog[1]));
						}
					}
					continue;
				}
				
//				menu and mapper arent loaded, just send to the game
				else if(gameMap.contains(code)){
					//great, mapping exists. now do whatever needs to be done
					int[][] mapping = gameMap.getKeyMapping(code);
					if(mapping[0][0]!=0){
						//has regular event
						switch(mapping[0][0]){
							case ControllerMap.MAP_KEY:
								if(type==1){
									vCon.sendKeyEvent(mapping[0][1], value);
								}
								break;
							case ControllerMap.MAP_TOUCH:
								if(type==1){
									//single press
									if(value==1){
										vTS.sendTouchEvent(mapping[0][1], mapping[0][2]);
									}
									//individual presses:
//									if(value==1){
//										vTS.sendTouchEventStart(mapping[0][1], mapping[0][2]);
//									}
//									else{
//										vTS.sendTouchEventEnd(mapping[0][1], mapping[0][2]);
//									}
								}
								break;
							case ControllerMap.MAP_ANALOG:
								if(type==3){
									currentAnalogState.put(code, value);
									int[] analogs = gameMap.getAnalogValues(code);
									int[] mapanalogs = gameMap.getAnalogValues(mapping[0][1]);
									for(int i=0;i<analogs.length;i++){
										int j=currentAnalogState.get(analogs[i]);
										vCon.sendAnalogEvent(mapanalogs[i],j);
									}
								}
								break;
							case ControllerMap.MAP_ANALOGCYCLE:
								if(type==3){
									int threshold = mapping[0][1]==0 ? mapping[0][1] : (int)((mapping[0][1]/100.00)*ANALOGMAX);
									int[] analogs = gameMap.getAnalogValues(code);
									if(code==analogs[0]){
										if(value>ANALOGMAX/2 && value >= ANALOGMAX-threshold){
											gameMap.runCallback(code, ControllerMap.CB_CYCLERIGHT);
										}
										else if(value<ANALOGMAX/2 && value <= 0+threshold){
											gameMap.runCallback(code, ControllerMap.CB_CYCLELEFT);
										}
									}
									else if(code==analogs[1]){
										if(value>ANALOGMAX/2 && value >= ANALOGMAX-threshold){
											gameMap.runCallback(code, ControllerMap.CB_CYCLEDOWN);
										}
										else if(value<ANALOGMAX/2 && value <= 0+threshold){
											gameMap.runCallback(code, ControllerMap.CB_CYCLEUP);
										}
									}
									pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
									pointintent.putExtra("coords", gameMap.getCyclePos());
									service.sendBroadcast(pointintent);
								}
								break;
							case ControllerMap.MAP_ANALOGDPADANALOG:
								if(type==3){
									int threshold = mapping[0][1]==0 ? mapping[0][1] : (int)((mapping[0][1]/100.00)*ANALOGMAX);
									currentAnalogState.put(code, value);
									int[] analogs = gameMap.getAnalogValues(code);
									for(int i=0;i<analogs.length;i++){
										int j=currentAnalogState.get(analogs[i]);
										vCon.sendAnalogEvent(analogs[i],j);
									}
									
									if(code==analogs[0]){
										if(value >= (ANALOGMAX-threshold)){
											vCon.sendKeyEvent(gameMap.getOutputCode("right"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("right"), 0);
										}
										else if(value <= 0+threshold){
											vCon.sendKeyEvent(gameMap.getOutputCode("left"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("left"), 0);
										}
									}
									else if(code==analogs[1]){
										if(value >= (ANALOGMAX-threshold)){
											vCon.sendKeyEvent(gameMap.getOutputCode("down"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("down"), 0);
										}

										else if(value <= 0+threshold){
											vCon.sendKeyEvent(gameMap.getOutputCode("up"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("up"), 0);
										}
									}
								}
								break;
							case ControllerMap.MAP_ANALOGDPAD:
								if(type==3){
									
									int threshold = mapping[0][1]==0 ? mapping[0][1] : (int)((mapping[0][1]/100.00)*ANALOGMAX);
									int[] analogs = gameMap.getAnalogValues(code);
									if(code==analogs[0]){
										if(value>ANALOGMAX/2 && value >= (ANALOGMAX-threshold)){
											vCon.sendKeyEvent(gameMap.getOutputCode("right"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("right"), 0);
										}
										else if(value<ANALOGMAX/2 && value <= 0+threshold){
											vCon.sendKeyEvent(gameMap.getOutputCode("left"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("left"), 0);
										}
									}
									else if(code==analogs[1]){
										if(value>ANALOGMAX/2 && value >= (ANALOGMAX-threshold)){
											vCon.sendKeyEvent(gameMap.getOutputCode("down"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("down"), 0);
										}

										else if(value<ANALOGMAX/2 && value <= 0+threshold){
											vCon.sendKeyEvent(gameMap.getOutputCode("up"), 1);
											vCon.sendKeyEvent(gameMap.getOutputCode("up"), 0);
										}
									}
								}
								break;
							case ControllerMap.MAP_TOUCHANALOG:
								if(type==3){
									//analog return is: x center, y center, radius, deadzone
									//send begin touchpoint at center

									double rad = (double) mapping[0][3];
									double analogToPx = ANALOGMAX/(rad*2);
									int normalizedValue = (int)(value/analogToPx-rad);
									currentAnalogState.put(code, normalizedValue);
									int dz = mapping[0][4];
									
									int x = gameMap.getAnalogValues(code)[0];
									x = currentAnalogState.get(x);
									int y = gameMap.getAnalogValues(code)[1];
									y = currentAnalogState.get(y);
									if(x < dz && x > -dz
											&& y < dz && y > -dz
											){
										if(analogStart.get(code)){
											vTS.endDragEvent();
											analogStart.put(code, false);
										}
										break;
									}
									else{
										//start
										if(!analogStart.get(code)){
											vTS.beginDragEvent(mapping[0][1], mapping[0][2]);
											analogStart.put(code,  true);
										}
										vTS.sendDragEvent(mapping[0][1]+x,mapping[0][2]+y);
									}
									//this is exactly the same for drag too!
									break;
								}
						}
					}
					if(mapping[1][0]!=0 && value==1){
						//has callback
						
						int[][] ret = gameMap.runCallback(code, mapping[1][0]);
						statusintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_STATUS");
						statusintent.putExtra("set",gameMap.getCurrentSet());
						statusintent.putExtra("cycle", gameMap.getCurrentCycle());
						service.sendBroadcast(statusintent);
						pointintent = new Intent("com.chrisjdowd.empio.mapper.UPDATE_POINT");
						pointintent.putExtra("coords", gameMap.getCyclePos());
						service.sendBroadcast(pointintent);
						if(ret[0][0]==-1)break;
						for(int i=0;i<ret.length;i++){
							if(ret[i][0]==ControllerMap.MAP_TOUCH){
								vTS.sendTouchEvent(ret[i][1], ret[i][2]);
							}
						}
					}
				}
			}
		}	
		if(exclusive)con.releaseExclusive();
		con.closeDevice();
		vCon.closeDevice();
		vTS.closeDevice();
		service.sendBroadcast(new Intent("com.chrisjdowd.empio.mapper.MAPPER_QUIT"));
		service = null;
		gameMap = null;
	}
	
	
}
