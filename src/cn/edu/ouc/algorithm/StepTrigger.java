package cn.edu.ouc.algorithm;

public interface StepTrigger {
	/**
	 * 每当探测到脚步时出发trigger
	 * 
	 * @param strideLength 步长
	 * @param orientation 方向
	 */
	public void trigger(int stepCount, float strideLength, float[] orientation);
}
