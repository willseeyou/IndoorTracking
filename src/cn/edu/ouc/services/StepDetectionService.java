package cn.edu.ouc.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import cn.edu.ouc.algorithm.StepDetection;
import cn.edu.ouc.algorithm.StepTrigger;
import cn.edu.ouc.db.DatabaseHelper;

public class StepDetectionService extends Service implements StepTrigger {

	private static final String TAG = "StepDetectionService";

	private StepDetection mStepDetection;
	
	private float distance;
	
	SharedPreferences sharedPreferences;
	/* ----------------------------------------------*/
	// 数据库操作相关参数
	DatabaseHelper mHelper;
	SQLiteDatabase db;
	private static final String TBL_NAME = "track_tbl";
	
	// 客户通过mBinder和服务进行通信
	private final IBinder mBinder = new StepDetectionBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	/**
     * 通讯类，用于和客户端绑定。  
     * 因为StepDetectionService和其客户运行在同一个进程，所以不需要IPC。
     */
	public class StepDetectionBinder extends Binder {
		public StepDetectionService getService() {
			// 返回StepDetectionService实例，这样客户就可以调用服务的公共方法
			return StepDetectionService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "[StepDetectionService] onCreate");
		super.onCreate();
		distance = 0;
		sharedPreferences = getSharedPreferences("stepInfo", Context.MODE_PRIVATE);
		mHelper = new DatabaseHelper(this);
		db = mHelper.getWritableDatabase();
		mStepDetection = new StepDetection(this, this);
		startSensor();
	}
	
	@Override
	public void onDestroy() {
		stopSensor();
		super.onDestroy();
	}
	
	/**
	 * 启用传感器
	 */
	public void startSensor() {
		mStepDetection.startSensor();
	}
	
	/**
	 * 停止传感器
	 */
	public void stopSensor() {
		mStepDetection.stopSensor();
	}

	@Override
	public void trigger(int stepCount, float stepLength, float[] orientation) {
		distance += stepLength;
		Cursor c = db.query(TBL_NAME, null, null, null, null, null, null);
		Editor editor = sharedPreferences.edit();
		editor.putInt("stepCount", c.getCount());
		editor.putFloat("stepLength", stepLength);
		editor.putFloat("distance", distance);
		editor.putFloat("yaw", orientation[0]);
		editor.putFloat("pitch", orientation[1]);
		editor.putFloat("roll", orientation[2]);
		editor.commit();
	}
	
}
