/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.algorithm;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;
import cn.edu.ouc.db.DatabaseHelper;
import cn.edu.ouc.preferences.IndoorTrackSettings;
import cn.edu.ouc.util.StepDetectionUtil;

/**
 * StepDetection类用于脚步探测.
 * 当探测到脚步时，调用回调函数StepTrigger,并将步数、步长和方向当作参数传递.
 * 
 * @author Chu Hongwei, Hong Feng
 */
public class StepDetection {

	private static final String TAG = StepDetection.class.getSimpleName();
	
	private StepTrigger st; // 使用接口StepTrigger向外部组件通知脚步探测情况
	
	private static SensorManager mSensorManager;
	
	/* ----------------------------------------------*/
	// 用于checkForStep方法的参数
	private float[] accel = new float[3]; // 瞬时加速度
	private float[] orientation = new float[3]; // 瞬时磁力计方向
	private List<float[]> accelList = new ArrayList<float[]>(); // 加速度列表
	private List<float[]> orientationList = new ArrayList<float[]>(); // 磁力计方向列表
	private List<float[]> gyroOrientationList = new ArrayList<float[]>(); // 陀螺仪方向列表
	private static final int W = 15; // 局部窗口大小，用于计算局部平均加速度和方差
	private int swSize; // 滑动窗口大小
	
	private static final int BLOCKSIZE = 8; // 连续1或连续0的阈值
	private int stepCount; //探测脚步数
	private double stepLength; //步长
	
	
	/* ----------------------------------------------*/
	// 用于gyroFunction方法的参数
	public static final float EPSILON = 0.000000001f;
	private float[] gyro = new float[3]; // 陀螺仪数据
	private float timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f; // 纳秒到秒的转换
    public float[] matrix = new float[9]; // 旋转矩阵
    private float[] gyroOrientation = new float[3]; // 陀螺仪采集的方向
    
    /* ----------------------------------------------*/
	// 数据库操作相关参数
    DatabaseHelper mHelper;
	SQLiteDatabase db;
	private static final String TBL_NAME = "track_tbl";
	double lat = 36.16010; // 经度
    double lng = 120.491951; // 纬度
    
    /* ----------------------------------------------*/
	// HDE方向补偿相关参数
	/* 误差标志: 
	 * 如果E为正（行走方向偏向主方向的左侧）,控制器IController增加固定增量IC.
	 * 如果E为负，控制器IController减少固定增量IC.
	 */
	private float E = 0.0f;
	private float IC = -0.0001f; // 固定增量，表示控制器IController的补偿因子
	private static final float DELTA = 90f; // 主方向间隔角度：各主方向互相垂直
	float IController = 0; // 控制器，用来消除偏移误差
	private int SIGN = 0; // 判断行走方向偏离主方向的哪一侧，SIGN = 1偏向左侧，SIGN = 0偏向右侧
	private double priOrientation = 0f; // 前一步的方向
	private boolean STEPDETECTED = false; // 脚步探测标志
	 
	// 参数设定
	SharedPreferences mSettings;
	IndoorTrackSettings mIndoorTrackSettings;
	
	// 算法设定
	private final static int MAGNETIC_BASED_ALGORITHM = 1;
	private final static int GYROSCOPE_BASED_ALGORITHM = 2;
	private final static int HDE_BASED_ALGORITHM = 3;
	private final static int PSP_BASED_ALGORITHM = 4;
	
	// 手机放置位置设定
	private final static int HAND_HELD = 1;
	private final static int TROUSER_POCKET = 2;
	
