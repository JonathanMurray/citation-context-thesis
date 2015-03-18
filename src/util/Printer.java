package util;

public class Printer {
	
	public boolean enabled;
	private boolean backspaceProgress;
	private int lastProgressStrLen;
	
	public Printer(boolean enabled){
		this.enabled = enabled;
		backspaceProgress = Environment.exjobbInTerminal();
	}
	
	public void progress(int i, int period){
		if(enabled){
			if(backspaceProgress){
				if(i == 0){
					lastProgressStrLen = 0; //new task, with no previously printed progress
				}
				if(i % period == 0){
					for(int c = 0; c < lastProgressStrLen; c++){
						System.out.print("\b");
					}
					System.out.print(i);
					lastProgressStrLen = Integer.toString(i).length();
				}
			}else{
				if(i % period == 0){
					System.out.print(i + "  ");
				}
			}
		}
	}
	
	public void resetProgressBackspace(){
		lastProgressStrLen = 0;
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
}
