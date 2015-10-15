/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import cn.edu.ouc.R;

/**
 * Preference Activity类，用于显示参数配置界面
 * 
 * @author Chu Hongwei, Hong Feng
 */
public class Settings extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}

}
