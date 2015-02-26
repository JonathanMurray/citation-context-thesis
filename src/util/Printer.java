package util;

public class Printer {
	public static void printProgress(int i, int period, int printsPerLine){
		if(i % period == 0){
			System.out.print(i + "    ");
		}
		if(i % (period*printsPerLine) == 0){
			System.out.println();
		}
	}
}
