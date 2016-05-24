package com.chrisjdowd.empio.views;

import java.util.ArrayList;
import java.util.List;

import com.chrisjdowd.empio.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DonateDialog extends Dialog {
	
	public DonateDialog(Context context) {
		super(context);
	}

	Spinner s;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.donate);
		setTitle("Donate");
		s = (Spinner)findViewById(R.id.spinner1);
		int[] donateValues = getContext().getResources().getIntArray(R.array.donatevalues);
		List<String> values = new ArrayList<String>();
		for(int value : donateValues){
			values.add("$"+value);
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, values);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(dataAdapter);
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event){
		switch(keycode){
//			case OuyaController.BUTTON_O:
//				int donatevalue = Integer.parseInt(((String)s.getSelectedItem()).replaceAll("\\D+", ""));
//				OUYALicenseManager.donate(donatevalue);
//				break;
			case 97://OuyaController.BUTTON_A:
				dismiss();
				break;
		}
		return true;
	}
}
