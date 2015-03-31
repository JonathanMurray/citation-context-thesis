package main;

import java.io.File;

import org.apache.pdfbox.ExtractText;

public class ConvertPDFsToText {
	
	public static void main(String[] args) {
//		convertAllPDFsToText(dir); //TODO
	}
	
	public static void convertAllPDFsToText(File dir){
		for(File pdfFile : dir.listFiles()){
			if(pdfFile.getName().endsWith(".pdf")){
				String baseName = pdfFile.getName().split("\\.pdf")[0];
				File textFile = new File(dir, baseName + ".txt");
				pdfToText(pdfFile, textFile);
			}
		}
	}
	
	public static void pdfToText(File pdfFile, File textFile){
		try {
			System.out.print("Converting " + pdfFile.getName() + " to " + textFile.getName() + "...  ");
			ExtractText.main(new String[]{pdfFile.getAbsolutePath(), textFile.getAbsolutePath()});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(textFile.exists()? "SUCCESS" : "FAIL");
	}
}
