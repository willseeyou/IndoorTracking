package cn.edu.ouc.algorithm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import cn.edu.ouc.util.StepDetectionUtil;

/**
 * StepDetection类用于脚步探测.
 * 当探测到脚步时，调用回调函数StepTrigger,并将步长和方向当作参数传递.
 * @author will
 *
 */
public class StepDetection {

	private static final String TAG = "StepDetection";
	
	// 使用接口StepTrigger向外部组件通知脚步探测情况
	private StepTrigger st;
	
	// 通过Context访问传感器服务
	private Context context;
	
	private static SensorManager mSensorManager;
	
	// 加速度矢量
	private float[] accel = new float[3];
	
	// 局部平均加速度
	private float[] localMeanAccel;
	
	// 加速度方差
	private float[] accelVariance;
	
	// 判断条件，用于进行脚步探测
	private int[] condition;
	
	// 陀螺仪矢量
	private float[] gyro = new float[3];
	
	// 方向矢量
	private float[] orient = new float[3];
	
	// 滑动窗口大小
	private int swSize;
	
	// 滑动窗口,用于存储合加速度
	private float[] slide_windows;
	
	// 局部窗口大小，用于计算局部平均加速度和方差
	private static final int W = 15;
	
	// 滑动窗口指针，指示存储位置。
	// 指针从2 * W处开始，前2 * W作为滑动窗口的缓冲使用。
	private int swPointer = 2 * W;
	
	// 连续1或连续0的阈值
	private static final int BLOCKSIZE = 8;
	
	private int stepCount = 0;
	
	private boolean firstStart = true;
	
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
		this.slide_windows = new float[swSize];
		this.localMeanAccel = new float[swSize];
		this.accelVariance = new float[swSize];
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
		        slide_windows[swPointer % (swSize - 1)] = StepDetectionUtil.getMagnitudeOfAccel(accel);
		        if((swPointer == swSize - 1)) {
		        	checkForStep(); // 开始脚步探测
		        }
		        swPointer++;
		        if(swPointer > swSize - 1) {
		        	swPointer = (swPointer % (swSize - 1)) + 2 * W;
		        }
		        swPointer = swPointer % swSize;
		        break;
		 
		    case Sensor.TYPE_GYROSCOPE:
		        break;
		
		    case Sensor.TYPE_ORIENTATION:
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
		mSensorManager.unregisterListener(mSensorEventListener);
	}
	
	/**
	 * 脚步探测算法，利用行走的加速度特征判断脚步
	 */
	private void checkForStep() {
		localMeanAccel = StepDetectionUtil.getLocalMeanAccel(slide_windows, W);
		//accelVariance = StepDetectionUtil.getAccelVariance(slide_windows, localMeanAccel, W);
		float threshold = StepDetectionUtil.getAverageLocalMeanAccel(localMeanAccel) + 0.5f;
		condition = StepDetectionUtil.getCondition(localMeanAccel, threshold);
		
		int numOne = 0; // 记录判断条件condition中，连续1的个数
		int numZero = 0; // 记录判断条件condition中，连续0的个数
		boolean flag = false; // 记录当前点是1还是0
		
		// 通过数连续1和连续0的个数判断脚步
		for(int i = 0, j = 1; i < swSize - 1 && j < swSize -W; i++, j++) {
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
			/* 忽略前W个和后W个采样点，如果前一个采样点i不等于当前采样点j的值，
			 * 并且连续1和连续0的个数均大于BLOCKSIZE，则探测到脚步，
			 * 将numOne和numZero置0，计算探测脚步的步长和方向。 */
			if((condition[i] != condition[j]) && j > W && j < swSize - W) {
				if(numOne > BLOCKSIZE && numZero > BLOCKSIZE) {
					numOne = 0;
					numZero = 0;
					st.trigger(slide_windows[i], 0);
				}
			}
		}
		
		/* 将数组中的最后2 * W个采样点放置到数组的前2 * W位置中，
		 * 模拟循环队列。
		*/
		for(int k = 0; k < 2 * W; k++) {
			slide_windows[k] = slide_windows[k + swSize - 2 * W];
		}
	}
	
}
