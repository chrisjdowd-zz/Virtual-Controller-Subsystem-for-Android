package com.chrisjdowd.empio.views;

import com.chrisjdowd.empio.R;
//import tv.ouya.console.api.OuyaController;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;

public class PurchaseDialog extends Dialog{
	
	private static Handler handler;
	int w, h;

	public PurchaseDialog(Context context) {
		super(context);
		Display d = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point p = new Point();
		d.getSize(p);
		w = (int)(p.x * (2.0/3.0));
		h = (int)(p.y * (2.0/3.0));
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle("Purchase EMPIO Pro");
		setContentView(R.layout.purchase);
		getWindow().setLayout(w,h);
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event){
		switch(keycode){
			case 96://OuyaController.BUTTON_O:
				handler.sendEmptyMessage(1);
				dismiss();
				break;
//			case OuyaController.BUTTON_U:
//				//open features
//				openFeatures();
//				break;
//			case OuyaController.BUTTON_Y:
//				//open media. pictures, videos, etc
//				openMedia();
//				break;
			case 97://OuyaController.BUTTON_A:
				handler.sendEmptyMessage(0);
				dismiss();
				break;
		}
		return true;
	}
	
//	private void openFeatures(){
//		ImageView picture = (ImageView)findViewById(R.id.purchaseicon);
//		TextView title = (TextView)findViewById(R.id.purchasetitle);
//		TextView text = (TextView)findViewById(R.id.purchasetext);
//		picture.setImageDrawable(getContext().getResources().getDrawable(R.drawable.purchase_features));
//		title.setText(R.string.purchase_features_title);
//		text.setText(R.string.purchase_features_text);
//		
//	}
//	
//	private void openMedia(){
//		ImageView picture = (ImageView)findViewById(R.id.purchaseicon);
//		TextView title = (TextView)findViewById(R.id.purchasetitle);
//		TextView text = (TextView)findViewById(R.id.purchasetext);
//		picture.setImageDrawable(getContext().getResources().getDrawable(R.drawable.purchase_media));
//		title.setText(R.string.purchase_media_title);
//		text.setText(R.string.purchase_media_text);
//	}

	public void setHandler(Handler dialogHandler) {
		handler = dialogHandler;
	}
	
	public static Handler purchaseHandler = new Handler(){
		public void handleMessage(Message m){
			if(handler!=null){
				handler.sendEmptyMessage(0);
			}
		}
	};
}
