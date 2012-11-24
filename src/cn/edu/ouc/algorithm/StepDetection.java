package cn.edu.ouc.algorithm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import cn.edu.ouc.util.StepDetectionUtil;

/**
 * StepDetection类用于脚步探测.
 * 当探测到脚步时，调用回调函数StepTrigger,并将步长和方向当作参数传递.
 * @author will
 *
 */
public class StepDetection {

	private static final String TAG = "StepDetection";
	
	private StepTrigger st; // 使用接口StepTrigger向外部组件通知脚步探测情况
	
	@SuppressWarnings("unused")
	private Context context; // 通过Context访问传感器服务
	
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
	// 滑动窗口指针，指示存储位置。
	// 指针从2 * W处开始，前2 * W作为滑动窗口的缓冲使用。
	private int swPointer = 2 * W;
	private static final int BLOCKSIZE = 8; // 连续1或连续0的阈值
	private boolean firstStart = true;
	private int stepCount;
	
	/* ----------------------------------------------*/
	// 用于gyroFunction方法的参数
	public static final float EPSILON = 0.000000001f;
	private float timestamp;
	float dT = 0;
	public float axisX = 0;
	public float axisY = 0;
	public float axisZ = 0;
    private static final float NS2S = 1.0f / 1000000000.0f; // 纳秒到秒的转换
    private final float[] deltaRotationVector = new float[4];
    public float omegaMagnitude = 0;
    public float[] matrix = new float[9]; // 旋转矩阵
    private float[] orientation = new float[3]; // 方向角
    private float[][] slide_windows_ori; //滑动窗口，用于存储方向
	
	/**
	 * 构造函数
	 * @param context
	 * @param stepTrigger 接口，用于实现回调
	 * @param swSize 滑动窗口大小
	 */
	public StepDetection(Context context, StepTrigger stepTrigger, int swSize) {
		this.context = context;
		this.st = stepTrigger;
		this.swSize = swSize;
		this.slide_windows_acc = new float[swSize];
		this.localMeanAccel = new float[swSize];
		this.slide_windows_ori = new float[swSize][3];
		stepCount = 0;
		
		matrix[0] = 1.0f; matrix[1] = 0.0f; matrix[2] = 0.0f;
		matrix[3] = 1.0f; matrix[4] = 1.0f; matrix[5] = 0.0f;
		matrix[6] = 1.0f; matrix[7] = 0.0f; matrix[8] = 1.0f;
		
		orientation[0] = 0.0f; orientation[1] = 0.0f; orientation[2] = 0.0f;
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
		        if(swPointer > swSize - 1) { // 如果指针位置超过窗口大小，则将指针移到距离窗口起始位置2 * W处
		        	swPointer = (swPointer % (swSize - 1)) + 2 * W; // 窗口的前2 * W个位置作为缓冲使用
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
		//accelVariance = StepDetectionUtil.getAccelVariance(slide_windows_acc, localMeanAccel, W);
		float threshold = StepDetectionUtil.getAverageLocalMeanAccel(localMeanAccel) + 0.5f;
		condition = StepDetectionUtil.getCondition(localMeanAccel, threshold);
		
		int numOne = 0; // 记录判断条件condition中，连续1的个数
		int numZero = 0; // 记录判断条件condition中，连续0的个数
		boolean flag = false; // 记录当前点是1还是0
		
		// 通过数连续1和连续0的个数判断脚步
		for(int i = 0, j = 1; i < swSize - 1 && j < swSize - W; i++, j++) {
			if(firstStart) {
				i = 2 * W;
				j = i + 1;
			}
			firstStart = false;
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
					stepCount++;
					st.trigger(stepCount, 0, slide_windows_ori[swPointer]);
				}
			}
		}
		
		/* 将数组中的最后2 * W个采样点放置到数组的前2 * W位置中，
		 * 模拟循环队列。
		*/
		for(int k = 0; k < 2 * W; k++) {
			slide_windows_acc[k] = slide_windows_acc[k + swSize - 2 * W];
		}
	}
	
	/**
     * gyroFunction将陀螺仪数据积分，获取方向数据，
     * 并将方向数据写入orientation
     * @param event 传感器事件
     */
    public void gyroFunction(SensorEvent event) {
        if(timestamp != 0) {
			dT = (event.timestamp - timestamp) * NS2S;
			axisX = event.values[0];
			axisY = event.values[1];
			axisZ = event.values[2];
			
			omegaMagnitude = (float) Math.sqrt(axisX * axisX + 
					axisY * axisY + axisZ * axisZ);
			if(omegaMagnitude > EPSILON) 
			{	
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}
			
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;
			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
			deltaRotationVector[0] = sinThetaOverTwo * axisX;
			deltaRotationVector[1] = sinThetaOverTwo * axisY;
			deltaRotationVector[2] = sinThetaOverTwo * axisZ;
			deltaRotationVector[3] = cosThetaOverTwo;
		}
		timestamp = event.timestamp;
		float[] deltaRotationMatrix = new float[9];
		SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, 
				deltaRotationVector);
		
		matrix = StepDetectionUtil.matrixMultiplication(matrix, deltaRotationMatrix);
		SensorManager.getOrientation(matrix, orientation);
		slide_windows_ori[swPointer] = orientation;
    }
	
}
