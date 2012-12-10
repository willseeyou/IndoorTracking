/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.algorithm;

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
	private float[] accel = new float[3]; // 加速度矢量
	private float[] localMeanAccel; // 局部平均加速度
	//private float[] accelVariance; // 加速度方差
	private int[] condition; // 判断条件，用于进行脚步探测
	private static final int W = 15; // 局部窗口大小，用于计算局部平均加速度和方差
	private int swSize; // 滑动窗口大小
	private float[] slide_windows_acc; // 滑动窗口,用于存储合加速度
	private final static int CACHE = 35; // 作为滑动窗口的缓冲使用
	// 滑动窗口指针，指示存储位置。
	// 指针从CACHE处开始，前CACHE个位置作为滑动窗口的缓冲使用
	private int swPointer = CACHE;
	private static final int BLOCKSIZE = 8; // 连续1或连续0的阈值
	private boolean firstStart = true; //判断程序是否首次运行，以便对滑动窗口的起始位置进行设定
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
    private float[][] slide_windows_ori; // 滑动窗口，用于存储陀螺仪采集的方向
    
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
	private float IC = -0.00009f; // 固定增量，表示控制器IController的补偿因子
	private static final float DELTA = 90f; // 主方向间隔角度：各主方向互相垂直
	float IController = 0; // 控制器，用来消除偏移误差
	private int SIGN = 0; // 判断行走方向偏离主方向的哪一侧，SIGN = 1偏向左侧，SIGN = 0偏向右侧
	private float priOrientation = 0f; // 前一步的方向
	private boolean STEPDETECTED = false; // 脚步探测标志
	 
	// 参数设定
	SharedPreferences mSettings;
	IndoorTrackSettings mIndoorTrackSettings;
	
	// 算法设定
	private final static int MAGNETIC_BASED_ALGORITHM = 1;
	private final static int GYROSCOPE_BASED_ALGORITHM = 2;
	private final static int HDE_BASED_ALGORITHM = 3;
	
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
		this.slide_windows_acc = new float[swSize];
		this.localMeanAccel = new float[swSize];
		this.slide_windows_ori = new float[swSize][3];
		stepCount = 0;
		mHelper = new DatabaseHelper(context);
		db = mHelper.getWritableDatabase();
		
		matrix[0] = 1.0f; matrix[1] = 0.0f; matrix[2] = 0.0f;
		matrix[3] = 0.0f; matrix[4] = 1.0f; matrix[5] = 0.0f;
		matrix[6] = 0.0f; matrix[7] = 0.0f; matrix[8] = 1.0f;
		
		gyroOrientation[0] = 0.0f; gyroOrientation[1] = 0.0f; gyroOrientation[2] = 0.0f;
		//this.accelVariance = new float[swSize];
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	/**
	 * 处理传感器事件
	 */
	public SensorEventListener mSensorEventListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			switch(event.sensor.getType()) {
		    case Sensor.TYPE_ACCELEROMETER:
		    	
		    	System.arraycopy(event.values, 0, accel, 0, 3);
		    	slide_windows_acc[swPointer % (swSize - 1)] = StepDetectionUtil.getMagnitudeOfAccel(accel);
		        if((swPointer == swSize - 1)) {
		        	checkForStep(); // 开始脚步探测
		        }
		        swPointer++;
		        if(swPointer > swSize - 1) { // 如果指针位置超过窗口大小，则将指针移到距离窗口起始位置CACHE处
		        	swPointer = (swPointer % (swSize - 1)) + CACHE; // 窗口的前CACHE个位置作为缓冲使用
		        }
		        swPointer = swPointer % swSize;
		        break;
		    case Sensor.TYPE_GYROSCOPE:
		    	gyroFunction(event); // 处理陀螺仪数据
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
	}
	
	/**
	 * 注销传感器
	 */
	public void stopSensor() {
		Log.i(TAG, "[StepDetection] stopSensor");
		mSensorManager.unregisterListener(mSensorEventListener);
	}
	
	/**
	 * 脚步探测算法，利用行走的加速度特征判断脚步
	 */
	private void checkForStep() {
		Log.i(TAG, "[StepDetection] checkForStep");
		
		localMeanAccel = StepDetectionUtil.getLocalMeanAccel(slide_windows_acc, W);
		float threshold = StepDetectionUtil.getAverageLocalMeanAccel(localMeanAccel) + 0.5f;
		condition = StepDetectionUtil.getCondition(localMeanAccel, threshold);
		
		int numOne = 0; // 记录判断条件condition中，连续1的个数
		int numZero = 0; // 记录判断条件condition中，连续0的个数
		boolean flag = false; // 记录当前点是1还是0
		
		// 通过数连续1和连续0的个数判断脚步
		for(int i = 0, j = 1; i < swSize - 1 && j < swSize - W; i++, j++) {
			if(firstStart) { // 首次运行程序时，滑动窗口的初始位置设置为CACHE，
				i = CACHE;   // 前CACHE 用作缓冲区
				j = i + 1;
			}
			firstStart = false;
			if(mIndoorTrackSettings.getAlgorithms() == HDE_BASED_ALGORITHM) {
				HDEComp(); // HDE校正过程
			}
			flag = StepDetectionUtil.isOne(condition[i]); // 判断前一个采样点的判断条件i是否为1
			/* 如果前一个采样点i的判断条件和当前采样点j的判断条件相同，
			 * 并且都等于1，则numOne加1. */
			if((condition[i] == condition[j]) && flag == true) 
			{				
				numOne++;
			}
			/* 如果前一个采样点i的判断条件和当前采样点j的判断条件相同，
			 * 并且都等于0，则numZero加1. */
			if((condition[i] == condition[j]) && flag == false) 
			{
				numZero++;	
			}
			/* 如果前一个采样点i不等于当前采样点j的值，
			 * 并且连续1和连续0的个数均大于BLOCKSIZE，则探测到脚步，
			 * 将numOne和numZero置0，计算探测脚步的步长和方向。 */
			if((condition[i] != condition[j]) && j > W && j < swSize - W) {
				if(numOne > BLOCKSIZE && numZero > BLOCKSIZE) {
					numOne = 0;
					numZero = 0;
					STEPDETECTED = true;
					stepCount++;
					float meanA = StepDetectionUtil.getMean(localMeanAccel, j, W);
					if(!mIndoorTrackSettings.getStepLengthMode() == FIXED_STEP_LENGTH)
						{
						stepLength = StepDetectionUtil.getSL(0.33f, meanA);
						}
					else stepLength = mIndoorTrackSettings.getStepLength() / 100f;
					st.trigger(stepCount, (float) stepLength, gyroOrientation);
					if(mIndoorTrackSettings.getPhonePosition() == HAND_HELD) {
					priOrientation = (float) ((gyroOrientation[0] * 180 / Math.PI + 360) % 360);
					}
					else priOrientation = (float) ((gyroOrientation[2] * 180 / Math.PI + 360) % 360);
					saveToDb();
				}
			}
		}
		
		/* 将数组中的最后CACHE个采样点放置到数组的前CACHE位置中，
		 * 模拟循环队列。
		*/
		for(int k = 0; k < CACHE; k++) {
			slide_windows_acc[k] = slide_windows_acc[k + swSize - CACHE];
		}
	}
	
	/**
	 * HDE校正过程
	 */
	public void HDEComp() {
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
		slide_windows_ori[swPointer] = gyroOrientation;
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
    private void saveToDb() {
    	Cursor c = db.query(TBL_NAME, null, null, null, null, null, null);
		
		double newlat = 0;
		double newlng = 0;
		if(c != null) {
			if(c.getCount() == 0) {
				if(mIndoorTrackSettings.getPhonePosition() == HAND_HELD) {
				newlat = StepDetectionUtil.getPoint(lat, lng, (double) (slide_windows_ori[swPointer][0] * 180/Math.PI+ 360) % 360, (double) stepLength)[0];
				newlng = StepDetectionUtil.getPoint(lat, lng, (double) (slide_windows_ori[swPointer][0] * 180/Math.PI+ 360) % 360, (double) stepLength)[1];
				}
				else {
					newlat = StepDetectionUtil.getPoint(lat, lng, (double) (slide_windows_ori[swPointer][2] * 180/Math.PI+ 360) % 360, (double) stepLength)[0];
					newlng = StepDetectionUtil.getPoint(lat, lng, (double) (slide_windows_ori[swPointer][2] * 180/Math.PI+ 360) % 360, (double) stepLength)[1];
				}
			}
			if(c.getCount() >=1) {
				c.moveToLast();
				if(mIndoorTrackSettings.getPhonePosition() == HAND_HELD) {
				newlat = StepDetectionUtil.getPoint(c.getDouble(5), c.getDouble(6), (double) (slide_windows_ori[swPointer][0] * 180/Math.PI+ 360) % 360, (double) stepLength)[0];
				newlng = StepDetectionUtil.getPoint(c.getDouble(5), c.getDouble(6), (double) (slide_windows_ori[swPointer][0] * 180/Math.PI+ 360) % 360, (double) stepLength)[1];
				}
				else {
					newlat = StepDetectionUtil.getPoint(c.getDouble(5), c.getDouble(6), (double) (slide_windows_ori[swPointer][2] * 180/Math.PI+ 360) % 360, (double) stepLength)[0];
					newlng = StepDetectionUtil.getPoint(c.getDouble(5), c.getDouble(6), (double) (slide_windows_ori[swPointer][2] * 180/Math.PI+ 360) % 360, (double) stepLength)[1];
				}
			}
		}
		
		ContentValues values = new ContentValues();
		values.put("length", stepLength);
		values.put("azimuth", (double) (slide_windows_ori[swPointer][0] * 180/Math.PI+ 360) % 360);
		values.put("pitch", (double) (slide_windows_ori[swPointer][1] * 180/Math.PI+ 360) % 360);
		values.put("roll", (double) (slide_windows_ori[swPointer][2] * 180/Math.PI+ 360) % 360);
		values.put("lat", newlat);
		values.put("lng", newlng);
		db.insert(TBL_NAME, null, values);
    }
	
}
