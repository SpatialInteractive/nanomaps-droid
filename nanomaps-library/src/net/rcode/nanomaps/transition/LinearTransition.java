package net.rcode.nanomaps.transition;

import net.rcode.nanomaps.MapState;
import net.rcode.nanomaps.util.Constants;
import android.util.Log;

/**
 * Performs a linear animation between all components
 * @author stella
 *
 */
public class LinearTransition extends Transition {
	private double projectedXStart, projectedXDelta;
	private double projectedYStart, projectedYDelta;
	private double resolutionStart, resolutionDelta;
	
	public LinearTransition(MapState activeMapState) {
		super(activeMapState);
	}
	
	@Override
	public void onStart() {
		projectedXStart=getInitialMapState().getViewportProjectedX(0, 0);
		projectedYStart=getInitialMapState().getViewportProjectedY(0, 0);
		projectedXDelta=getFinalMapState().getViewportProjectedX(0, 0) - projectedXStart;
		projectedYDelta=getFinalMapState().getViewportProjectedY(0, 0) - projectedYStart;
		resolutionStart=getInitialMapState().getResolution();
		resolutionDelta=getFinalMapState().getResolution() - resolutionStart;
	}
	
	@Override
	public void onFrame(float factor) {
		MapState ms=getActiveMapState();
		double resolution=resolutionStart+factor*resolutionDelta;
		double projectedX=projectedXStart+factor*projectedXDelta;
		double projectedY=projectedYStart+factor*projectedYDelta;
		
		Log.d(Constants.LOG_TAG, "Transition frame: factor=" + factor + ", resolution=" + resolution + ", x=" + projectedX + ", y=" + projectedY);
		ms.lock();
		ms.setResolution(resolution, 0, 0);
		ms.setViewportProjected(
				projectedX,
				projectedY,
				0,0);
		ms.unlock();
	}
}
