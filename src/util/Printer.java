package util;

public class Printer {
	
	private boolean enabled;
	
	public Printer(boolean enabled){
		this.enabled = enabled;
	}
	
	public void printProgress(int i, int period, int printsPerLine){
		if(enabled){
			if(i % period == 0){
				System.out.print(i + "    ");
			}
			if(i % (period*printsPerLine) == 0){
				System.out.println();
			}
		}
	}
	
	public void println(String s){
		if(enabled){
			System.out.println(s);
		}
	}
	
	public void print(String s){
		if(enabled){
			System.out.print(s);
		}
	}
	
	public void println(Object o){
		if(enabled){
			System.out.println(o);
		}
	}
	
	public void print(Object o){
		if(enabled){
			System.out.print(o);
		}
	}
}
