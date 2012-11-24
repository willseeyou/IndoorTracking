package cn.edu.ouc.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import cn.edu.ouc.algorithm.StepDetection;
import cn.edu.ouc.algorithm.StepTrigger;

public class StepDetectionService extends Service implements StepTrigger {

	private static final String TAG = "StepDetectionService";

	private StepDetection mStepDetection;
	
	private int stepCount = 0;
	
	private float[] orientation = new float[3];
	
	private static final int HAND_HELD = 1; // 手持
	private static final int POCKET = 2; //口袋
	
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
		mStepDetection = new StepDetection(this, this, 300);
		mStepDetection.startSensor();
		Toast.makeText(getBaseContext(), "StepDetection started", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDestroy() {
		mStepDetection.stopSensor();
		super.onDestroy();
	}

	@Override
	public void trigger(int stepCount, float length, float[] orientation) {
		this.stepCount = stepCount;
		this.orientation = orientation;
	}
	
	// 获取探测脚步数
	public int getStep() {
		return stepCount;
	}
	
	// 获取行进方向
	public float getHeading(int CARRY_MODEL) {
		switch(CARRY_MODEL) {
		case HAND_HELD:
			return (float) ((orientation[0] * 360 / Math.PI + 360) % 360); 
		case POCKET:
			return (float) ((orientation[1] * 360 / Math.PI + 360) % 360); 
		default:
			return (float) ((orientation[0] * 360 / Math.PI + 360) % 360); 
		}
		
	}
}
