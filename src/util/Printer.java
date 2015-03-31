package util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;

import dataset.Dataset;
import dataset.Result;
import dataset.Sentence;
import dataset.Text;

/**
 * Printer that can be disabled, and has advanced progress printing capabilities.
 * @author jonathan
 *
 */
public class Printer {
	
	public final boolean enabled;
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
	
	public static <T extends Text> void printMultipleResults(String title, Collection<Result> results, List<Dataset<T>> datasets, boolean verbose){
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		System.out.println("                 MULTIPLE RESULTS (" + title + "): ");
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		int i = 0;
		double sumF1 = 0;
		double sumF3 = 0;
		for(Result result : results){
			Dataset<T> dataset = null;
			if(datasets != null){
				dataset = datasets.get(i);
			}
			printResult(result, null, verbose, dataset);
			i ++;
			sumF1 += result.positiveFMeasure(1);
			sumF3 += result.positiveFMeasure(3);
		}
		
		System.out.println();
		System.out.println("SUM F1: " + sumF1/(double)results.size());
		System.out.println("SUM F3: " + sumF3/(double)results.size());
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		System.out.println("\n\n\n");
	}
	
	public static <T extends Text> void printResult(Result result, List<Sentence<T>> testSentences, boolean verbose, Dataset<T> dataset){
		NumberFormat f = new DecimalFormat("#.000"); 
		if(verbose){
			System.out.println();
			System.out.println(result.label());
			System.out.println("-------------------------");
			System.out.println("Passed time: " + (int)(result.getPassedMillis()/1000.0) + "s");
			System.out.println(result.confusionMatrixToString());
			System.out.println("pos F: " + f.format(result.positiveFMeasure(1)));
			System.out.println("pos F3: " + f.format(result.positiveFMeasure(3)));
			System.out.println("neg F: " + f.format(result.negativeFMeasure(1)));
			System.out.println("Micro avg. F: " + f.format(result.microAvgFMeasure(1)));
			System.out.println("Macro avg. F: " + f.format(result.macroAvgFMeasure(1)));
			System.out.println();
		}else{
			System.out.print("neg F: " + f.format(result.negativeFMeasure(1)));
			System.out.print("    pos F: " + f.format(result.positiveFMeasure(1)));
			System.out.print("    pos F3: " + f.format(result.positiveFMeasure(3)));
			if(dataset != null){
				System.out.print("  (" + dataset.citedMainAuthor + ")");
				if(dataset != null && dataset.hasExtra){
					System.out.print("    " + dataset.getLexicalHooks() + " " + dataset.getAcronyms() + " ");
				}
			}
			System.out.println();
		}
	}
}