	// 步长计算方式设定
	private final static boolean FIXED_STEP_LENGTH = true;
	
    
	/**
	 * 构造函数
	 * @param context
	 * @param stepTrigger 接口，用于实现回调
	 * @param swSize 滑动窗口大小
	 */
	public StepDetection(Context context, StepTrigger stepTrigger) {
		this.st = stepTrigger;
		mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        mIndoorTrackSettings = new IndoorTrackSettings(mSettings);
        swSize = mIndoorTrackSettings.getSensitivity();
		stepCount = 0;
		mHelper = new DatabaseHelper(context);
		db = mHelper.getWritableDatabase();
		
		matrix[0] = 1.0f; matrix[1] = 0.0f; matrix[2] = 0.0f;
		matrix[3] = 0.0f; matrix[4] = 1.0f; matrix[5] = 0.0f;
		matrix[6] = 0.0f; matrix[7] = 0.0f; matrix[8] = 1.0f;
		
		gyroOrientation[0] = 0.0f; gyroOrientation[1] = 0.0f; gyroOrientation[2] = 0.0f;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	/**
	 * 处理传感器事件
	 */
	public SensorEventListener mSensorEventListener = new SensorEventListener() {
		
		@SuppressWarnings("deprecation")
		@Override
		public void onSensorChanged(SensorEvent event) {
			switch(event.sensor.getType()) {
		    case Sensor.TYPE_ACCELEROMETER:
		    	System.arraycopy(event.values, 0, accel, 0, 3);
		        break;
		        
		    case Sensor.TYPE_GYROSCOPE:
		    	gyroFunction(event); // 处理陀螺仪数据
		    	break;
		    	
		    case Sensor.TYPE_ORIENTATION:
		    	System.arraycopy(event.values, 0, orientation, 0, 3);
		    	break;
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
	};
	
	/**
	 * 注册传感器
	 */
	@SuppressWarnings("deprecation")
	public void startSensor() {
		Log.i(TAG, "[StepDetection] startSensor");
		mSensorManager.registerListener(mSensorEventListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		
		mSensorManager.registerListener(mSensorEventListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_FASTEST);
		
		mSensorManager.registerListener(mSensorEventListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	/**
	 * 注销传感器
	 */
	public void stopSensor() {
		Log.i(TAG, "[StepDetection] stopSensor");
		mSensorManager.unregisterListener(mSensorEventListener);
		accelList.clear();
		gyroOrientationList.clear();
	}
	
	/**
	 * 脚步探测算法，利用行走的加速度特征判断脚步
	 */
	private void checkForStep(List<float[]> orientationList) {
		Log.i(TAG, "[StepDetection] checkForStep");
		
		List<Float> magnitudeOfAccel = StepDetectionUtil.getMagnitudeOfAccel(accelList);
		List<Float> localMeanAccel = StepDetectionUtil.getLocalMeanAccel(magnitudeOfAccel, W);
		float threshold = StepDetectionUtil.getAverageLocalMeanAccel(localMeanAccel) + 0.5f;
		List<Integer> condition = StepDetectionUtil.getCondition(localMeanAccel, threshold);
		
		int numOne = 0; // 记录判断条件condition中，连续1的个数
		int numZero = 0; // 记录判断条件condition中，连续0的个数
		boolean flag = false; // 记录当前点是1还是0
		
		// 通过数连续1和连续0的个数判断脚步
		for(int i = 0, j = 1; i < swSize - 1 && j < swSize - W; i++, j++) {
			flag = StepDetectionUtil.isOne(condition.get(i)); // 判断前一个采样点的判断条件i是否为1
			/* 如果前一个采样点i的判断条件和当前采样点j的判断条件相同，
			 * 并且都等于1，则numOne加1. */
			if((condition.get(i) == condition.get(j)) && flag == true) 
			{				
				numOne++;
			}
			/* 如果前一个采样点i的判断条件和当前采样点j的判断条件相同，
			 * 并且都等于0，则numZero加1. */
			if((condition.get(i) == condition.get(j)) && flag == false) 
			{
				numZero++;	
			}
			/* 如果前一个采样点i不等于当前采样点j的值，
			 * 并且连续1和连续0的个数均大于BLOCKSIZE，则探测到脚步，
			 * 将numOne和numZero置0，计算探测脚步的步长和方向。 */
			if((condition.get(i) != condition.get(j)) && j > W && j < swSize - W) {
				if(numOne > BLOCKSIZE && numZero > BLOCKSIZE) {
					
					STEPDETECTED = true;
					stepCount++;
					float meanA = StepDetectionUtil.getMean(localMeanAccel, j, W);
					
					if(!mIndoorTrackSettings.getStepLengthMode() == FIXED_STEP_LENGTH)
						{
						stepLength = StepDetectionUtil.getSL(0.33f, meanA);
						}
					else stepLength = mIndoorTrackSettings.getStepLength() / 100f;
					
					double meanOrientation = 0;
					meanOrientation = StepDetectionUtil.getMeanOrientation(numOne, numZero, j, 
							orientationList, mIndoorTrackSettings.getPhonePosition(), mIndoorTrackSettings.getAlgorithms());
					st.trigger(stepCount, (float) stepLength, gyroOrientation);
					
					priOrientation = meanOrientation;
					System.out.println(priOrientation);
					
					saveToDb(meanOrientation);
					numOne = 0;
					numZero = 0;
				}
			}
		}
		
	}
	
	/**
	 * HDE校正过程
	 */
	public void HDEComp() {
		if(stepCount < 2) {
			matrix[0] = 1.0f; matrix[1] = 0.0f; matrix[2] = 0.0f;
			matrix[3] = 0.0f; matrix[4] = 1.0f; matrix[5] = 0.0f;
			matrix[6] = 0.0f; matrix[7] = 0.0f; matrix[8] = 1.0f;
			IC = -0.0006f;
		}
		else IC = -0.0001f;
		
		E = (float) (DELTA / 2 - priOrientation % DELTA);
		IController += StepDetectionUtil.getSign(E) * IC;
		if(STEPDETECTED) {
			if(SIGN != StepDetectionUtil.getSign(E)) IController = 0;
			if(mIndoorTrackSettings.getPhonePosition() == HAND_HELD) {
				gyroOrientation[0] = gyroOrientation[0] + IController;
			}
			else gyroOrientation[2] = gyroOrientation[2] + IController;
			matrix = StepDetectionUtil.getRotationMatrixFromOrientation(gyroOrientation[0], gyroOrientation[1], gyroOrientation[2]);
			STEPDETECTED = false;
			SIGN = StepDetectionUtil.getSign(E);
		}
	}
	
	/**
     * gyroFunction将陀螺仪数据积分，获取方向数据，
     * 并将方向数据写入orientation
     * @param event 传感器事件
     */
    public void gyroFunction(SensorEvent event) {
    	float[] deltaVector = new float[4];
        if(timestamp != 0) {
			final float dT = (event.timestamp - timestamp) * NS2S;
			System.arraycopy(event.values, 0, gyro, 0, 3);
			getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }
        
        timestamp = event.timestamp;
        
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
        
		matrix = StepDetectionUtil.matrixMultiplication(matrix, deltaMatrix);
		SensorManager.getOrientation(matrix, gyroOrientation);
		
		if(mIndoorTrackSettings.getAlgorithms() == HDE_BASED_ALGORITHM || mIndoorTrackSettings.getAlgorithms() == PSP_BASED_ALGORITHM) {
			HDEComp(); // HDE校正过程
		}
		
		float[] tempAccel = new float[3];
		System.arraycopy(accel, 0, tempAccel, 0, 3);
		accelList.add(tempAccel);
		
		float[] tempOrientation = new float[3];
		System.arraycopy(orientation, 0, tempOrientation, 0, 3);
		orientationList.add(tempOrientation);
		
		float[] tempGyroOrientation = new float[3];
		System.arraycopy(gyroOrientation, 0, tempGyroOrientation, 0, 3);
		gyroOrientationList.add(tempGyroOrientation);
		
		if(gyroOrientationList.size() > swSize) {
			if (mIndoorTrackSettings.getAlgorithms() != MAGNETIC_BASED_ALGORITHM) {
				checkForStep(gyroOrientationList);
			}
			else {
				checkForStep(orientationList);
			}
			for(int i = 0; i < swSize - 35; i++) {
				accelList.remove(0);
				orientationList.remove(0);
				gyroOrientationList.remove(0);
			}
		}
		
    }
    
    private void getRotationVectorFromGyro(float[] gyroValues,
    		float[] deltaRotationVector,
    		float timeFactor) {
    	
    	float[] normValues = new float[3];
    	
    	// Calculate the angular speed of the sample
    	float omegaMagnitude = 
    			(float) Math.sqrt(gyroValues[0] * gyroValues[0] +
    					gyroValues[1] * gyroValues[1] +
    					gyroValues[2] * gyroValues[2]);
    	
    	// Normalize the rotation vector if it's big enough to get the axis
    	if(omegaMagnitude > EPSILON) {
    		normValues[0] = gyroValues[0] / omegaMagnitude;
    		normValues[1] = gyroValues[1] / omegaMagnitude;
    		normValues[2] = gyroValues[2] / omegaMagnitude;
    	}
    	
    	float thetaOvetTwo = omegaMagnitude * timeFactor;
    	float sinThetaOverTwo = (float) Math.sin(thetaOvetTwo);
    	float cosThetaOverTwo = (float) Math.cos(thetaOvetTwo);
    	deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
    	deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
    	deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
    	deltaRotationVector[3] = cosThetaOverTwo;
    }
    
    /**
     * 将脚步数据保存到数据库中
     */
    private void saveToDb(double bearing) {
    	Cursor c = db.query(TBL_NAME, null, null, null, null, null, null);
		
		double newlat = 0;
		double newlng = 0;
		if(c != null) {
			if(c.getCount() == 0) {
				newlat = StepDetectionUtil.getPoint(lat, lng, bearing, (double) stepLength)[0];
				newlng = StepDetectionUtil.getPoint(lat, lng, bearing, (double) stepLength)[1];
			}
			if(c.getCount() >=1) {
				c.moveToLast();
				newlat = StepDetectionUtil.getPoint(c.getDouble(2), c.getDouble(3), bearing, (double) stepLength)[0];
				newlng = StepDetectionUtil.getPoint(c.getDouble(2), c.getDouble(3), bearing, (double) stepLength)[1];
			}
		}
		
		ContentValues values = new ContentValues();
		values.put("length", stepLength);
		values.put("lat", newlat);
		values.put("lng", newlng);
		db.insert(TBL_NAME, null, values);
    }
    
}
