package com.chrisjdowd.empio;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.paypal.android.sdk.a;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class WiimoteSyncActivity extends Activity {

	
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		
		setContentView(R.layout.wiimotesync);
		
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		String macAddress = wInfo.getMacAddress(); 
		String[] mac = macAddress.split(":");
		Log.d("mac",Arrays.toString(mac));
		String[] newmac = new String[mac.length];
		for(int i=0;i<mac.length;i++){
			newmac[i] = mac[mac.length-1-i];
		}
		Log.d("new mac",Arrays.toString(newmac));
		
		Button sync = (Button)findViewById(R.id.syncbutton);
		sync.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
				if(ba.startDiscovery()){
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
					intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
					registerReceiver(discoveryFinishedReceiver, intentFilter);	
				}
			}
			
		});
		
	}
	BluetoothDevice wiimote = null;
	BroadcastReceiver discoveryFinishedReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, "Done discovering", Toast.LENGTH_SHORT).show();
			String action = intent.getAction();
			if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
				if(wiimote!=null){
					try {
						BluetoothSocket bs = wiimote.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001124-0000-1000-8000-00805f9b34fb"));
						BluetoothServerSocket bss = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("EMPIO", UUID.fromString("00001124-0000-1000-8000-00805f9b34fb"));
						bs.connect();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else if(action.equals(BluetoothDevice.ACTION_FOUND)){
				wiimote = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);
				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
			}
		}
	};
}
