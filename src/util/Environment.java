package util;

/**
 * Handles environment variables that should be set on the computer running the
 * programs. The point of this that a different directory structure can be used
 * on different stations, without changing the code.
 * @author jonathan
 *
 */
public class Environment {
	public static String exjobbHome(){
		return System.getenv("EXJOBB_HOME");
	}
	
	public static String resources(){
		return System.getenv("RESOURCES_DIR");
	}
	
	public static boolean exjobbInTerminal(){
		String var = "EXJOBB_IN_TERMINAL";
		if(System.getenv().containsKey(var)){
			return System.getenv(var).equals("1");
		}
		System.err.println("envvar '" + var + "' not found!");
		return false;
	}
}
