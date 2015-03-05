package util;

public class Dirs {
	public static String exjobbHome(){
		return System.getenv("EXJOBB_HOME");
	}
	
	public static String resources(){
		return System.getenv("RESOURCES_DIR");
	}
}
