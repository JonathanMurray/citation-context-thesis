package util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;

public class NonThrowingFileWriter implements Closeable, Flushable{
	
	private BufferedWriter writer;
	
	public NonThrowingFileWriter(File f){
		try{
			writer = new BufferedWriter(new FileWriter(f));
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("write crashed");
			System.exit(0);
		}
	}
	
	public void write(String s){
		try {
			writer.write(s);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("write crashed");
			System.exit(0);
		}
	}
	
	public void close(){
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}
}
