/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.algorithm;

/**
 * 接口，实现回调功能
 * 
 * @author Chu Hongwei, Hong Feng
 */
public interface StepTrigger {
	/**
	 * 每当探测到脚步时出发trigger
	 * 
	 * @param strideLength 步长
	 * @param orientation 方向
	 */
	public void trigger(int stepCount, float strideLength, float[] orientation);
}
