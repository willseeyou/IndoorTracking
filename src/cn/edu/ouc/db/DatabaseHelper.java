/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Êý¾Ý¿â¸¨ÖúÀà
 * @author Chu Hongwei, Hong Feng
 */
public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final int VERSION = 1;
	// Columns
	public static final String NUM = "num";
	public static final String LENGTH = "length";
	public static final String LAT = "lat";
	public static final String LNG = "lng";
	
	private static final String DB_NAME = "indoortracking.db";
	private static final String TBL_NAME = "track_tbl " +
			"(" + NUM + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			LENGTH + " FLOAT, " +
			LAT + " FLOAT, " +
			LNG + " FLOAT)";
	private static final String CREATE_TBL_SQL = "create table " + TBL_NAME;
	
	private static final String TAG = "DatabaseHelper";

	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}
	
	public DatabaseHelper(Context context) {
		this(context, DB_NAME, null, VERSION);
	}
	
	public DatabaseHelper(Context context, String name) {
		this(context, name, VERSION);
	}
	
	public DatabaseHelper(Context context, String name, int version) {
		this(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(CREATE_TBL_SQL);
		Log.i(TAG, "DatabaseHelper onCreate()......");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
