/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.preferences;

import android.content.SharedPreferences;

/**
 * 参数设置类
 * 
 * @author Chu Hongwei, Hong Feng
 */
public class IndoorTrackSettings {
	
	SharedPreferences mSettings;
	
	public IndoorTrackSettings(SharedPreferences settings) {
		mSettings = settings;
	}
	
	public float getStepLength() {
        try {
            return Float.valueOf(mSettings.getString("step_length", "65").trim());
        }
        catch (NumberFormatException e) {
            return 0f;
        }
    }
    
    public float getBodyWeight() {
        try {
            return Float.valueOf(mSettings.getString("body_weight", "65").trim());
        }
        catch (NumberFormatException e) {
            return 0f;
        }
    }
    
    public int getAlgorithms() {
    	try {
    		return Integer.valueOf(mSettings.getString("track_algorithm", "3").trim());
    	} catch (NumberFormatException e) {
    		return 0;
    	}
    }
    
    public int getSensitivity() {
    	try {
    		return Integer.valueOf(mSettings.getString("sensitivity", "300").trim());
    	} catch (NumberFormatException e) {
    		return 0;
    	}
    }
    
    public int getPhonePosition() {
    	try {
    		return Integer.valueOf(mSettings.getString("phone_position", "1").trim());
    	} catch (NumberFormatException e) {
    		return 0;
    	}
    }
    
    public boolean getStepLengthMode() {
    	try {
    		return Boolean.valueOf(mSettings.getBoolean("fixed_step_length", false));
    	} catch (NumberFormatException e) {
    		return false;
    	}
    }
    
}
