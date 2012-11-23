package cn.edu.ouc.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import cn.edu.ouc.R;
import cn.edu.ouc.service.StepDetectionService;
import cn.edu.ouc.service.StepDetectionService.StepDetectionBinder;

public class HomeActivity extends BaseActivity {

	private static final String TAG = "HomeActivity";
	
	private TextView stepTextView;
	
	StepDetectionService mService;
	boolean mBound = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "[HomeActivity] onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mService = null;
		stepTextView = (TextView) findViewById(R.id.step_value);
	}
	
	@Override
	protected void onStart() {
		Log.i(TAG, "[HomeActivity] onStart");
		super.onStart();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "[HomeActivity] onPause");
		super.onPause();
	}

	@Override
	protected void onRestart() {
		Log.i(TAG, "[HomeActivity] onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "[HomeActivity] onResume");
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "[HomeActivity] onDestroy");
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
		super.onDestroy();
	}



	private static final int MENU_SETTINGS = 8;
	private static final int MENU_QUIT = 9;

	private static final int MENU_PAUSE = 1;
	private static final int MENU_RESET = 2;
	
	/* 创建菜单 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		
		menu.add(0, MENU_PAUSE, 0, R.string.pause)
					.setIcon(android.R.drawable.ic_media_pause)
					.setShortcut('1', 'p');
		menu.add(0, MENU_RESET, 0, R.string.reset)
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
				.setShortcut('2', 'r');
		menu.add(0, MENU_SETTINGS, 0, R.string.settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShortcut('8', 's');
		menu.add(0, MENU_QUIT, 0, R.string.quit)
				.setIcon(android.R.drawable.ic_lock_power_off)
				.setShortcut('9', 'q');
		return true;
	}

	/* 处理菜单选择事件 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PAUSE:
			Intent intent = new Intent(this, StepDetectionService.class);
	        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			mBound = true;
			return true;
		case MENU_RESET:
			load();
			return true;
		case MENU_QUIT:
			finish();
			return true;
		}
		return false;
	}
	
	/** 定义服务绑定的回调函数，传给bindService */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			StepDetectionBinder binder = (StepDetectionBinder) service;
			mService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBound = false;
		}
		
	};
	public final long INTERVAL_MS = 1000/30;

	Timer timer;
	
	public void load() {
		timer = new Timer("UpdateData", false);
		TimerTask task = new TimerTask(){

			@Override
			public void run() {
				stepTextView.post(new Runnable() {
					
					@Override
					public void run() {
						stepTextView.setText(mService.getTest() + "");
					}
				});
			}
		};
		timer.schedule(task, 0, INTERVAL_MS);
	}
	
}
