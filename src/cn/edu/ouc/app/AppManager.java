package cn.edu.ouc.app;

import java.util.Stack;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

/**
 * 应用程序Activity管理类：用于Activity管理和应用程序退出
 * @author will
 *
 */
public class AppManager {

	private static Stack<Activity> activityStack;
	private static AppManager instance;
	
	private AppManager() {}
	
	/**
	 * 单例
	 * @return
	 */
	public static AppManager getAppManager() {
		if(instance == null) {
			instance = new AppManager();
		}
		return instance;
	}
	
	/**
	 * 添加Activity到堆栈
	 * @param activity
	 */
	public void addActivity(Activity activity) {
		if(activityStack == null) {
			activityStack = new Stack<Activity>();
		}
		activityStack.add(activity);
	}
	
	/**
	 * 获取当前Activity(堆栈中最后一个压入的)
	 * @return
	 */
	public Activity currentActivity() {
		Activity activity = activityStack.lastElement();
		return activity;
	}
	
	/**
	 * 结束当前Activity(堆栈中最后一个压入的)
	 */
	public void finishActivity() {
		Activity activity = activityStack.lastElement();
		finishActivity(activity);
	}
	
	/**
	 * 结束指定的Activity
	 * @param activity
	 */
	public void finishActivity(Activity activity) {
		if(activity != null) {
			activityStack.remove(activity);
			activity.finish();
			activity = null;
		}
	}
	
	/**
	 * 结束指定类名的Activity
	 * @param cls
	 */
	public void finishActivity(Class<?> cls) {
		for(Activity activity : activityStack) {
			if(activity.getClass().equals(cls)) {
				finishActivity(activity);
			}
		}
	}
	
	/**
	 * 结束所有Activity
	 */
	public void finishAllActivity() {
		for(int i = 0, size = activityStack.size(); i < size; i++) {
			if(null != activityStack.get(i)) {
				activityStack.get(i).finish();
			}
		}
		activityStack.clear();
	}
	
	/**
	 * 退出应用程序
	 * @param context
	 */
	public void AppExit(Context context) {
		try {
			finishAllActivity();
			ActivityManager activityMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			activityMgr.restartPackage(context.getPackageName());
			System.exit(0);
		} catch (Exception e) {}
	}
}
