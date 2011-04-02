package net.rcode.nanomaps.sample;

import net.rcode.nanomaps.MapSurface;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class SampleActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout rl=new RelativeLayout(this);
        rl.setBackgroundColor(Color.GREEN);
        
        MapSurface map=new MapSurface(this);
        rl.addView(map, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        setContentView(rl);
    }
}