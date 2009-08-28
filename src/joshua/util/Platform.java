package joshua.util;

public class Platform {

	
	public static boolean isMac() {
		return System.getProperties().getProperty("os.name").toLowerCase().indexOf("mac") != -1;
	}
	
}
