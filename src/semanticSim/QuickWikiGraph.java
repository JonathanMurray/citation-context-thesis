package semanticSim;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.Printer;

/**
 * An "online" version of WikiGraph, that loads data when it needs it. The idea was to have a class that is quicker for 
 * tiny test examples.
 * @author jonathan
 *
 */
public class QuickWikiGraph extends WikiGraph{
	
	private static Printer printer = new Printer(true);
	
	private Scanner titlesSortedFile;
	private Scanner linksSortedFile;
	private int linksLineNumber = 0;
	
	private HashMap<String, Integer> indices = new HashMap<String, Integer>();
	private HashMap<Integer, TIntArrayList> links = new HashMap<Integer, TIntArrayList>();
	
	/**
	 * Accepts the 2 text files from the wiki corpus
	 * @param titlesSortedFile
	 * @param linksSortedFile
	 */
	public QuickWikiGraph(File titlesSortedFile, File linksSortedFile) {
		try {
			this.titlesSortedFile = new Scanner(titlesSortedFile);
			this.linksSortedFile = new Scanner(linksSortedFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected int getPhraseIndex(String phrase) {
		printer.println("getPhraseIndex(" + phrase + ")");
		
		if(indices.containsKey(phrase)){
			System.out.println("Already cached " + indices.get(phrase));
			return indices.get(phrase);
		}
		
		int lineNumber = 1;
		while(titlesSortedFile.hasNextLine()){
			String line = titlesSortedFile.nextLine();
			indices.put(line, lineNumber);
			if(line.equals(phrase)){
				printer.println("return " + lineNumber);
				return lineNumber;
			}
			lineNumber ++;
		}
		printer.println("Not found");
		throw new NoSuchElementException();
	}

	@Override
	protected TIntArrayList getLinksFrom(int index) {
		printer.println("getLinksFrom(" + index + ")");
		
		if(links.containsKey(index)){
			System.out.println("Already cached " + links.get(index));
			return links.get(index);
		}
		
		while(linksSortedFile.hasNextLine()){
			String line = linksSortedFile.nextLine();
			linksLineNumber ++;
			if(linksLineNumber == index){
				String[] tokens = line.split("\\s+");
				TIntArrayList links = new TIntArrayList(tokens.length - 1);
				for(int j = 1; j < tokens.length; j++){
					int otherIndex = Integer.parseInt(tokens[j]);
					links.add(otherIndex);
				}
				printer.println("return " + links);
				return links;
			}
		}
		printer.println("not found");
		throw new NoSuchElementException();
	}
}
