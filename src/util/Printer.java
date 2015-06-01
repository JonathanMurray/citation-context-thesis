package util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
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
	
	private final static DecimalFormat decimalFormat = new DecimalFormat("#0.00");
	
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
	
	public static <T extends Text, R extends Result> void printMultipleResults(String title, Collection<R> results, List<Dataset<T>> datasets, boolean verbose){
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		System.out.println("                 MULTIPLE RESULTS (" + title + "): ");
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		int i = 0;
		double sumNegF1 = 0;
		double sumPosF1 = 0;
		double sumPosF3 = 0;
		double sumMicroAvgF = 0;
		double sumMacroAvgF = 0;
		int sumPassedMillis = 0;
		for(Result result : results){
			Dataset<T> dataset = null;
			if(datasets != null){
				dataset = datasets.get(i);
			}
			printResult(result, null, verbose, dataset);
			i ++;
			sumNegF1 += result.negativeFMeasure(1);
			sumPosF1 += result.positiveFMeasure(1);
			sumPosF3 += result.positiveFMeasure(3);
			sumPassedMillis += result.getPassedMillis();
			sumMicroAvgF += result.microAvgFMeasure(1);
			sumMacroAvgF += result.macroAvgFMeasure(1);
		}
		
		System.out.println();
		System.out.println("Avg neg. F1: " + sumNegF1/(double)results.size());
		System.out.println("Avg pos. F1: " + sumPosF1/(double)results.size());
		System.out.println("Avg pos. F3: " + sumPosF3/(double)results.size());
		System.out.println("Avg micro avg. F: " + decimalFormat.format(sumMicroAvgF/(double)results.size()));
		System.out.println("Avg macro avg. F: " + decimalFormat.format(sumMacroAvgF/(double)results.size()));
		System.out.println("Total time: " + (sumPassedMillis/1000.0) + "s");
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
			System.out.print("\t" + result.label() + " & " + f.format(result.negativeFMeasure(1)));
			System.out.print(" & " + f.format(result.positiveFMeasure(1)));
			System.out.print(" & " + f.format(result.positiveFMeasure(3)));
			System.out.print(" \\\\ \\hline");
			if(dataset != null){
				System.out.print("  (" + dataset.citedMainAuthor + ")");
				if(dataset != null && dataset.hasAcronymsHooks){
					System.out.print("    " + dataset.getLexicalHooks() + " " + dataset.getAcronyms() + " ");
				}
			}
			System.out.println();
		}
	}
	
	public static void printBigHeader(String text){
		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("                 " + text);
		System.out.println("--------------------------------------------------");
		System.out.println();
	}
	
	public static String toString(double d){
		return decimalFormat.format(d);
	}
	
	public static <K,V extends Comparable<V>> String valueSortedMap(HashMap<K,V> map, int limit){
		StringBuilder s= new StringBuilder();
		s.append("{\n");
		map.entrySet().stream()
			.sorted((e1,e2) -> e2.getValue().compareTo(e1.getValue()))
			.limit(limit)
			.forEach(e -> {
				s.append("  " + e.getKey() + ": " + e.getValue() + ",\n");
			});
		s.append("}");
		return s.toString();
	}
}
