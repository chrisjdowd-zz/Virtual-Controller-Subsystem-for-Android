package com.chrisjdowd.empio.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.os.Environment;
import android.util.Log;
//import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class ControllerMap {

	private String dir = "OUYA Mappings";
	private boolean mapIsLoaded = false;
	private File mapini;
	private String map;
	
	private String currentSet = "";
	private String currentCycle = "";
	private int[] currentCoords = new int[2];
	
	public static final int MAP_KEY = 1;
	public static final int MAP_TOUCH = 2;
	public static final int MAP_ANALOG = 3;
	public static final int MAP_DRAG = 4;
	public static final int MAP_GESTURE = 5;
	public static final int MAP_CALLBACK = 6;
	public static final int MAP_TOUCHANALOG = 7;
	public static final int MAP_ANALOGDPAD = 8;
	public static final int MAP_ANALOGCYCLE = 9;
	public static final int MAP_ANALOGDPADANALOG = 10;
	public static final int CB_CYCLEUP = 1;
	public static final int CB_CYCLEDOWN = 2;
	public static final int CB_CYCLELEFT = 3;
	public static final int CB_CYCLERIGHT = 4;
	public static final int CB_CYCLESELECT = 5;
	public static final int CB_CYCLECHANGE = 6;
	public static final int CB_CYCLEPREV = 7;
	public static final int CB_CYCLENEXT = 8;
	public static final int CB_SETCHANGE = 9;
	public static final int CB_SETPREV = 10;
	public static final int CB_SETNEXT = 11;
	public static final int CB_SETVARIABLE = 12;
	public static final int FUNC_CYCLE = 1;
	public static final int FUNC_CYCLESELECTED = 2;
	public static final int FUNC_CYCLEPRESSED = 3;
	
	private Map<String, SparseArray<int[][]>> setMaps = new HashMap<String, SparseArray<int[][]>>();				//setname<button name, [maptype, args]
	private Map<String, SparseArray<String[]>> callbacks = new HashMap<String, SparseArray<String[]>>();			//setname<buttonname, [callback name, cbarg]>
	private Map<String, Map<String, String[][]>> cycles = new HashMap<String, Map<String, String[][]>>();			//setname<cyclename, [row, col]=x:y
	private Map<String, Map<String, String[][][]>> cycleselected = new HashMap<String, Map<String, String[][][]>>();	//setname<cyclename, [cyclepos, args]>
	private Map<String, Map<String, String[][][]>> cyclepressed = new HashMap<String, Map<String, String[][][]>>();		//setname<cyclename, [cyclepos, args]>
	private Map<String, String[]> cycleNames = new HashMap<String, String[]>();										//setname, [cyclepos, name]
	private String[] setNames = new String[1];
	
	private String mapVariable = "";
	private int currentCycleRow = 0;
	private int currentCycleCol = 0;
	private int currentCycleRowMax = 0;
	private int currentCycleColMax = 0;
	private int currentCyclePos = 0;
	private int currentCycleNum = 1;
	private int currentSetPos = 0;
	private SparseIntArray cyclePosMax = new SparseIntArray();
	
	private SparseArray<int[]> analogValues = new SparseArray<int[]>();
	
	private Map<String, Integer> functions = new HashMap<String, Integer>();
	private Map<String, Integer> inputButtons = new HashMap<String, Integer>();
	private Map<String, Integer> outputKeys = new HashMap<String, Integer>();
	private Map<String, Integer> mapTypes = new HashMap<String, Integer>();
	private Map<String, Integer> cbTypes = new HashMap<String, Integer>();
		
	private final boolean DEBUG = false;
	private void debug(String tag, String arg){
		if(DEBUG){
			Log.d(tag, arg);
		}
	}
	
	private String controllerType = "";
	
	protected ControllerMap(String controllerType, String loadedPackage){
		this.controllerType = controllerType;
		map = Environment.getExternalStorageDirectory().getAbsolutePath();
		map = map+"/"+dir+"/"+loadedPackage+".ini";
		debug("MAP",map);
				
		setupCommands();
		loadMap();
	}
	
	private void loadMap(){
		mapIsLoaded = false;
		currentSet="";
		currentCycle="";
		currentCoords = new int[2];
		currentCycleRow = currentCycleCol = currentCycleRowMax = currentCycleColMax =
				currentCyclePos = currentCycleNum = currentSetPos = 0;
		setMaps.clear();
		callbacks.clear();
		cycles.clear();
		cycleselected.clear();
		cyclepressed.clear();
		cycleNames.clear();
		mapVariable = "";
		cyclePosMax.clear();
		analogValues.clear();
		setNames = new String[1];
		debug("map","map loaded: "+isLoaded());
		debug("map","loading map: "+map);
		mapini = new File(map);
		BufferedReader in = null;
		try{
			if(!mapini.exists()){
				debug("map","map doesn't exist, exiting");
				mapIsLoaded = false;
				return;
			}
			debug("map","map "+map+" exists, continuing");
			in = new BufferedReader(new FileReader(mapini));
			String line;

			while((line = in.readLine()) != null){
				if(
						line.startsWith("//") ||
						line.equals("") || 
						line.length()==0
						)continue;
				
				if(line.contains("//")){
					line = line.substring(0, line.indexOf("//"));
				}
				
				line = line.replace(" ", "");
				line = line.replaceAll("\\r","");
				line = line.trim();
				String[] args = line.split(",");
				debug("MAP", "Adding: "+line);
				if(functions.containsKey(args[0])){
					//if the person is calling a function
					String[] funcargs = new String[args.length-1];
					for(int i=0;i<funcargs.length;i++){
						funcargs[i] = args[i+1];
					}
					setFunctionMap(args[0], funcargs);
				}
				else{
					String[] mapargs = new String[args.length-3];
					for(int i=0;i<mapargs.length;i++){
						mapargs[i] = args[i+3];
					}
					setDefaultMap(args[0], args[1], args[2], mapargs);
				}
				mapIsLoaded = true;
			}
			debug("map","done loading, loaded: "+isLoaded());
//			debugMaps();
			in.close();
		} catch(Exception e){
			e.printStackTrace();
			
			mapIsLoaded = false;
			debug("map","exception loading map, loaded: "+isLoaded());
		}
		debug("map","exiting load(), loaded: "+isLoaded());
	}
	
	public void reload(String packagename){
		map = Environment.getExternalStorageDirectory().getAbsolutePath();
		map = map+"/"+dir+"/"+packagename+".ini";
		debug("map","reloading map: "+map);
		loadMap();
	}
	
	public boolean contains(int keycode){
		if(setMaps.containsKey(currentSet))
		if(setMaps.get(currentSet).indexOfKey(keycode) >-1 ){
			return true;
		}
		return false;
	}
	
	public boolean containsCycles(){
		if(cycles.containsKey(currentSet))
			if(cycles.get(currentSet).containsKey(currentCycle)){
				return true;
			}
		return false;
	}
	
	protected String getCurrentSet(){
		debug("getcurrentset", currentSet);
		if(currentSet.length()==0){
			return "N/A";
		}
		else{
			debug("getcurrentset","returning: "+currentSet);
			return currentSet;
		}
	}

	protected String getCurrentCycle(){
		return currentCycle;
	}
	
	protected int[] getCyclePos(){
		debug("getcyclepos",Arrays.toString(currentCoords));
		//if landscape phone
//		int temp = currentCoords[0];
//		currentCoords[0] = currentCoords[1];
//		currentCoords[1] = temp;
		return currentCoords;
	}
	
	public int[] getAnalogValues(int analogKeycode){
		if(analogValues.indexOfKey(analogKeycode)>-1)
		return analogValues.get(analogKeycode);
		else return new int[] {analogKeycode};
	}

	public int[][] getKeyMapping(int keycode){
		return setMaps.get(currentSet).get(keycode);
	}
		
	public int getInputCode(String buttonName){
		if(inputButtons.containsKey(buttonName))
			return inputButtons.get(buttonName);
		else return -1;
	}
	
	public int getOutputCode(String buttonname){
		if(outputKeys.containsKey(buttonname))
			return outputKeys.get(buttonname);
		else return -1;
	}
	
	public boolean isLoaded(){
		return mapIsLoaded;
	}

	public int[][] runCallback(int keycode, int cb){
		//return: int with maptype,
		//check for args in callbacks map first when i add custom cb feature
		int[][] ret = new int[1][1];
		ret[0][0] = -1;
		int prevRow = currentCycleRow;
		int prevCol = currentCycleCol;
		debug("CALLBACK",""+cb);
		debug("ROW/COL",currentCycleRow+","+currentCycleCol);
		switch(cb){
			case CB_CYCLEUP:
				debug("callback","cycleup");
				currentCycleRow-=1;
				break;
			case CB_CYCLEDOWN:
				currentCycleRow+=1;
				break;
			case CB_CYCLELEFT:
				currentCycleCol-=1;
				break;
			case CB_CYCLERIGHT:
				currentCycleCol+=1;
				break;
			case CB_CYCLEPREV:
				if(cycleNames.containsKey(currentSet)){
					debug("CYCLEPREV", currentCyclePos+"");
					if(currentCyclePos>0)currentCyclePos--;
					else currentCyclePos = cycleNames.get(currentSet).length-1;
					debug("CYCLEPREV", currentCyclePos+"");
					currentCycle = cycleNames.get(currentSet)[currentCyclePos];
					debug("CYCLEPREV", currentCycle);
					String[][] sizes = cycles.get(currentSet).get(currentCycle);
					currentCycleRowMax = sizes.length;
					currentCycleColMax = sizes[0].length;
					currentCycleRow = currentCycleCol = 0;
					String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
					currentCoords[0] = Integer.parseInt(strCoords[0]);
					currentCoords[1] = Integer.parseInt(strCoords[1]);
				}
				
				break;
			case CB_CYCLENEXT:
				if(cycleNames.containsKey(currentSet)){
					if(currentCyclePos<cycleNames.get(currentSet).length-1)currentCyclePos++;
					else currentCyclePos = 0;
					currentCycle = cycleNames.get(currentSet)[currentCyclePos];
					String[][] sizes = cycles.get(currentSet).get(currentCycle);
					currentCycleRowMax = sizes.length;
					currentCycleColMax = sizes[0].length;
					currentCycleRow = currentCycleCol = 0;
					String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
					currentCoords[0] = Integer.parseInt(strCoords[0]);
					currentCoords[1] = Integer.parseInt(strCoords[1]);
				}
				
				break;
			case CB_SETNEXT:
				debug("SETNEXT","pos before change: "+currentSetPos);
				debug("setnext",Arrays.toString(setNames));
				if(currentSetPos<setNames.length-1)currentSetPos++;
				else currentSetPos = 0;
				debug("SETNEXT","pos after change: "+currentSetPos);
				currentSet = setNames[currentSetPos];
				if(cycleNames.containsKey(currentSet)){
					currentCycle = cycleNames.get(currentSet)[0];
					String[][] sizes = cycles.get(currentSet).get(currentCycle);
					currentCycleRowMax = sizes.length;
					currentCycleColMax = sizes[0].length;
					currentCycleRow = currentCycleCol = 0;
					String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
					currentCoords[0] = Integer.parseInt(strCoords[0]);
					currentCoords[1] = Integer.parseInt(strCoords[1]);
				}
				else{
					currentCycle = "";
					currentCycleRow = currentCycleCol = currentCyclePos = currentCycleRowMax = currentCycleColMax = 0;
					currentCoords[0] = -1;
					currentCoords[1] = -1;
				}
				
				break;
			case CB_SETPREV:
				if(currentSetPos>0)currentSetPos--;
				else currentSetPos = setNames.length-1;
				currentSet = setNames[currentSetPos];
				if(cycleNames.containsKey(currentSet)){
					currentCycle = cycleNames.get(currentSet)[0];
					String[][] sizes = cycles.get(currentSet).get(currentCycle);
					currentCycleRowMax = sizes.length;
					currentCycleColMax = sizes[0].length;
					currentCycleRow = currentCycleCol = 0;
					String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
					currentCoords[0] = Integer.parseInt(strCoords[0]);
					currentCoords[1] = Integer.parseInt(strCoords[1]);
				}
				else{
					currentCycle = "";
					currentCycleRow = currentCycleCol = currentCyclePos = currentCycleRowMax = currentCycleColMax = 0;
					currentCoords[0] = -1;
					currentCoords[1] = -1;
				}
				
				break;
			case CB_CYCLESELECT:
				ret = new int[1][3];
				ret[0][0] = MAP_TOUCH;
				String[][] coords = cycles.get(currentSet).get(currentCycle);
				debug("select","coords: "+Arrays.deepToString(coords));
				String[] xy = coords[currentCycleRow][currentCycleCol].split(":");
				debug("select","xy: "+Arrays.deepToString(xy));
				ret[0][1] = Integer.parseInt(xy[0]);
				ret[0][2] = Integer.parseInt(xy[1]);
				String[] pressed = cyclepressed.get(currentSet).get(currentCycle)[currentCycleRow][currentCycleCol];
				if(pressed!=null && pressed[0]!=null){
					debug("select","pressed isn't null");
					debug("select","getting callback");
					for(int i=0;i<pressed.length;i++){						
						debug("pressed","running pressed "+i+" in "+Arrays.deepToString(pressed));
						if(pressed[i]!=null){
							debug("pressed","running pressed "+i+": "+pressed[i]);
							String[] vals = pressed[i].split(":");
							cb = cbTypes.get(vals[0]);
							if(cb==CB_SETCHANGE){
								String newset = vals[1];
								debug("pressed setchange","newset "+newset);
								if(newset.equals("variable")) newset = mapVariable;
								debug("pressed setchange", "newset "+newset);
								if(setMaps.containsKey(newset)){
									debug("SETCHANGE", "contains "+newset);
									currentSet = newset;
									if(cycleNames.containsKey(currentSet)){
										currentCycle = cycleNames.get(currentSet)[0];
										String[][] sizes = cycles.get(currentSet).get(currentCycle);
										currentCycleRowMax = sizes.length;
										currentCycleColMax = sizes[0].length;
										currentCycleRow = currentCycleCol = 0;
										String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
										currentCoords[0] = Integer.parseInt(strCoords[0]);
										currentCoords[1] = Integer.parseInt(strCoords[1]);
									}
									else{
										currentCycle = "";
										currentCycleRow = currentCycleCol = currentCyclePos = currentCycleRowMax = currentCycleColMax = 0;
										currentCoords[0] = -1;
										currentCoords[1] = -1;
									}
								}
							}
							else if(cb==CB_CYCLECHANGE){
								debug("callback","running cyclechange in cycleselect");
								String newcycle = vals[1];
								debug("callback","new cycle "+newcycle);
								if(newcycle.equals("variable")) newcycle = mapVariable;
								if(cycles.get(currentSet).containsKey(newcycle)){
									debug("cyclechange", "New cycle: "+newcycle);
									currentCycle = newcycle;
									String[][] sizes = cycles.get(currentSet).get(currentCycle);
									currentCycleRowMax = sizes.length;
									currentCycleColMax = sizes[0].length;
									currentCycleRow = currentCycleCol = 0;
									String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
									currentCoords[0] = Integer.parseInt(strCoords[0]);
									currentCoords[1] = Integer.parseInt(strCoords[1]);
								}
							}
							else if(cb==CB_SETVARIABLE){
								debug("cyclepressed","setting variable to "+vals[1]);
								mapVariable = vals[1];
							}
						}
					}
				}
				debug("cyclepressed",Arrays.deepToString(ret));
				break;
			case CB_SETCHANGE:
				SparseArray<String[]> cbs = callbacks.get(currentSet);
				String[] vals = cbs.get(keycode);
				for(int i=0; i<vals.length; i++){
					debug("MAP", vals[i]);
				}
				
				String newset = vals[1];
				if(newset.equals("variable")) newset = mapVariable;
				debug("SETCHANGE", newset);
				if(setMaps.containsKey(newset)){
					debug("SETCHANGE", "contains "+newset);
					currentSet = newset;

					if(cycleNames.containsKey(currentSet)){
						currentCycle = cycleNames.get(currentSet)[0];
						debug("cycle change from setchange", currentCycle);
						String[][] sizes = cycles.get(currentSet).get(currentCycle);
						debug("cyclevals",Arrays.deepToString(sizes));
						currentCycleRowMax = sizes.length;
						debug("cyclerowmax",""+currentCycleRowMax);
						currentCycleColMax = sizes[0].length;
						debug("cyclecolmax",""+currentCycleColMax);
						currentCycleRow = currentCycleCol = 0;
						String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
						currentCoords[0] = Integer.parseInt(strCoords[0]);
						currentCoords[1] = Integer.parseInt(strCoords[1]);
					}
					else{
						currentCycle = "";
						currentCycleRow = currentCycleCol = currentCyclePos = currentCycleRowMax = currentCycleColMax = 0;
						currentCoords[0] = -1;
						currentCoords[1] = -1;
					}
				}
				break;
			case CB_CYCLECHANGE:
				debug("CYCLECHANGE", "running cyclechange");
				String newcycle = callbacks.get(currentSet).get(keycode)[1];
				if(newcycle.equals("variable")) newcycle = mapVariable;
				debug("cyclechange", newcycle);
				if(cycles.get(currentSet).containsKey(newcycle)){
					debug("cyclechange", "contains new cycle");
					currentCycle = newcycle;
				}
				debug("CYCLECHANGE", "new cycle is "+currentCycle);
				String[][] sizes = cycles.get(currentSet).get(currentCycle);
				debug("cyclechange", "New sizes: "+Arrays.deepToString(sizes));
				currentCycleRowMax = sizes.length;
				currentCycleColMax = sizes[0].length;
				currentCycleRow = currentCycleCol = 0;
				String[] strCoords = sizes[currentCycleRow][currentCycleCol].split(":");
				currentCoords[0] = Integer.parseInt(strCoords[0]);
				currentCoords[1] = Integer.parseInt(strCoords[1]);
				break;
			case CB_SETVARIABLE:
				debug("callback","running setvariable cb");
				mapVariable = callbacks.get(currentSet).get(keycode)[1];
				debug("callback","new variable: "+mapVariable);
				break;
		}
		if(currentCycleCol<0)currentCycleCol=0;
		if(currentCycleCol==currentCycleColMax)currentCycleCol=currentCycleColMax-1;
		if(currentCycleRow<0)currentCycleRow=0;
		if(currentCycleRow==currentCycleRowMax)currentCycleRow=currentCycleRowMax-1;
		if(prevRow!=currentCycleRow || prevCol!=currentCycleCol){
			if(!currentCycle.equals("")){
				String[] coords = cycles.get(currentSet).get(currentCycle)[currentCycleRow][currentCycleCol].split(":");
				debug("COORDS", cycles.get(currentSet).get(currentCycle)[currentCycleRow][currentCycleCol]);
				currentCoords[0] = Integer.parseInt(coords[0]);
				currentCoords[1] = Integer.parseInt(coords[1]);
			}
			if(cycleselected.containsKey(currentSet)){
				if(cycleselected.get(currentSet).containsKey(currentCycle)){
					String[] cbs = cycleselected.get(currentSet).get(currentCycle)[currentCycleRow][currentCycleCol];
					debug("selected", Arrays.toString(cbs));
					if(cbs != null && cbs[0]!=null){
						debug("selected","cbs is not null");
						int offset = 0;
						debug("selected","ret length: "+ret.length);
						ret[0][0] = 0;
						if(ret!=null){
							debug("selected","ret isn't null");
							offset = ret.length;
							int[][] tmp = ret;
							ret = new int[tmp.length+cbs.length][];
							for(int i=0;i<tmp.length;i++){
								ret[i] = tmp[i];
							}
							debug("selected","new ret size "+ret.length);
						}
						for(int i=0;i<cbs.length;i++){
							debug("selected","running cb "+i+": "+cbs[i]);
							if(cbs[i]!=null){
								String[] vals = cbs[i].split(":");
								ret[i+offset] = new int[vals.length];
								debug("selected","vals.length "+vals.length);
								debug("selected", "individual ret"+i+": "+ret[i+offset].length+", "+Arrays.toString(ret[i+offset]));
								debug("selected","vals: "+Arrays.toString(vals));
								debug("selected","ret before: "+Arrays.deepToString(ret));
								ret[i+offset][0] = mapTypes.get(vals[0]);
								debug("selected","ret after maptype: "+Arrays.deepToString(ret));

								for(int j=1;j<vals.length;j++){
									debug("selected", "i: "+i+", j: "+j+", vals len: "+vals.length);
									debug("selected", "ret: "+Arrays.toString(ret[i+offset]));
									debug("selected", "reti "+ret[i+offset].length);
									debug("selected", "retij: "+ret[i+offset][j]);
									debug("selected","valsj: "+vals[j]);
									ret[i+offset][j] = Integer.parseInt(vals[j]);
									debug("selected","ret after cb: "+Arrays.deepToString(ret));

								}
							}
						}
					}
				}
			}
		}
		debug("cyclerow",""+currentCycleRow);
		debug("cyclecol",""+currentCycleCol);
		return ret;
	}

	
	private void setupCommands(){
		//input buttons changes based on controller
		debug("SETUP","RUNNING SETUP");
		outputKeys.put("home", 102);
		outputKeys.put("back", 158);
		outputKeys.put("power", 116);
		outputKeys.put("search", 217);
		outputKeys.put("volumedown", 114);
		outputKeys.put("volumeup", 115);
		outputKeys.put("volumemnute", 113);
		
		outputKeys.put("o", 304);
		outputKeys.put("u", 307);
		outputKeys.put("y", 308);
		outputKeys.put("a", 305);
		outputKeys.put("l1", 310);
		outputKeys.put("lb", 310);
		outputKeys.put("l2", 312);
		outputKeys.put("l3", 317);
		outputKeys.put("r1", 311);
		outputKeys.put("rb", 311);
		outputKeys.put("r2", 313);
		outputKeys.put("r3", 318);
		outputKeys.put("system", 172);
		outputKeys.put("ouya", 172);
		outputKeys.put("menu", 172);
		outputKeys.put("menulp", 102);
		outputKeys.put("up", 103);
		outputKeys.put("down", 108);
		outputKeys.put("left", 105);
		outputKeys.put("right", 106);
		outputKeys.put("start", 0x13b);
		outputKeys.put("select", 0x13c);
		
		
		if(controllerType.equals("ouya")){
			inputButtons.put("o", 304);
			inputButtons.put("u", 305);
			inputButtons.put("y", 306);
			inputButtons.put("a", 307);
			inputButtons.put("l1", 308);
			inputButtons.put("lb", 308);
			inputButtons.put("r1", 309);
			inputButtons.put("rb", 309);
			inputButtons.put("l3", 310);
			inputButtons.put("r3",  311);
			inputButtons.put("up", 312);
			inputButtons.put("down", 313);
			inputButtons.put("left", 314);
			inputButtons.put("right", 315);
			inputButtons.put("l2", 316);
			inputButtons.put("l2analog", 0x30);
			inputButtons.put("lt",0x30);
			inputButtons.put("r2", 317);
			inputButtons.put("r2analog", 0x31);
			inputButtons.put("rt", 0x31);
			inputButtons.put("menu", 318);
			inputButtons.put("system", 318);
			inputButtons.put("start", 318);
			inputButtons.put("menulp", 319);
			inputButtons.put("analogleftx", 0x00);
			inputButtons.put("analoglefty", 0x01);
			inputButtons.put("leftanalog", 10);
			inputButtons.put("analogrightx", 0x03);
			inputButtons.put("analogrighty", 0x04);
			inputButtons.put("rightanalog", 11);
		}
		else if(controllerType.equals("ps3")){
			inputButtons.put("o", 302);
			inputButtons.put("u", 303);
			inputButtons.put("y", 300);
			inputButtons.put("a", 301);
			inputButtons.put("l1", 298);
			inputButtons.put("l2", 296);
			inputButtons.put("r1", 299);
			inputButtons.put("r2", 297);
			inputButtons.put("l2analog", 0x30);
			inputButtons.put("r2analog", 0x31);
			inputButtons.put("l3", 289);
			inputButtons.put("r3", 299);
			inputButtons.put("up", 292);
			inputButtons.put("down", 294);
			inputButtons.put("left", 295);
			inputButtons.put("right", 293);			
//			inputButtons.put("menu", 720);
			inputButtons.put("system", 291);
			inputButtons.put("menu",291);
			inputButtons.put("start", 291);
			inputButtons.put("select", 288);
			inputButtons.put("menulp", 720);
			inputButtons.put("analogleftx", 0x00);
			inputButtons.put("analoglefty", 0x01);
			inputButtons.put("leftanalog", 10);
			inputButtons.put("analogrightx", 0x02);
			inputButtons.put("analogrighty", 0x05);
			inputButtons.put("rightanalog", 11);
		}
		
		//maptypes is the same
		mapTypes.put("key", MAP_KEY);
		mapTypes.put("touch", MAP_TOUCH);
		mapTypes.put("callback", MAP_CALLBACK);
		mapTypes.put("analog", MAP_ANALOG);

		//callbacks and functions
		//mapTypes.put("drag", MAP_DRAG);
		mapTypes.put("touchanalog", MAP_TOUCHANALOG);
		mapTypes.put("dpad", MAP_ANALOGDPAD);
		mapTypes.put("analogdpad", MAP_ANALOGDPADANALOG);
		mapTypes.put("cycle", MAP_ANALOGCYCLE);
		cbTypes.put("cycleup", CB_CYCLEUP);
		cbTypes.put("cycledown", CB_CYCLEDOWN);
		cbTypes.put("cycleleft", CB_CYCLELEFT);
		cbTypes.put("cycleright", CB_CYCLERIGHT);
		cbTypes.put("cycleselect", CB_CYCLESELECT);
		cbTypes.put("setchange", CB_SETCHANGE);
		cbTypes.put("setprev", CB_SETPREV);
		cbTypes.put("setnext", CB_SETNEXT);
		cbTypes.put("cyclechange", CB_CYCLECHANGE);
		cbTypes.put("cycleprev", CB_CYCLEPREV);
		cbTypes.put("cyclenext", CB_CYCLENEXT);
		cbTypes.put("setvariable", CB_SETVARIABLE);
		functions.put("cycle", FUNC_CYCLE);
		functions.put("cycleselected", FUNC_CYCLESELECTED);
		functions.put("cyclepressed", FUNC_CYCLEPRESSED);
	}
	
	private void setFunctionMap(String functionName, String[] funcargs){
		//add try/catch for error catching/reporting. also because people are dumb
		//and will often fuck up maps. and corruption?
		switch(functions.get(functionName)){
			case FUNC_CYCLE:
				if(!cycleNames.containsKey(funcargs[0])){
					cycleNames.put(funcargs[0], new String[]{funcargs[1]});
				}
				else{
					String[] cyclenames = cycleNames.get(funcargs[0]);
					String[] newnames = new String[cyclenames.length+1];
					for(int i=0;i<cyclenames.length;i++){
						newnames[i] = cyclenames[i];
					}
					newnames[cyclenames.length] = funcargs[1];
					cycleNames.put(funcargs[0], newnames);
				}
				
				debug("FUNCTION MAP", "ADDING CYCLE");
				if(!cycles.containsKey(funcargs[0])){
					//if it doesnt contain the cycle name
					cycles.put(funcargs[0], new HashMap<String, String[][]>());
					cycleselected.put(funcargs[0], new HashMap<String, String[][][]>());
					cyclepressed.put(funcargs[0], new HashMap<String, String[][][]>());
				}
				Map<String, String[][]> cycle = cycles.get(funcargs[0]);
				int rows = Integer.parseInt(funcargs[2]);
				int cols = Integer.parseInt(funcargs[3]);
				debug("CYCLE","rows,cols: "+rows+","+cols);
				if(cycle.containsKey(funcargs[1])){
					//if it contains the cycle for the set
					cycle.remove(funcargs[0]);
				}
				else{
					cycle.put(funcargs[1], new String[rows][cols]);
					cycleselected.get(funcargs[0]).put(funcargs[1], new String[rows][cols][1]);
					cyclepressed.get(funcargs[0]).put(funcargs[1], new String[rows][cols][1]);
				}
				
				//funcargs = 	0		1			2	3	4	5	6	7 8	 9   10
				//funcargs = setname, cyclename, rows, cols,num, x, y, x, y, x, y
				debug("CYCLE", funcargs.toString());
				int num = Integer.parseInt(funcargs[4]);
				debug("CYCLE","Num of items: "+num);
				int offset = 5;
				String[][] idxs = new String[rows][cols];
				
				int row = 0;
				int col = 0;
				//i is place in function args for all x,y
				for(int idx=0;idx<num;idx++){
					int pos = offset+(idx*2);
					debug("CYCLE","position: "+pos);
					debug("CYCLE","row, col, args: "+row+","+col+","+funcargs[pos]+":"+funcargs[pos+1]);
					//switched because ouya is landscape
					idxs[row][col] = funcargs[pos]+":"+funcargs[pos+1];
					if(col==cols-1){
						col=0;
						row++;
					}
					else{
						col++;
					}
				}
				for(int idx=num; idx<(rows*cols); idx++){
					idxs[row][col] = "-1:-1";
					if(col==cols-1){
						col=0;
						row++;
					}
					else{
						col++;
					}
				}
				debug("addcycle",Arrays.deepToString(idxs));
				cycle.put(funcargs[1], idxs);
				cycles.put(funcargs[0], cycle);
				if(currentCycle.equals("")){
					currentCycle = funcargs[1];
					currentCycleRowMax = rows;
					currentCycleColMax = cols;
					String[] firstcoords = idxs[0][0].split(":");
					currentCoords[0] = Integer.parseInt(firstcoords[0]);
					currentCoords[1] = Integer.parseInt(firstcoords[1]);
				}
				debug("CYCLE", cycles.toString());
				break;
			case FUNC_CYCLESELECTED:
				debug("SELECTED", "Running cycle selected");

//				cycleselected = setname, <cyclename, [idx][maptype,args]
//				funcargs = setname, cyclename, row, col, callback, cbarg
				if(!mapTypes.containsKey(funcargs[4])){
					return;
				}
				if(!cycleselected.containsKey(funcargs[0])){
					debug("SELECTEd", "New - doesn't contain map yet. Creating...");
					cycleselected.put(funcargs[0], new HashMap<String, String[][][]>());
				}
				Map<String, String[][][]> setCycles = cycleselected.get(funcargs[0]);
				if(!setCycles.containsKey(funcargs[1])){
					return;
				}
				
				String[][][] cbs = setCycles.get(funcargs[1]);
				//res==callbacktype
				String res = funcargs[4];
				for(int i=5;i<funcargs.length;i++){
					//each cbarg
					res = res+=":"+funcargs[i];
				}
				debug("SELECTED", "res = "+res);
				row = Integer.parseInt(funcargs[2].replaceAll("\\D+", ""))-1;
				col = Integer.parseInt(funcargs[3].replaceAll("\\D+", ""))-1;
				String[] cb = null;
				
				if(row == -1){
					for(int i=0; i<cbs.length; i++){
						if(cbs[i][col]!=null){
							cb = cbs[i][col];
							String[] newcb = cb;
							cb = new String[newcb.length+1];
							for(int k=0;i<newcb.length;i++){
								cb[k] = newcb[k];
							}
							cb[cb.length-1] = res;
							cbs[i][col] = cb;
						}
					}
				}
				else if(col == -1){
					for(int i=0; i<cbs[row].length; i++){
						if(cbs[row][i]!=null){
							cb = cbs[row][i];
							String[] newcb = cb;
							cb = new String[newcb.length+1];
							for(int k=0;i<newcb.length;i++){
								cb[k] = newcb[k];
							}
							cb[cb.length-1] = res;
							cbs[row][i] = cb;
						}
					}
				}
				else if(row == -1 && col == -1){
					for(int i=0; i<cbs.length; i++){
						for(int j=0; j<cbs[i].length; j++){
							if(cbs[i][j]!=null){
								cb = cbs[i][j];
								String[] newcb = cb;
								cb = new String[newcb.length+1];
								for(int k=0;i<newcb.length;i++){
									cb[k] = newcb[k];
								}
								cb[cb.length-1] = res;
								cbs[i][j] = cb;
							}
						}
					}
				}
				else{
					if(cbs[row][col][0]!=null){
						debug("pressed","pressed exists, adding another");
						cb = cbs[row][col];
						String[] tmp = cb;
						cb = new String[tmp.length+1];
						for(int i=0;i<tmp.length;i++){
							cb[i] = tmp[i];
						}
						cb[cb.length-1] = res;
						cbs[row][col] = cb;
					}
					else{
						debug("pressed","no cb, adding first one");
						cbs[row][col][0] = res;
					}
				}
				
				debug("adding selected",Arrays.toString(cb));
				debug("adding selected",Arrays.deepToString(cbs));
				setCycles.put(funcargs[1], cbs);
				cycleselected.put(funcargs[0], setCycles);
				break;
			case FUNC_CYCLEPRESSED:
				debug("PRESSED", "Running cycle presseded");

//				cycleselected = setname, <cyclename, [idx][maptype,args]
//				funcargs = setname, cyclename, row, col, callback, cbarg
				debug("PRESSED", "arg4: "+funcargs[4]);
				if(!cbTypes.containsKey(funcargs[4])){
					return;
				}
				if(!cyclepressed.containsKey(funcargs[0])){
					debug("PRESSED", "New - doesn't contain map yet. Creating...");
					cyclepressed.put(funcargs[0], new HashMap<String, String[][][]>());
				}
				Map<String, String[][][]> cycles = cyclepressed.get(funcargs[0]);
				if(!cycles.containsKey(funcargs[1])){
					debug("SELECTEd", "cycles doesn't contain "+funcargs[0]+", returning");
					return;
				}
				cbs = cycles.get(funcargs[1]);
				debug("pressed","cbs1: "+cbs.length+":"+Arrays.deepToString(cbs));
				String ret = funcargs[4];
				for(int i=5;i<funcargs.length;i++){
					ret = ret+=":"+funcargs[i];
				}
				debug("SELECTED", "res = "+ret);
				row = Integer.parseInt(funcargs[2].replaceAll("\\D+", ""))-1;
				col = Integer.parseInt(funcargs[3].replaceAll("\\D+", ""))-1;
				cb = null;
				
				if(row == -1){
					for(int i=0; i<cbs.length; i++){
						for(int j=0;j<cbs[i][col].length;j++){
							if(cbs[i][col]!=null){
								cb = cbs[i][col];
								String[] newcb = cb;
								cb = new String[newcb.length+1];
								for(int k=0;k<newcb.length;k++){
									cb[i] = newcb[i];
								}
								cb[cb.length-1] = ret;
							}
						}
					}
				}
				else if(col == -1){
					for(int i=0; i<cbs[row].length; i++){
						for(int j=0;j<cbs[row][i].length;j++){
							if(cbs[row][i]!=null){
								cb = cbs[row][i];
								String[] newcb = cb;
								cb = new String[newcb.length+1];
								for(int k=0;i<newcb.length;i++){
									cb[k] = newcb[k];
								}
								cb[cb.length-1] = ret;
							}
						}
					}
				}
				else if(row == -1 && col == -1){
					for(int i=0; i<cbs.length; i++){
						for(int j=0; j<cbs[i].length; j++){
							if(cbs[i][j]!=null){
								cb = cbs[i][j];
								String[] newcb = cb;
								cb = new String[newcb.length+1];
								for(int k=0;i<newcb.length;i++){
									cb[k] = newcb[k];
								}
								cb[cb.length-1] = ret;
							}
						}
					}
				}
				else{
					debug("pressed","adding "+ret+" to single spot");
					if(cbs[row][col][0]!=null){
						debug("pressed","pressed exists, adding another");
						cb = cbs[row][col];
						String[] tmp = cb;
						cb = new String[tmp.length+1];
						for(int i=0;i<tmp.length;i++){
							cb[i] = tmp[i];
						}
						cb[cb.length-1] = ret;
						cbs[row][col] = cb;
					}
					else{
						debug("pressed","no cb, adding first one");
						cbs[row][col][0] = ret;
					}
				}
				debug("pressed","cbs: "+Arrays.deepToString(cbs));
				cycles.put(funcargs[1], cbs);
				cyclepressed.put(funcargs[0], cycles);
				break;
		}
	}
	
	private void setDefaultMap(String setName, String buttonName, String mapType, String[] mapargs){
		debug("default", "running");
		if(!setMaps.containsKey(setName)){
			setMaps.put(setName, new SparseArray<int[][]>());
			debug("default", "in !setmaps contains");
			if(currentSet.equals("")){
				debug("default map", "hey");
				currentSet = setName;
				debug("default map", currentSet);
			}
		}
		SparseArray<int[][]> set = setMaps.get(setName);
		int button = inputButtons.containsKey(buttonName) ? inputButtons.get(buttonName) : -1;
		if(set.indexOfKey(button) < 0){
			//int[maptype][args...]
			//0 is map with button or touch, 1 is callback with cb and optarg
			set.put(button, new int[2][2]);
		}
		int[][] maps = set.get(button);
		
		if(mapTypes.containsKey(mapType)){
			switch(mapTypes.get(mapType)){
				case MAP_KEY:
					debug("map","adding key");
					maps[0] = new int[2];
					debug("map","maps[0] = "+Arrays.toString(maps[0]));
					maps[0][0] = mapTypes.get(mapType);
					if(!outputKeys.containsKey(mapargs[0])){
						debug("map","exiting key");
						return;
					}
					debug("map","maparg: "+mapargs[0]);
					int key = outputKeys.get(mapargs[0]);
					debug("map", "key: "+key);
					maps[0][1] = key;
					set.put(button, maps);
					break;
				case MAP_TOUCH:
					maps[0] = new int[mapargs.length+1];
					for(int i=1;i<maps[0].length;i++){
						maps[0][i] = Integer.parseInt(mapargs[i-1].replaceAll("\\D+", ""));
					}
					maps[0][0] = mapTypes.get(mapType);
					set.put(button, maps);
					break;
				case MAP_ANALOG:
					debug("default map","adding analog");
					maps[0] = new int[3];
					maps[0][0] = mapTypes.get(mapType);
					Log.d("maps",maps[0][0]+"");
					Log.d("maps",inputButtons.toString());
					Log.d("maps", inputButtons.get("menu")+"");
					maps[0][1] = inputButtons.get(mapargs[0]);
					set.put(button, maps);
					switch(button){
						case 10: //left analog...
							set.put(inputButtons.get("analogleftx"), maps);
							set.put(inputButtons.get("analoglefty"), maps);
							int[] left = {inputButtons.get("analogleftx"), inputButtons.get("analoglefty")};
							debug("leftx",inputButtons.get("analogleftx")+"");
							debug("lefty",inputButtons.get("analoglefty")+"");
							debug("left",Arrays.toString(left));
							analogValues.put(inputButtons.get("analogleftx"), left);
							analogValues.put(inputButtons.get("analoglefty"), left);
							analogValues.put(inputButtons.get("leftanalog"), left);
							break;
						case 11: //right analog...
							set.put(inputButtons.get("analogrightx"), maps);
							set.put(inputButtons.get("analogrighty"), maps);
							int[] right = {inputButtons.get("analogrightx"), inputButtons.get("analogrighty")};
							analogValues.put(inputButtons.get("analogrightx"), right);
							analogValues.put(inputButtons.get("analogrighty"), right);
							analogValues.put(inputButtons.get("rightanalog"), right);
							break;
					}
					break;
				case MAP_ANALOGDPAD:
				case MAP_ANALOGDPADANALOG:
				case MAP_ANALOGCYCLE:
					maps[0] = new int[2];
					maps[0][0] = mapTypes.get(mapType);
					maps[0][1] = Integer.parseInt(mapargs[0].replaceAll("\\D+", ""));
					set.put(button, maps);
					switch(button){
						case 10: //left analog...
							set.put(inputButtons.get("analogleftx"), maps);
							set.put(inputButtons.get("analoglefty"), maps);
							int[] left = {inputButtons.get("analogleftx"), inputButtons.get("analoglefty")};
							debug("leftx",inputButtons.get("analogleftx")+"");
							debug("lefty",inputButtons.get("analoglefty")+"");
							debug("left",Arrays.toString(left));
							analogValues.put(inputButtons.get("analogleftx"), left);
							analogValues.put(inputButtons.get("analoglefty"), left);
							analogValues.put(inputButtons.get("leftanalog"), left);
							break;
						case 11: //right analog...
							set.put(inputButtons.get("analogrightx"), maps);
							set.put(inputButtons.get("analogrighty"), maps);
							int[] right = {inputButtons.get("analogrightx"), inputButtons.get("analogrighty")};
							analogValues.put(inputButtons.get("analogrightx"), right);
							analogValues.put(inputButtons.get("analogrighty"), right);
							analogValues.put(inputButtons.get("rightanalog"), right);
							break;
					}
					break;
				case MAP_TOUCHANALOG:
					maps[0] = new int[5];
					maps[0][0] = mapTypes.get(mapType);
					maps[0][1] = Integer.parseInt(mapargs[0].replaceAll("\\D+", ""));
					maps[0][2] = Integer.parseInt(mapargs[1].replaceAll("\\D+", ""));
					maps[0][3] = Integer.parseInt(mapargs[2].replaceAll("\\D+", ""));
					maps[0][4] = Integer.parseInt(mapargs[3].replaceAll("\\D+", ""));
					set.put(button, maps);
					switch(button){
						case 10: //left analog...
							set.put(inputButtons.get("analogleftx"), maps);
							set.put(inputButtons.get("analoglefty"), maps);
							int[] left = {inputButtons.get("analogleftx"), inputButtons.get("analoglefty")};
							debug("leftx",inputButtons.get("analogleftx")+"");
							debug("lefty",inputButtons.get("analoglefty")+"");
							debug("left",Arrays.toString(left));
							analogValues.put(inputButtons.get("analogleftx"), left);
							analogValues.put(inputButtons.get("analoglefty"), left);
							analogValues.put(inputButtons.get("leftanalog"), left);
							break;
						case 11: //right analog...
							set.put(inputButtons.get("analogrightx"), maps);
							set.put(inputButtons.get("analogrighty"), maps);
							int[] right = {inputButtons.get("analogrightx"), inputButtons.get("analogrighty")};
							analogValues.put(inputButtons.get("analogrightx"), right);
							analogValues.put(inputButtons.get("analogrighty"), right);
							analogValues.put(inputButtons.get("rightanalog"), right);
							break;
					}
					break;
				case MAP_CALLBACK:
					debug("adding callback", "adding callback");
					if(!cbTypes.containsKey(mapargs[0])){
						debug("CALLBACK","does not contain callback");
						return;
					}
					
					maps[1] = new int[1];
					debug("adding callback","maps: "+Arrays.toString(maps[1]));
					debug("adding callback","maparg: "+Arrays.toString(mapargs[0].toCharArray()));
					debug("adding callback","cbtypes.get: "+cbTypes.get(mapargs[0])+" cbtypes.contains: "+cbTypes.containsKey(mapargs[0]));
					maps[1][0] = cbTypes.get(mapargs[0]);
					debug("adding callback","maps: "+Arrays.toString(maps[1]));
					debug("adding callback", Arrays.deepToString(maps));
					set.put(button, maps);
					
					SparseArray<String[]> cb = callbacks.get(setName);
					if(cb == null){
						cb = new SparseArray<String[]>();
					}
					debug("MAP", "Adding button "+buttonName+","+inputButtons.get(buttonName)+" with args "+Arrays.toString(mapargs));
					cb.put(inputButtons.get(buttonName), mapargs);
					callbacks.put(setName, cb);	
					break;
			}
		}
		setNames = new String[setMaps.size()];
		Set<String> keys = setMaps.keySet();
		Iterator<String> it = keys.iterator();
		int i=0;
		while(it.hasNext()){
			setNames[i++] = it.next();
		}
		debug("SETNAMES",Arrays.toString(setNames));
	}

	public int getValue(String string) {
		return inputButtons.get(string);
	}
		
