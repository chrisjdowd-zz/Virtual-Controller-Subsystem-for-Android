package com.chrisjdowd.empio.mapper;

import com.chrisjdowd.empio.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class ControllerMapper extends ViewGroup{

	private Context service;
	private ControllerMap gameMap;
	
	public ControllerMapper(Context context) {
		super(context);
		this.service = context;
		LayoutInflater.from(context).inflate(R.layout.mapper, null);
	}
	
	public ControllerMapper(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event){
//		Log.d("mapper","key: "+keycode);
		return true;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}
}