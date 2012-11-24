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
	
	/**
	 * 矩阵相乘
	 * @param A 矩阵A
	 * @param B 矩阵B
	 * @return
	 */
	public static float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
	
	/**
	 * Werberg SL 步长计算算法
	 * @param k 步长计算参数，根据人体特征调节
	 * @param maxA 局部最大加速度
	 * @param minA 局部最小加速度
	 * @return
	 */
	public static float getSL(float k, float maxA, float minA)
	{
		return (float) (k * Math.pow(maxA - minA, 0.25f));
	}
	
	/**
	 * 平滑步长计算算法
	 * @param k 步长计算参数，根据人体特征调节
	 * @param meanA 局部平均加速度
	 * @return
	 */
	public static float getSL(float k, float meanA) {
		return (float) (k * Math.pow(meanA, 1/3f));
	}
	
	/**
	 * 获取当前采样点j与周围-w ~ +w 个采样点中最大的加速度
	 * @param magAccel 合加速度
	 * @param j 当前采样点指针
	 * @param w 
	 * @return
	 */
	public static float getMax(float[] magAccel, int j, int w)
	{
		float a = magAccel[j];
		float b = 0;
		float maxA = 0;
		for(int k = -w; k < w; k++) {						
			b = magAccel[j+k];
			maxA = a > b ? a : b;
		}
		return maxA;
	}
	
	/**
	 * 获取当前采样点j与周围-w ~ +w 个采样点中最小的加速度
	 * @param magAccel 合加速度
	 * @param j 当前采样点指针
	 * @param w
	 * @return
	 */
	public static float getMin(float [] magAccel, int j, int w)
	{
		float a = magAccel[j];
		float b = 0;
		float minA = 0;
		for(int k = -w; k < w; k++) {						
			b = magAccel[j+k];
			minA = a < b ? a : b;
		}
		return minA;
	}
	
	/**
	 * 获取当前采样点j与周围-w ~ +w 个采样点的平均加速度
	 * @param magAccel 合加速度
	 * @param j 当前采样点指针
	 * @param w
	 * @return
	 */
	public static float getMean(float[] magAccel, int j, int w)
	{
		float sum = 0;
		for(int i = -w; i < w; i++) {
			sum += magAccel[j + i];
		}
		return sum / (2 * w);
	}
	
}
