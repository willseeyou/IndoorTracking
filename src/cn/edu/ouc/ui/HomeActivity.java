package cn.edu.ouc.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cn.edu.ouc.R;
import cn.edu.ouc.preferences.Settings;
import cn.edu.ouc.services.StepDetectionService;

public class HomeActivity extends Activity {

	private static final String TAG = "HomeActivity";
	
	SharedPreferences sharedPreferences;
	
	private long time;
	private int stepCount;
	private float stepLength;
	private float velocity;
	private float distance;
	private float calorie;
	private int frequency;
	
	public final long INTERVAL_MS = 1000;
	
	private final OnClickListener recordListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), StepDetectionService.class);
			trackController.startTrack(getApplicationContext(), intent);
	        updateUI();
	        }
	};
	
	private final OnClickListener stopListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(timer != null)
			timer.cancel();
			trackController.stopTrack(getApplicationContext());
		}
		
	};

	StepDetectionService stepDetectionService;
	
	// The following are set in onCreate
	// Munu items
	private MenuItem resetMenuItem;
	private MenuItem settingsMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem quitMenuItem;
	
	// TextView
	private TextView stepValuesTextView;
	private TextView lengthValuesTextView;
	private TextView velocityValuesTextView;
	private TextView distanceValuesTextView;
	private TextView calorieValuesTextView;
	private TextView frequencyValuesTextView;
	
	// Button
	private Button openMapButton;
	
	private TrackController trackController;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "[HomeActivity] onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		sharedPreferences = getSharedPreferences("stepInfo", Context.MODE_PRIVATE);
		
		stepValuesTextView = (TextView) findViewById(R.id.step_values);
		lengthValuesTextView = (TextView) findViewById(R.id.length_values);
		velocityValuesTextView = (TextView) findViewById(R.id.velocity_values);
		distanceValuesTextView = (TextView) findViewById(R.id.distance_values);
		calorieValuesTextView = (TextView) findViewById(R.id.calorie_values);
		frequencyValuesTextView = (TextView) findViewById(R.id.frequency_values);
		
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
		reset();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home_activity_menu, menu);
		
		resetMenuItem = menu.findItem(R.id.home_activity_reset);
		settingsMenuItem = menu.findItem(R.id.home_activity_settings);
		helpMenuItem = menu.findItem(R.id.home_activity_help);
		quitMenuItem = menu.findItem(R.id.home_activity_quit);
		return true;
	}

	/* 处理菜单选择事件 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.home_activity_reset:
			if(!trackController.isRecording()) {
				reset();
			}
			else {
				Toast.makeText(this, R.string.reset_warning, Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.home_activity_settings:
			Intent intent = new Intent(this, Settings.class);
			startActivity(intent);
			return true;
		case R.id.home_activity_help:
			intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		case R.id.home_activity_quit:
			finish();
			return true;
		}
		return false;
	}
	
	// 重置所有数据
	private void reset() {
		Editor editor = sharedPreferences.edit();
		editor.putInt("stepCount", 0);
		editor.putFloat("stepLength", 0.0f);
		editor.putFloat("distance", 0.0f);
		editor.putLong("time", 0L);
		editor.putFloat("yaw", 0.0f);
		editor.putFloat("pitch", 0.0f);
		editor.putFloat("roll", 0.0f);
		editor.commit();
		trackController.reset();
		stepValuesTextView.post(new Runnable() {
			
			@Override
			public void run() {
				stepValuesTextView.setText("0");
			}
		});
		
		lengthValuesTextView.post(new Runnable() {
			@Override
			public void run() {
				lengthValuesTextView.setText("0.0");
			}
		});
		
		velocityValuesTextView.post(new Runnable() {
			@Override
			public void run() {
				velocityValuesTextView.setText("0.0");
			}
		});
		
		distanceValuesTextView.post(new Runnable() {
			@Override
			public void run() {
				distanceValuesTextView.setText("0.0");
			}
		});
		
		calorieValuesTextView.post(new Runnable() {
			@Override
			public void run() {
				calorieValuesTextView.setText("0.0");
			}
		});
		
		frequencyValuesTextView.post(new Runnable() {
			@Override
			public void run() {
				frequencyValuesTextView.setText("0");
			}
		});
					
	}
	
	/**
	 * 更新用户界面
	 */
	private void updateUI() {
		timer = new Timer("update UI", false);
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
					stepValuesTextView.post(new Runnable() {
						@Override
						public void run() {
							stepCount = sharedPreferences.getInt("stepCount", 1);	
							stepValuesTextView.setText(
									stepCount+"");	
						}
					});
					
					lengthValuesTextView.post(new Runnable() {
						@Override
						public void run() {
							stepLength = sharedPreferences.getFloat("stepLength", 0.0f);
							lengthValuesTextView.setText(
									(stepLength + "").length() > 4 ? (stepLength + "").subSequence(0, 4) : (stepLength + ""));
						}
					});
					
					velocityValuesTextView.post(new Runnable() {
						@Override
						public void run() {
							time = sharedPreferences.getLong("time", 0L);
							if(time == 0) velocity = 0;
							else {
							velocity = distance / time * 3600 / 1000;
							}
							velocityValuesTextView.setText(
									(velocity + "").length() > 5 ? (velocity + "").subSequence(0, 5) : (velocity + ""));
						}
					});
					
					distanceValuesTextView.post(new Runnable() {
						@Override
						public void run() {
							distance = sharedPreferences.getFloat("distance", 0.0f);
							distanceValuesTextView.setText(
									(distance + "").length() > 5 ? (distance + "").subSequence(0, 5) : (distance + ""));
						}
					});
					
					frequencyValuesTextView.post(new Runnable() {
						@Override
						public void run() {
							frequency = (int)(stepCount / (float) time * 60f);
							frequencyValuesTextView.setText(
									frequency + "");
						}
					});
			}
		};
		timer.schedule(timerTask, 0, INTERVAL_MS);
	}
	
	Timer timer;
	
}
