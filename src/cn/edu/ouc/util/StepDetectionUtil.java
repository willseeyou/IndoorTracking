package cn.edu.ouc.util;


public class StepDetectionUtil {
	
	/**
	 * 计算合加速度
	 * @param accel 三轴加速度
	 * @return
	 */
	public static float getMagnitudeOfAccel(float[] accel) {
		return (float) Math.sqrt(accel[0] * accel[0] + 
				accel[1] * accel[1] + 
				accel[2] * accel[2]);
	}
	
	/**
	 * 计算局部平均加速度
	 * @param magAccel 合加速度
	 * @param w 局部窗口大小
	 * @return
	 */
	public static float[] getLocalMeanAccel(float[] magAccel, int w) {
		int size = magAccel.length;
		float[] localMeanAccel = new float[size];
		float sum = 0;
		for(int i = 0; i < size; i++) {
			sum = magAccel[i];
			for(int j = 1; j <= w; j++) {
				int right = i + j; // 当前位置i右侧的下标
				int left = i - j; // 当前位置i左侧的下标
				if(right >= size) { // 如果右侧的下标超过窗口大小，则减掉窗口大小，回到窗口起始位置
					right = right - size;
				}
				if(left < 0) { // 如果左侧的下标小于窗口最小下标零，则加上窗口大小，回到窗口末尾位置
					left = size + left;
				}
				sum += magAccel[left] + magAccel[right];
			}
			localMeanAccel[i] = sum / ( 2 * w + 1);
		}
		return localMeanAccel;
	}
	
	/**
	 * 计算局部平均加速度的平均值，用于确定脚步判断条件的阈值
	 * @param localMeanAccel 局部平均加速度
	 * @return
	 */
	public static float getAverageLocalMeanAccel(float[] localMeanAccel) {
		float sum = 0;
		int size = localMeanAccel.length;
		for(int i = 0; i < size; i++) {
			sum += localMeanAccel[i];
		}
		return sum / size;
	}
	
	/**
	 * 计算局部加速度方差
	 * @param magAccel 合加速度
	 * @param localMeanAccel 局部平均加速度
	 * @param w 局部窗口大小
	 * @return
	 */
	public static float[] getAccelVariance(float[] magAccel, float[] localMeanAccel, int w) {
		int size = magAccel.length;
		float[] accelVariance = new float[size];
		float sum = 0;
		for(int i = 0; i < size; i++) {
			sum = (float) Math.pow(magAccel[i] - localMeanAccel[i], 2);
			for(int j = 1; j <= w; j++) {
				int right = i + j; // 当前位置i右侧的下标
				int left = i - j; // 当前位置i左侧的下标
				if(right >= size) { // 如果右侧的下标超过窗口大小，则减掉窗口大小，回到窗口起始位置
					right = right - size;
				}
				if(left < 0) { // 如果左侧的下标小于窗口最小下标零，则加上窗口大小，回到窗口末尾位置
					left = size + left;
				}
				sum +=  Math.pow(magAccel[left] - localMeanAccel[left], 2)
						+ Math.pow(magAccel[right] - localMeanAccel[right], 2);
			}
			accelVariance[i] = sum / ( 2 * w + 1);
		}
		return accelVariance;
	}
	
	/**
	 * 计算脚步判断条件
	 * 如果方差大于指定阈值，判断条件置1，否则置0
	 * @param localMeanAccel 局部平均加速度
	 * @param w 局部窗口大小
	 * @return
	 */
	public static int[] getCondition(float[] localMeanAccel, float threshold) {
		int size = localMeanAccel.length;
		int[] condition = new int[size];
		for(int i = 0; i < size; i++) {
			if(localMeanAccel[i] > threshold)
				condition[i] = 1;
			else condition[i] = 0;
		}
		return condition;
	}
	
	/**
	 * 判断data是否为1，如果为1，返回true，否则返回false
	 */
	public static boolean isOne(int data) {
		if(data == 1)
			return true;
		else return false;
	}
}
