package main;

import java.io.File;

import org.apache.pdfbox.ExtractText;

import util.Environment;

public class ConvertPDFsToText {
	
	public static void main(String[] args) {
		String corpusDir = Environment.resources() + "/corpus";
		convertAllPDFsToText(new File(corpusDir, "pdfs"), new File(corpusDir, "full-text"));
	}
	
	public static void convertAllPDFsToText(File pdfDir, File txtDir){
		for(File pdfFile : pdfDir.listFiles()){
			if(pdfFile.getName().endsWith(".pdf")){
				String baseName = pdfFile.getName().split("\\.pdf")[0];
				File textFile = new File(txtDir, baseName + ".txt");
				pdfToText(pdfFile, textFile);
			}
		}
	}
	
	public static void pdfToText(File pdfFile, File textFile){
		try {
			System.out.print("Converting " + pdfFile.getName() + " to " + textFile.getName() + " ...  ");
			ExtractText.main(new String[]{pdfFile.getAbsolutePath(), textFile.getAbsolutePath()});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(textFile.exists()? "[x]" : "[FAILED]");
	}
}
