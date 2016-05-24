package com.chrisjdowd.empio;

//import tv.ouya.console.api.OuyaController;
import java.util.Locale;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class FirstTimeActivity extends Activity {
	
	private int page = 0;
	private int pagemax = 7;
	RadioGroup paginator;
	ImageView picture;
	FragmentManager fm;
	SharedPreferences prefs;

	@Override
	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		if(android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
			setContentView(R.layout.intro);
		}
		else{
			setContentView(R.layout.intro_phone);
		}
		picture = (ImageView)findViewById(R.id.imageView1);
		
		fm = getFragmentManager();
		
		for(int i=0;i<pagemax; i++){
			if(android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
				paginator = (RadioGroup)findViewById(R.id.radioGroup1);
				RadioButton pageicon = new RadioButton(this);
				pageicon.setId(0xA+i);
				pageicon.setChecked(false);
				pageicon.setText("");
				pageicon.setFocusable(false);
				pageicon.setOnCheckedChangeListener(new OnCheckedChangeListener(){
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(isChecked){
							int id = buttonView.getId()-0xA;
							((TextView)findViewById(R.id.introtitle)).setText(getResources().getStringArray(R.array.introtitles)[id]);
							((TextView)findViewById(R.id.textView1)).setText(getResources().getStringArray(R.array.introtexts)[id]);
							((ImageView)findViewById(R.id.introimage)).setImageDrawable(getResources().getDrawable(R.drawable.intro0+id));
						}
					}
				});
				paginator.addView(pageicon);
			}
			else{
				Button prev = (Button)findViewById(R.id.button1);
				Button next = (Button)findViewById(R.id.button2);
				prev.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if(page>0){
							page--;
							((TextView)findViewById(R.id.introtitle)).setText(getResources().getStringArray(R.array.introtitles)[page]);
							((TextView)findViewById(R.id.textView1)).setText(getResources().getStringArray(R.array.introtexts)[page]);
							((ImageView)findViewById(R.id.introimage)).setImageDrawable(getResources().getDrawable(R.drawable.intro0+page));
						}
					}
				});
				next.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if(page<pagemax-1){
							page++;
							((TextView)findViewById(R.id.introtitle)).setText(getResources().getStringArray(R.array.introtitles)[page]);
							((TextView)findViewById(R.id.textView1)).setText(getResources().getStringArray(R.array.introtexts)[page]);
							((ImageView)findViewById(R.id.introimage)).setImageDrawable(getResources().getDrawable(R.drawable.intro0+page));	
							if(page==pagemax-1 && !android.os.Build.DEVICE.toLowerCase(Locale.US).contains("ouya")){
								Button next = (Button)findViewById(R.id.button2);
								next.setText("Finish");
								next.setOnClickListener(new OnClickListener(){
									@Override
									public void onClick(View v){
										finish();
									}
								});
								
							}
						}
					}
				});
			}
		}
		if(paginator!=null)((RadioButton)paginator.getChildAt(page)).setChecked(true);
		((TextView)findViewById(R.id.introtitle)).setText(getResources().getStringArray(R.array.introtitles)[page]);
		((TextView)findViewById(R.id.textView1)).setText(getResources().getStringArray(R.array.introtexts)[page]);
		((ImageView)findViewById(R.id.introimage)).setImageDrawable(getResources().getDrawable(R.drawable.intro0+page));						
		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event){
		boolean handled = false;
		switch(keycode){
			case 21://OuyaController.BUTTON_DPAD_LEFT:
			case 102://OuyaController.BUTTON_L1:
				handled = true;
				if(page>0){
					((RadioButton)paginator.getChildAt(page)).setChecked(false);
					page--;
					((RadioButton)paginator.getChildAt(page)).setChecked(true);
				}
				break;
			case 22://OuyaController.BUTTON_DPAD_RIGHT:
			case 103://OuyaController.BUTTON_R1:
				handled = true;
				if(page<pagemax-1){
					((RadioButton)paginator.getChildAt(page)).setChecked(false);
					page++;
					((RadioButton)paginator.getChildAt(page)).setChecked(true);
				}
				break;
			case KeyEvent.KEYCODE_BACK:
			case 97://OuyaController.BUTTON_A:
			case 96://OuyaController.BUTTON_O:
				handled = true;
				finish();
				
				break;
		}
		return handled;
	}
	
	@Override
	public void finish(){
		Editor editor = prefs.edit();
		editor.putBoolean("introshown", true);
		editor.commit();
		super.finish();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
}
