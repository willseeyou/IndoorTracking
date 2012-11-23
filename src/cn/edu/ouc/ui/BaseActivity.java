package cn.edu.ouc.ui;

import cn.edu.ouc.app.AppManager;
import android.app.Activity;
import android.os.Bundle;

/**
 * 应用程序Activity的基类
 * @author will
 *
 */
public class BaseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 将Activity添加到堆栈
		AppManager.getAppManager().addActivity(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// 结束Activity & 从堆中移除
		AppManager.getAppManager().finishActivity(this);
	}

}
