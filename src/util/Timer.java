package util;

import com.ibm.icu.text.DecimalFormat;

/**
 * Used for timing tasks.
 * @author jonathan
 *
 */
public class Timer {
	private long startTime;
	private long endTime;
	private boolean isRunning;
	
	public Timer(){
		reset();
	}
	
	public Timer reset(){
		startTime = System.currentTimeMillis();
		isRunning = true;
		return this;
	}
	
	public Timer stop(){
		endTime = System.currentTimeMillis();
		isRunning = false;
		return this;
	}
	
	public long getMillis(){
		if(isRunning){
			return System.currentTimeMillis() - startTime;
		}
		return endTime - startTime;
	}
	
	public String getMillisString(){
		return getMillis() + "ms";
	}
	
	public String getSecString(){
		return new DecimalFormat("#.#").format((getMillis()/1000.0)) + "s";
	}
}
