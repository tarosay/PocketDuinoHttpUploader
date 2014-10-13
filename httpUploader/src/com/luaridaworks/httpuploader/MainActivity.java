package com.luaridaworks.httpuploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.programmer.avr.UploadErrors;

public class MainActivity extends Activity {
	public static final String TAG = MainActivity.class.getSimpleName();

	public static MainActivity MainActivity_Thread;
	public static Handler MainActivity_Handler;

	private Myhttpd prServer = null;
	private int prPort = 8080;

	
	Button btOpen, btClose;
	TextView tvNotification;
	//Boolean blnButtonState = false;
	Physicaloid mPhysicaloid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//画面縦固定にする
		//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		lockScreenOrientation(true);
		
		setContentView(R.layout.activity_main);

		btOpen  = (Button) findViewById(R.id.btOpen);
		btClose = (Button) findViewById(R.id.btClose);
		tvNotification  = (TextView) findViewById(R.id.tvNotification);

		setEnabledUi(false);

		mPhysicaloid = new Physicaloid(this);

		//****************************************************************
		// TODO : register intent filtered actions for device being attached or detached
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		//****************************************************************
		
		//どこからでも呼び出せるようにスレッドをセットします
		MainActivity_Thread = this;
		//ハンドラを生成する
		MainActivity_Handler = new Handler();
		
		//Log.e(TAG, "onDreate");
	}

	@Override
	protected void onResume() {
		super.onResume();

		httpStart();

		openPhysicaloidDevice();

		//Log.e(TAG, "onResume");
	}
	 
	@Override
	protected void onPause() {
		super.onPause();
		if (prServer != null){
			//prServer.stop();
		}

		//Log.e(TAG, "onPause");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (prServer != null){
			prServer.stop();
		}

		//****************************************************************
		// TODO : unregister the intent filtered actions
		unregisterReceiver(mUsbReceiver);
		//****************************************************************
		
		closePhysicaloidDevice();

		try {
			Thread.sleep(200);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		prServer = null;
		mPhysicaloid = null;
		
		//Log.e(TAG, "onDestroy");

		//強制終了の案内を表示します
		//NotificationOfExitByForce();

		// システムを強制的に終わらせる
		ExitByForce();	//本当はこんなことしたくないんだけど、NanoHTTPD#stopでスレッドのjoinが戻ってこないので・・。
	}

	//**************************************************
	// 強制終了の案内を表示します
	//**************************************************
	private void NotificationOfExitByForce(){
			
		//OKボタン
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setIcon(R.drawable.ic_launcher)
		.setTitle("Notification")
		.setMessage(R.string.ended)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//OKボタンをクリックした時の処理
			}
		}).show();

		dialog.setOnDismissListener(new OnDismissListener(){
			@Override
			public void onDismiss(DialogInterface dialog) {

				// システムを強制的に終わらせる
				ExitByForce();
			}
		});
	}
    	
	//**************************************************
	// システムを強制的に終わらせる
	//**************************************************
    public void ExitByForce(){
        //****** 強制終了 *******
        //System.exit(RESULT_OK);
    	//moveTaskToBack(true);
    	Process.killProcess(Process.myPid());
    }
    
	public void onClickOpen(View v) {
		openPhysicaloidDevice();
	}

	private void openPhysicaloidDevice() {
		
		if(mPhysicaloid == null){
			mPhysicaloid = new Physicaloid(this);
		}
		
		if (!mPhysicaloid.isOpened()) {
			if (mPhysicaloid.open()) { // default 9600bps
				setEnabledUi(true);
/*
				mPhysicaloid.addReadListener(new ReadLisener() {
					String readStr;

					// callback when reading one or more size buffer
					@Override
					public void onRead(int size) {
						byte[] buf = new byte[size];

						mPhysicaloid.read(buf, size);
						try {
							readStr = new String(buf, "UTF-8");
						}
						catch (UnsupportedEncodingException e) {
							Log.e(TAG, e.toString());
							return;
						}

						// UI thread
						tvAppend(readStr);
					}
				});
*/
			}
		}
	}

	public void onClickClose(View v) {
		closePhysicaloidDevice();
	}

	private void closePhysicaloidDevice() {
		if(mPhysicaloid.close()) {
			setEnabledUi(false);
			mPhysicaloid.clearReadListener();
		}
	}

	private UploadCallBack mUploadCallback = new UploadCallBack() {
		@Override
		public void onPreUpload() {
			tvClear("Upload : Start\n");
		}

		@Override
		public void onUploading(int value) {
			tvClear("Upload : " + value + "%\n");
		}

		@Override
		public void onPostUpload(boolean success) {
			if(success) {
				tvAppend("Upload : Successful\n");
            }
			else {
				tvAppend("Upload fail\n");
			}
		}

		@Override
		public void onCancel() {
			tvAppend("Cancel uploading\n");
		}

		@Override
		public void onError(UploadErrors err) {
			tvAppend("Error  : "+err.toString()+"\n");
		}
	};

	public void fileUpload(String filename) {
		
		if (!mPhysicaloid.isOpened()) {
			Log.i(TAG, "Physicaloid do not Open");
			return;
		}
		
		try {
			tvNotification.setText("");
			
            File file = new File(filename);
            FileInputStream fi = new FileInputStream(file);
			mPhysicaloid.upload(Boards.POCKETDUINO,fi, mUploadCallback);
		}
		catch (RuntimeException e) {
			Log.e(TAG, e.toString());
		}
		catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}

	Handler mHandler = new Handler();
	private void tvAppend(CharSequence text) {
		final TextView ftv = tvNotification;
		final CharSequence ftext = text;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				ftv.append(ftext);
			}
		});
	}

	private void tvClear(CharSequence text) {
		final TextView ftv = tvNotification;
		final CharSequence ftext = text;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				ftv.setText(ftext);
			}
		});
	}

	private void setEnabledUi(boolean on) {
		//blnButtonState = on;
		if(on) {
			btOpen.setEnabled(false);
			btClose.setEnabled(true);
			tvNotification.setEnabled(true);
			tvNotification.setText("");
			tvAppend("http://" + prServer.getIP() + ":" + String.valueOf(prPort) + "/\n");
        }
		else {
			btOpen.setEnabled(true);
			btClose.setEnabled(false);
			tvNotification.setEnabled(false);
		}
	}

	private boolean httpStart(){

		if(prServer == null){
			prServer = new Myhttpd(prPort);
		}
		try {
			prServer.start();
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
     * 画面の回転を固定・解除する関数
     * @param flg 真なら回転固定 偽なら回転可能
     */
    public void lockScreenOrientation(Boolean flg){
        if(flg){
            switch (((WindowManager) this.getSystemService(Activity.WINDOW_SERVICE))
                    .getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
            	 this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
            	 this.setRequestedOrientation(9/* reversePortait */);
                break;
            case Surface.ROTATION_270:
            	 this.setRequestedOrientation(8/* reverseLandscape */);
                break;
            default :
            	 this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        else{
        	 this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
    
    
	//****************************************************************
	// TODO : get intent when a USB device attached
	protected void onNewIntent(Intent intent) {
		String action = intent.getAction();

		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			openPhysicaloidDevice();
		}
	};
	//****************************************************************

	//****************************************************************
	// TODO : get intent when a USB device detached
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				closePhysicaloidDevice();
			}
		}
	};
	//****************************************************************
}
