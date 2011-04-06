package net.rcode.nanomaps.transition;

import net.rcode.nanomaps.util.Constants;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

/**
 * Manages time based transitions of changes to mapstate.
 * 
 * @author stella
 *
 */
public class TransitionController implements Runnable {
	private static final int MIN_FRAME_DURATION=50;
	private int frameRate=10;
	private Handler handler;
	
	// -- active transition
	private Transition activeTransition;
	private long startTime;
	private long endTime;
	private int totalDuration;
	private int frameDuration;
	private int framesCompleted;
	
	
	public TransitionController() {
		this.handler=new Handler();
	}
	
	public final boolean isTransitionActive() {
		return activeTransition!=null;
	}
	
	public final Transition getActiveTransition() {
		return activeTransition;
	}
	
	/**
	 * Perform a transition immediately
	 * @param t
	 */
	public void doImmediately(Transition t) {
		finish();
		
		Log.d(Constants.LOG_TAG, "Executing transition " + t);
		
		activeTransition=t;
		t.complete();
		if (activeTransition==t) activeTransition=null;
	}
	
	/**
	 * Start a new transition
	 * @param t
	 * @param duration
	 */
	public void start(Transition t, int duration) {
		finish();
		
		Log.d(Constants.LOG_TAG, "Starting transition " + t);
		
		activeTransition=t;
		startTime=SystemClock.uptimeMillis();
		endTime=startTime+duration;
		totalDuration=duration;
		framesCompleted=0;
		frameDuration=totalDuration/frameRate;
		if (frameDuration<MIN_FRAME_DURATION) frameDuration=MIN_FRAME_DURATION;
		
		t.start();
		if (!schedule()) {
			finish();
		}
	}
	
	/**
	 * If there is an active transition, finishes it
	 * immediately.
	 */
	public void finish() {
		if (activeTransition!=null) {
			Transition t=activeTransition;
			activeTransition=null;
			handler.removeCallbacks(this);
			
			t.complete();
			Log.d(Constants.LOG_TAG, String.format("Transition complete (total frames=%s, duration=%s)", framesCompleted, SystemClock.uptimeMillis() - startTime));
		}
	}
	
	private boolean schedule() {
		if (activeTransition==null) return false;
		long currentTime=SystemClock.uptimeMillis();
		if (currentTime>endTime) {
			return false;
		} else {
			handler.postDelayed(this, frameDuration);
			return true;
		}
	}
	
	@Override
	public void run() {
		if (schedule()) {
			// Run a frame
			float factor=(SystemClock.uptimeMillis() - startTime) / (float)totalDuration;
			if (factor<0) factor=0.0f;
			else if (factor>1.0) factor=1.0f;
			
			activeTransition.frame(factor);
			Log.d(Constants.LOG_TAG, "Transition frame");
			framesCompleted++;
		} else {
			// Done
			finish();
		}
	}
}
