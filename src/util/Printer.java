package util;

public class Printer {
	
	public boolean enabled;
	private boolean backspaceProgress;
	private int lastProgressStrLen;
	private int progress;
	
	public Printer(boolean enabled){
		this.enabled = enabled;
		backspaceProgress = Environment.exjobbInTerminal();
	}
	
	public synchronized void progress(){
		progress(1);
	}
	
	public synchronized void progress(int period){
		if(enabled){
			progress ++;
			if(backspaceProgress){
				if(progress == 0){
					lastProgressStrLen = 0; //new task, with no previously printed progress
				}
				if(progress % period == 0){
					for(int c = 0; c < lastProgressStrLen; c++){
						System.out.print("\b");
					}
					System.out.print(progress);
					lastProgressStrLen = Integer.toString(progress).length();
				}
			}else{
				if(progress % period == 0){
					System.out.print(progress + "  ");
				}
			}
		}
	}
	
	public void resetProgress(){
		lastProgressStrLen = 0;
		progress = 0;
	}
	
	public void println(String s){
		if(enabled){
			System.out.println(s);
			lastProgressStrLen = 0;
		}
	}
	
	public void print(String s){
		if(enabled){
			System.out.print(s);
			lastProgressStrLen = 0;
		}
	}
	
	public void println(Object o){
		if(enabled){
			System.out.println(o);
			lastProgressStrLen = 0;
		}
	}
	
	public void print(Object o){
		if(enabled){
			System.out.print(o);
			lastProgressStrLen = 0;
		}
	}
	
	public static void printBigProgressHeader(int progress, int total){
		System.out.println("-------------------------------- [ " + progress + " / " + total + " ] --------------------------------");
	}
}