//	@SuppressWarnings("unused")
//	private void debugMaps(){
//		Set<String> setKeys = setMaps.keySet();
//		for(String key : setKeys){
//			debug("DEBUG", "Set name: "+key);
//			SparseArray<int[][]> button = setMaps.get(key);
//			Set<Integer> buttonKeys = button.
//			debug("buttonkeys", ""+buttonKeys);
//			for(int k : buttonKeys){
//				debug("DEBUG", "Button name: "+k);
//				int[][] vals = button.get(k);
//				for(int row=0;row<vals.length;row++){
//					for(int col=0;col<vals[row].length;col++){
//						debug("DEBUG","Row:"+row+" Col:"+col+" = "+vals[row][col]);
//					}
//				}
//			}
//		}
//		Set<String> cycleKeys = cycles.keySet();
//		for(String key : cycleKeys){
//			debug("DEBUG", "Cycle name: "+key);
//			Map<String, String[][]> cycle = cycles.get(key);
//			Set<String> keys = cycle.keySet();
//			for(String k : keys){
//				debug("DEBUG", "Set name: "+k);
//				String[][] c = cycle.get(k);
//				for(int row=0;row<c.length;row++){
//					for(int col=0;col<c[row].length;col++){
//						debug("DEBUG","Row:"+row+" Col:"+col+" = "+c[row][col]);
//					}
//				}
//			}
//		}
//		
//		Set<String> selectedKeys = cycleselected.keySet();
//		for(String key : selectedKeys){
//			debug("DEBUG","Cycle selected name:"+key);
//			Map<String, String[][]> c = cycleselected.get(key);
//			Set<String> set = c.keySet();
//			for(String s : set){
//				debug("DEBUG", "Set selected name:"+s);
//				String[][] args = c.get(s);
//				for(int row=0;row<args.length;row++){
//					for(int col=0;col<args[row].length;col++){
//						debug("DEBUG","Row:"+row+" Col:"+col+" = "+args[row][col]);
//					}
//				}
//			}
//		}
//		Set<String> pressedKeys = cyclepressed.keySet();
//		for(String key : pressedKeys){
//			debug("DEBUG","Cycle pressed name:"+key);
//			Map<String, String[][]> c = cyclepressed.get(key);
//			Set<String> set = c.keySet();
//			for(String s : set){
//				debug("DEBUG", "Set pressed name:"+s);
//				String[][] args = c.get(s);
//				for(int row=0;row<args.length;row++){
//					for(int col=0;col<args[row].length;col++){
//						debug("DEBUG","Row:"+row+" Col:"+col+" = "+args[row][col]);
//					}
//				}
//			}
//		}
//		Set<String> callbackKeys = callbacks.keySet();
//		for(String key : callbackKeys){
//			debug("DEBUG", "Callback setname: "+key);
//			Map<Integer, String[]> maps = callbacks.get(key);
//			Set<Integer> mapKeys = maps.keySet();
//			for(int i : mapKeys){
//				debug("DEBUG","Button code: "+i);
//				debug("DEBUG","Button args: "+Arrays.toString(maps.get(i)));
//			}
//		}
//	}
}
