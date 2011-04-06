package net.rcode.nanomaps.sample.widgets;

import net.rcode.nanomaps.MapSurface;
import net.rcode.nanomaps.sample.R;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Helper for instantiating map controls
 * @author stella
 *
 */
public class MapControls {
	MapSurface map;
	Context context;

	public MapControls(MapSurface map) {
		this.map=map;
		this.context=map.getContext();
	}
	
	public View createZoomControl(int layoutResource) {
		FrameLayout root=new FrameLayout(context);
		View control=LayoutInflater.from(context).inflate(layoutResource, root);
		configureZoomControl(control);
		return root;
	}
	
	/**
	 * Given a view, hook it up with events so that it controls the
	 * zoom settings of the map.
	 * @param control
	 */
	public void configureZoomControl(View control) {
		View btn;
		
		// Zoom in button
		btn=control.findViewById(R.id.nmbtnzoomin);
		if (btn!=null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d("nanomaps", "Zoom In");
					double current=map.getMapState().getLevel();
					double next=Math.floor(current)+1;
					map.mapZoom(next, false);
				}
			});
		}
		
		// Zoom out button
		btn=control.findViewById(R.id.nmbtnzoomout);
		if (btn!=null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d("nanomaps", "Zoom Out");
					double current=map.getMapState().getLevel();
					double next=Math.ceil(current)-1;
					map.mapZoom(next, false);
				}
			});
		}
	}
}
