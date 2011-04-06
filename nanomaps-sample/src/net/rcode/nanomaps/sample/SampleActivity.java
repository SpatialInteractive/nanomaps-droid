package net.rcode.nanomaps.sample;

import net.rcode.nanomaps.MapState;
import net.rcode.nanomaps.MapSurface;
import net.rcode.nanomaps.MapTileView;
import net.rcode.nanomaps.UriTileSelector;
import net.rcode.nanomaps.sample.widgets.MapControls;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class SampleActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout rl=new RelativeLayout(this);
        rl.setBackgroundColor(Color.GREEN);
        
        final MapSurface map=new MapSurface(this);
        rl.addView(map, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        MapState ms=map.getMapState();
        ms.lock();
        ms.setLevel(14, 0, 0);
        ms.setCenterLatLng(47.626967, -122.315352);
        ms.unlock();
        
        MapTileView mtv=new MapTileView(this, new UriTileSelector("http://otile${modulo:1}.mqcdn.com/tiles/1.0.0/osm/${level}/${tileX}/${tileY}.png"));
        //MapTileView mtv=new MapTileView(this, new UriTileSelector("http://h0.ortho.tiles.virtualearth.net/tiles/h${quadkey}.jpeg?g=131"));
        map.getBackgroundLayer().addView(mtv);
        
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
				map.mapPanZoom(47.720232, -122.313698, 16);
			}
		});
        topBar.addView(btn);

        btn=new Button(this);
        btn.setText("Move BB");
        btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.mapPanZoom(47.628393, -122.521563, 12);
			}
		});
        topBar.addView(btn);
        
        setContentView(rl);
    }
}