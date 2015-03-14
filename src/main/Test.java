package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import util.Lemmatizer;

public class Test {
	
	
	private static void testPerformance(){
		int num = 1000000;
		StringBuilder line = new StringBuilder("" + num + ":");
		
		for(int i = 0; i < 5000; i++){
			num ++;
			line.append(" " + num);
		}
		
		ConcurrentHashMap<Integer, List<Integer>> links = new ConcurrentHashMap<Integer, List<Integer>>();
		
		parseLineAddLinksScanner(line.toString(), links);
		parseLineAddLinksScanner(line.toString(), links);
		long t = System.currentTimeMillis();
		parseLineAddLinksScanner(line.toString(), links);
		System.out.println("scanner: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		parseLineAddLinksSplit(line.toString(), links);
		System.out.println("split: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		parseLineAddLinksTokenizer(line.toString(), links);
		System.out.println("str-tokenizer: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
//		parseLineAddLinksIndexOf(line.toString(), links);
//		System.out.println("index-of: " + (System.currentTimeMillis()-t));
	}
	
	private static void parseLineAddLinksScanner(String line, ConcurrentHashMap<Integer, List<Integer>> links){
		try(Scanner sc = new Scanner(line)){
			String citerStr = sc.next();
			int citer = Integer.parseInt(citerStr.substring(0, citerStr.length() -1));
			ArrayList<Integer> cited = new ArrayList<Integer>();
			while(sc.hasNext()){
				cited.add(Integer.parseInt(sc.next()));
			}
			links.put(citer, cited);
		}
	}
	
	private static void parseLineAddLinksSplit(String line, ConcurrentHashMap<Integer, List<Integer>> links){
		String[] tokens = line.split(" ");
		int citer = Integer.parseInt(tokens[0].substring(0, tokens[0].length()-1));
		ArrayList<Integer> cited = Arrays.stream(tokens).parallel()
				.skip(1)
				.map(Integer::parseInt)
				.collect(Collectors.toCollection(ArrayList::new));
		links.put(citer, cited);
	}
	
	private static void parseLineAddLinksTokenizer(String line, ConcurrentHashMap<Integer, List<Integer>> links){
		StringTokenizer tokenizer = new StringTokenizer(line);
		String citerStr = tokenizer.nextToken();
		int citer = Integer.parseInt(citerStr.substring(0, citerStr.length()-1));
		ArrayList<Integer> cited = new ArrayList<Integer>();
		while(tokenizer.hasMoreTokens()){
			cited.add(Integer.parseInt(tokenizer.nextToken()));
		}
		links.put(citer, cited);
	}
}
