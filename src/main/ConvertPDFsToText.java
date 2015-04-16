package main;

import java.io.File;

import org.apache.pdfbox.ExtractText;

import util.Environment;
import util.Printer;

public class ConvertPDFsToText {
	
	public static void main(String[] args) {
		
		String pdfDir = "pdfs";
		String txtDir = "full-text";
		
		if(args.length == 2){
			pdfDir = args[0];
			txtDir = args[1];
		}else if(args.length != 0){
			System.out.println("Usage:");
			System.out.println("0 parameters or");
			System.out.println("2 parameters: 'pdf_dir' 'text_dir'");
			return;
		}
		
		Printer.printBigHeader("Convert PDFs to text");
		
		String corpusDir = Environment.resources() + "/corpus";
		convertAllPDFsToText(new File(corpusDir, pdfDir), new File(corpusDir, txtDir));
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
