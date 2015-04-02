package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import util.Environment;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;

public class DownloadCorpus {
	
	public static void main(String[] args) throws Exception{
		String[] prefixes = new String[]{
				"A92","D07","J93","N03","N06","P04","P07","W02","W05","C98",
				"J90","J96","N04","P02","P04","P05","P90","W04","W06"
		};
		for(String prefix : prefixes){
			fetchMany(prefix);
		}
	}
	
	private static void fetchMany(String prefix) throws InterruptedException, IOException, PageBiggerThanMaxSizeException{
		for(int lastDigits = 0; lastDigits < 3000; lastDigits++){
			try{
				String lastDigitsStr = "" + lastDigits;
				while(lastDigitsStr.length() < 4){
					lastDigitsStr = "0" + lastDigitsStr;
				}
				fetch(prefix, lastDigitsStr);
			}catch(FileNotFoundException e){
				if(lastDigits % 100 != 0){ //Go to next 100-step
					while(lastDigits % 100 != 0){
						lastDigits++;
					}
				}
			}
			
		}
	}
	
	private static void fetch(String prefix, String lastDigits) throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
		String resources = Environment.resources();
		File newFile = new File(resources + "/corpus/pdfs", prefix + "-" + lastDigits + ".pdf");
		if(newFile.exists()){
			System.out.println(newFile.getName() + " already exists.");
			return;
		}
		URL url = new URL("http://www.aclweb.org/anthology/" + prefix + "-" + lastDigits);
		BufferedInputStream in = new BufferedInputStream(url.openStream());
		System.out.println("Fetched " + newFile.getName());
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile));
		int b;
		while ((b = in.read()) != -1) {
			fos.write(b);
		}
		in.close();
		fos.flush();
		fos.close();
	}
}
