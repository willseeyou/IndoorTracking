package cn.edu.ouc.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import cn.edu.ouc.R;
import cn.edu.ouc.service.StepDetectionService;
import cn.edu.ouc.service.StepDetectionService.StepDetectionBinder;

@SuppressLint("NewApi")
public class HomeActivity extends Activity {

	private static final String TAG = "HomeActivity";
	
	public final long INTERVAL_MS = 1000/30;
	
	
	StepDetectionService mService;
	boolean mBound = false;

	// The following are set in onCreate
	// Munu items
	private MenuItem searchMenuItem;
	private MenuItem startMenuItem;
	private MenuItem pauseMenuItem;
	private MenuItem resetMenuItem;
	private MenuItem settingsMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem quitMenuItem;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "[HomeActivity] onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mService = null;
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

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home_activity_menu, menu);
		
		searchMenuItem = menu.findItem(R.id.home_activity_search);
		startMenuItem = menu.findItem(R.id.home_activity_start);
		pauseMenuItem = menu.findItem(R.id.home_activity_pause);
		resetMenuItem = menu.findItem(R.id.home_activity_reset);
		settingsMenuItem = menu.findItem(R.id.home_activity_settings);
		helpMenuItem = menu.findItem(R.id.home_activity_help);
		quitMenuItem = menu.findItem(R.id.home_activity_quit);
		
		SearchManager searchManager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) searchMenuItem.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
		searchView.setQueryRefinementEnabled(true);
		
		return true;
	}

	/* 处理菜单选择事件 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.home_activity_start:
			/*Intent intent = new Intent(this, StepDetectionService.class);
	        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			mBound = true;*/
		case R.id.home_activity_pause:
			return true;
		case R.id.home_activity_reset:
			return true;
		case R.id.home_activity_settings:
			return true;
		case R.id.home_activity_help:
			return true;
		case R.id.home_activity_quit:
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
	
}
