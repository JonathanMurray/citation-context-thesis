package util;

public class Environment {
	public static String exjobbHome(){
		return System.getenv("EXJOBB_HOME");
	}
	
	public static String resources(){
		return System.getenv("RESOURCES_DIR");
	}
	
	public static boolean exjobbInTerminal(){
		return System.getenv("EXJOBB_IN_TERMINAL").equals("1");
	}
}
