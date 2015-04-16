package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import util.Printer;
import dataset.Dataset;
import dataset.DatasetFactory;
import dataset.DatasetParams;
import dataset.Text;
import dataset.TextParams;

public class DownloadPDFs {
	public static void downloadPDFsForHTML_Files(File htmlDir) throws IOException{
		Printer.printBigHeader("Download PDFs for HTML-files");
		for(File f : htmlDir.listFiles()){
			if(f.getName().endsWith(".html")){
				Dataset<Text> dataset = DatasetFactory.fromHtmlFile(DatasetParams.basic(TextParams.basic()), f, "");
				String name = dataset.datasetLabel;
				URL url = new URL("http://www.aclweb.org/anthology/" + name);
				BufferedInputStream in = new BufferedInputStream(url.openStream());
				File newFile = new File(htmlDir, name + ".pdf");
				System.out.println("new file: " + newFile.getAbsolutePath());
				BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile));
				int b;
				while((b = in.read()) != -1){
					fos.write(b);
				}
				in.close();
				fos.flush();
				fos.close();
			}
		}
	}
}
