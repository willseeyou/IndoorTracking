package cn.edu.ouc.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SearchView;
import cn.edu.ouc.R;
import cn.edu.ouc.services.StepDetectionService;

public class HomeActivity extends Activity {

	private static final String TAG = "HomeActivity";
	
	public final long INTERVAL_MS = 1000/30;
	
	private final OnClickListener recordListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), StepDetectionService.class);
	        trackController.bindService(getApplicationContext(), intent);
		}
	};
	
	private final OnClickListener stopListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			trackController.unbindService(getApplicationContext());
		}
		
	};

	StepDetectionService stepDetectionService;
	
	// The following are set in onCreate
	// Munu items
	private MenuItem searchMenuItem;
	private MenuItem startMenuItem;
	private MenuItem pauseMenuItem;
	private MenuItem resetMenuItem;
	private MenuItem settingsMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem quitMenuItem;
	
	private Button openMapButton;
	
	private TrackController trackController;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "[HomeActivity] onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		openMapButton = (Button) findViewById(R.id.open_map_btn);
		openMapButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), GMapActivity.class);
				startActivity(intent);
			}
		});
		trackController = new TrackController(this, true, recordListener, stopListener);
	}
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "[HomeActivity] onDestroy");
		if(!trackController.isRecording())
		trackController.unbindService(this);
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
			return true;
		case R.id.home_activity_pause:
			return true;
		case R.id.home_activity_reset:
			return true;
		case R.id.home_activity_settings:
			return true;
		case R.id.home_activity_help:
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		case R.id.home_activity_quit:
			finish();
			return true;
		}
		return false;
	}
	
}
