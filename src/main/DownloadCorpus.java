package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.io.IOUtils;

import util.Environment;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;

public class DownloadCorpus {
	
	static String resources = Environment.resources();
	
	public static void main(String[] args) throws Exception{
		List<String> prefixes = new ArrayList<String>();
//		String[] prefixes = new String[]{
//				"J79", "J80", "J81"
//				"A92","D07","J93","N03","N06","P04","P07","W02","W05","C98",
//				"J90","J96","N04","P02","P04","P05","P90","W04","W06", "P08"
//		};
		
		addPrefixes(prefixes, "J", 79, 14);
		addPrefixes(prefixes, "P", 79, 14);
		addPrefixes(prefixes, "E", 83, 14);
		addPrefixes(prefixes, "A", 00, 13);
		addPrefixes(prefixes, "W", 90, 14);
		addPrefixes(prefixes, "S", 98, 14);
		addPrefixes(prefixes, "C", 65, 14);
		addPrefixes(prefixes, "H", 86, 01);
		addPrefixes(prefixes, "Y", 95, 14);
		addPrefixes(prefixes, "U", 03, 14);
		
		int numFetched = 0;
		for(String prefix : prefixes){
			System.out.println(prefix);
			numFetched += fetchMany(prefix);
			System.out.println("Fetched so far: " + numFetched);
		}
	}
	
	private static void addPrefixes(List<String> prefixes, String ch, int from, int to){
		for(int i = from; i != to; i = (i+1)%100){
			String number = "" + i;
			if(number.length() == 1){
				number = "0" + number;
			}
			prefixes.add(ch + i);
		}
	}
	
	private static int fetchMany(String prefix) throws InterruptedException, IOException, PageBiggerThanMaxSizeException{
		int numFetched = 0;
		for(int lastDigits = 0; lastDigits < 3000; lastDigits++){
			boolean fetched;
			try{
				String lastDigitsStr = "" + lastDigits;
				while(lastDigitsStr.length() < 4){
					lastDigitsStr = "0" + lastDigitsStr;
				}
				fetched = fetch(prefix, lastDigitsStr);
			}catch(FileNotFoundException e){
				fetched = false;
			}
			if(fetched){
				numFetched ++;
			}else{
				if(lastDigits % 100 != 0){ //Go to next 100-step
					while(lastDigits % 100 != 0){
						lastDigits++;
					}
				}
			}
		}
		return numFetched;
	}
	
	private static boolean fetch(String prefix, String lastDigits) throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
		System.out.println(lastDigits);
		File newFile = new File(resources + "/corpus/pdfs", prefix + "-" + lastDigits + ".pdf");
		if(newFile.exists()){
			System.out.println(newFile.getName() + " already exists.");
			return false;
		}
		URL url = new URL("http://www.aclweb.org/anthology/" + prefix + "-" + lastDigits);
		BufferedInputStream in = new BufferedInputStream(url.openStream());
		System.out.print("Fetched " + newFile.getName());
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile));
		
		System.out.print("  writing to file ... ");
		int numBytes = IOUtils.copy(in, fos);
		
		
		
//		int b;
//		while ((b = in.read()) != -1) {
//			fos.write(b);
//			
//		}
		System.out.print("~ closing, flushing ... ");
		in.close();
		fos.flush();
		fos.close();
		System.out.println("[x]  " + (numBytes/1024) + "kB");
		return true;
	}
}
