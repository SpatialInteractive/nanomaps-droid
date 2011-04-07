package net.rcode.nanomaps.sample;

import net.rcode.nanomaps.Coordinate;
import net.rcode.nanomaps.MapSurface;
import net.rcode.nanomaps.MapLayer.LayoutParams;
import net.rcode.nanomaps.overlay.CircleOverlay;
import net.rcode.nanomaps.overlay.UncertaintyHeadingOverlay;
import net.rcode.nanomaps.sample.widgets.MapControls;
import net.rcode.nanomaps.tile.MapTileView;
import net.rcode.nanomaps.tile.UriTileSelector;
import net.rcode.nanomaps.util.Constants;
import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class SampleActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //MapTileView mtv=new MapTileView(this, new UriTileSelector("http://h0.ortho.tiles.virtualearth.net/tiles/h${quadkey}.jpeg?g=131"));

        // Parent container
        FrameLayout frame=new FrameLayout(this);
        
        // Construct the map
        final MapSurface map=new MapSurface(this);
        frame.addView(map, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        map.setMapLocationZoom(Coordinate.latLng(47.616159, -122.320944), 15);
        
        // Add tile layer using MapQuest OSM Tiles
        MapTileView mtv=new MapTileView(this, new UriTileSelector("http://otile${modulo:1}.mqcdn.com/tiles/1.0.0/osm/${level}/${tileX}/${tileY}.png"));
        map.getLayer(MapSurface.LAYER_MAP).addView(mtv);
        
        // Add a shiny pink marker anchored at its bottom center
        ImageView marker1=new ImageView(this);
        marker1.setImageResource(R.drawable.pin_pink);
        map.addOverlay(marker1, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.61402, -122.318457),
        				MapSurface.BIAS_CENTER, MapSurface.BIAS_BOTTOM);
        attachClickEvents(map, marker1);
        
        // Add a shiny blue orb marker anchored at its center
        ImageView marker2=new ImageView(this);
        marker2.setImageResource(R.drawable.orb_blue);
        map.addOverlay(marker2, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.616523, -122.318407),
        				MapSurface.BIAS_CENTER, MapSurface.BIAS_CENTER);
        
        // Add an "uncertainty" circle with a radius of 60m
        CircleOverlay cov=new CircleOverlay(this);
        cov.setRadius(60);
        map.addOverlay(cov, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.615016, -122.316574));
        
        // Add an "uncertainty circle plus heading cone" with radius
        // of 120m, cone overshoot of 10px, heading of 45 degrees and uncertainty of 20 degrees
        UncertaintyHeadingOverlay ohv=new UncertaintyHeadingOverlay(this);
        ohv.setRadius(120);
        ohv.setHeadingConeOvershoot(10);
        ohv.setHeading(45, 20);
        map.addOverlay(ohv, MapSurface.LAYER_OVERLAY, Coordinate.latLng(47.619095, -122.321499));
        attachChangeHeadingEvents(ohv);
        
        
        // Add zoom control
        MapControls mapControls=new MapControls(map);
        RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        map.addView(mapControls.createZoomControl(R.layout.nmsmallzoom), lp);
        
        LinearLayout topBar=new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        lp=new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        map.addView(topBar, lp);
        
        Button btn;
        btn=new Button(this);
        btn.setText("Move NG");
        btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        map.transitionMapLocationZoom(Coordinate.latLng(47.720232, -122.313698), 16);
			}
		});
        topBar.addView(btn);

        btn=new Button(this);
        btn.setText("Move BB");
        btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        map.transitionMapLocationZoom(Coordinate.latLng(47.628393, -122.521563), 12);
			}
		});
        topBar.addView(btn);
        
        setContentView(frame);
        
        Log.d(Constants.LOG_TAG, "Inited");
    }

    private void attachChangeHeadingEvents(final UncertaintyHeadingOverlay ohv) {
    	ohv.setOnTouchListener(new View.OnTouchListener() {
    		Point anchor;
    		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action=event.getAction();
				Point xy=new Point((int) event.getX(), (int) event.getY());
				
				if (action==MotionEvent.ACTION_DOWN) {
					anchor=xy;
					v.bringToFront();
					return true;
				}
				if (action==MotionEvent.ACTION_UP) {
					return true;
				}
				
				if (action==MotionEvent.ACTION_MOVE) {
					int deltaX=xy.x - anchor.x;
					int deltaY=xy.y - anchor.y;
					anchor=xy;
					
					ohv.setHeading((ohv.getHeading()+deltaX)%360, (ohv.getHeadingUncertainty()+deltaY)%360);
					return true;
				}
				return true;
			}
    	});
	}

	private void attachClickEvents(final MapSurface map, View v) {
    	v.setOnTouchListener(new View.OnTouchListener() {
    		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action=event.getAction();
				Point xy=new Point((int) event.getX(), (int) event.getY());
				map.translateChildToMap(v, xy);
				
				if (action==MotionEvent.ACTION_DOWN) {
					v.bringToFront();
					return true;
				}
				if (action==MotionEvent.ACTION_UP) {
					return true;
				}
				
				if (action==MotionEvent.ACTION_MOVE) {
					Coordinate location=map.getMapLocation(xy.x, xy.y);
					Log.d(Constants.LOG_TAG, "Moving to " + location + " for (" + xy.x + "," + xy.y + ")");

					/*
					MapLayer.LayoutParams lp=(LayoutParams) v.getLayoutParams();
					v.setLayoutParams(lp.modify(location));
					map.invalidate();
					*/
					map.setOverlayPosition(v, location);
					return true;
				}
				return true;
			}
		});
		
	}
}