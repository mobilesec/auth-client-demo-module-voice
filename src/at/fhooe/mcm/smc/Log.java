package at.fhooe.mcm.smc;

/** Conveneince logger class. */
public class Log {

	public static void e(String msg, Object... args) {
		System.err.println(String.format(msg, args));
	}
	
	public static void w(String msg, Object... args) {
		System.out.println(String.format("WARN: " + msg, args));
	}
	
	public static void d(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}
}
