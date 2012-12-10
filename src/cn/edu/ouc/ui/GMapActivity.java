/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.ui;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import cn.edu.ouc.R;
import cn.edu.ouc.db.DatabaseHelper;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * 地图显示类，显示用户行走路线
 * 
 * @author Chu Hongwei, Hong Feng
 */
public class GMapActivity extends MapActivity {

	private MapView mMapView;  
	private MapController mMapController;  
	private GeoPoint centerPoint;
	private List<GeoPoint> geoPointList;
	MyLocationOverlay myLocationOverlay;
	
	//MyTouchListener myTouchListener;
	Path path;
	Bitmap bitmap;
	Bitmap bitmapDes;
	DatabaseHelper mHelper;
	SQLiteDatabase db;
	private static final String TBL_NAME = "track_tbl";
	Cursor c;
	//SharedPreferences sharedPreferences;
	@Override
	protected void onCreate(Bundle icicle) {
		// TODO Auto-generated method stub
		super.onCreate(icicle);
		setContentView(R.layout.map_layout);		
		mHelper = new DatabaseHelper(getApplicationContext());
		db = mHelper.getReadableDatabase();
		mMapView = (MapView) findViewById(R.id.MapView01);  
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
		bitmapDes = BitmapFactory.decodeResource(getResources(), R.drawable.markera);
        mMapView.setSatellite(true);   
        //myTouchListener = new MyTouchListener();
        mMapController = mMapView.getController();   
        mMapView.setEnabled(true);  
        mMapView.setClickable(true);  
        mMapView.setBuiltInZoomControls(true);    
        /*sharedPreferences = getSharedPreferences("LatLng", Context.MODE_PRIVATE);
        double lat = Double.parseDouble(sharedPreferences.getString("lat", "36.16010"));
        double lng = Double.parseDouble(sharedPreferences.getString("lng", "120.491951"));*/
        centerPoint=new GeoPoint((int)(36.16010 * 1E6),(int)(120.491951* 1E6));
        mMapController.animateTo(centerPoint);   
        c = query();
        mMapController.setZoom(21);   
        myLocationOverlay = new MyLocationOverlay();  
         List<Overlay> list = mMapView.getOverlays();  
         list.clear();
         list.add(myLocationOverlay);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		db.close();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	public Cursor query() {
		c = db.query(TBL_NAME, null, null, null, null, null, null);
		return c;
	}
	
	class MyLocationOverlay extends Overlay  
	{  
	    
		public MyLocationOverlay() {
			super();
			geoPointList = new ArrayList<GeoPoint>();
		}

		public boolean draw(Canvas canvas,MapView mapView,boolean shadow,long when)  
	    {  
	        super.draw(canvas, mapView, shadow);
	        path = new Path();
	        Paint paint = new Paint();  
	        Point myScreenCoords = new Point();	     
	        Point curCoords = myScreenCoords;
	        mapView.getProjection().toPixels(centerPoint,myScreenCoords);
	        paint.setStrokeWidth(5);  
	        paint.setARGB(255, 0, 0, 255);  
	        paint.setStyle(Paint.Style.STROKE);  
	        paint.setAntiAlias(true);
	        paint.setStrokeCap(Paint.Cap.ROUND);
	        canvas.drawBitmap(bitmap, myScreenCoords.x, myScreenCoords.y, paint);
	        c = query();
	        if(c != null){
			while(c.moveToNext()) {
				GeoPoint temPoint = new GeoPoint((int)(c.getFloat(5) * 1E6),
						(int)(c.getFloat(6) * 1E6));
				geoPointList.add(temPoint);
			}
			c.close();
	        if(geoPointList != null) 
	        {
	        for(int i = 0; i < geoPointList.size()-1; i++)
	        	{
	        	Point first = new Point();
	        	mapView.getProjection().toPixels(geoPointList.get(i), first);
	        	Point second = new Point();
	        	mapView.getProjection().toPixels(geoPointList.get(i+1), second);
	        	path.moveTo(first.x, first.y);
	        	path.lineTo(second.x, second.y);
	        	curCoords = second;
	        	}        
	        canvas.drawPath(path, paint);
	        canvas.drawBitmap(bitmapDes, curCoords.x, curCoords.y, paint);
	        geoPointList.clear();
	        }
	        }
	        return true;  
	    }  
	}
	
	/*class MyTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			GeoPoint point = mMapView.getProjection().fromPixels((int)event.getX(), 
					(int)event.getY());
			centerPoint = point;
			double lat = point.getLatitudeE6();
			double lng = point.getLongitudeE6();
			Editor editor = sharedPreferences.edit();
			editor.putString("lat", ((double)lat/1000000)+"");
			editor.putString("lng", ((double)lng/1000000)+"");
			editor.commit();
			return false;
		}
		
	}*/
	
	/*@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.startpoint:
	    	mMapView.setOnTouchListener(myTouchListener);
	        return true;
	    case R.id.quit:	    	
			finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}*/

}
