package net.rcode.nanomaps.transition;

import java.util.ArrayList;
import java.util.List;

import net.rcode.nanomaps.MapState;

/**
 * Base class for some kind of transition.
 * 
 * @author stella
 *
 */
public abstract class Transition {
	public interface Callback {
		public void onTransitionComplete(Transition t);
	}
	
	private MapState activeMapState;
	private MapState initialMapState;
	private MapState finalMapState;
	
	private List<Callback> callbacks;
	
	public Transition(MapState activeMapState) {
		this.activeMapState=activeMapState;
		this.initialMapState=activeMapState.dup();
		this.finalMapState=activeMapState.dup();
	}
	
	public MapState getActiveMapState() {
		return activeMapState;
	}
	
	public MapState getInitialMapState() {
		return initialMapState;
	}
	public MapState getFinalMapState() {
		return finalMapState;
	}
	
	public void addCallback(Callback cb) {
		if (callbacks==null) callbacks=new ArrayList<Callback>();
		callbacks.add(cb);
	}
	
	/**
	 * Performs the first step of the Transition.  Defaults to
	 * setting the activeMapState to initial.
	 */
	public final void start() {
		initialMapState.copy(activeMapState);
		onStart();
	}
	
	/**
	 * Perform a frame update.  Default implementation does nothing.
	 * @param factor Value between 0..1 indicating where we are on the timeline
	 */
	public final void frame(float factor) {
		onFrame(factor);
	}
	
	/**
	 * Performs the final update of the transition.  Defaults to setting the active
	 * map state to the final mapstate.
	 */
	public final void complete() {
		onComplete();
		if (callbacks!=null) {
			for (int i=0; i<callbacks.size(); i++) {
				callbacks.get(i).onTransitionComplete(this);
			}
		}
	}
	
	protected void onStart() {
	}
	protected void onFrame(float factor) {
	}
	protected void onComplete() {
		finalMapState.copy(activeMapState);
	}
	
	@Override
	public String toString() {
		return "Transition(initial=" + initialMapState + ", final=" + finalMapState + ")";
	}
}
