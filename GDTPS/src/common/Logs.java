package common;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Log system
 *
 * @author Marais-Viau
 */
public class Logs {
	private static Logger logger = Logger.getLogger("logger");
	private static boolean debug = true;

	/**
	 * Display a standard message
	 *
	 * @param message the message to display
	 */
	public static void log(String message) {
		if (debug) {
			logger.log(Level.INFO, message);
		}
	}

	/**
	 * Display a warning message
	 *
	 * @param message the message to display
	 */
	public static void warning(String message) {
		if (debug) {
			logger.log(Level.WARNING, message);
		}
	}

	/**
	 * Display an error message
	 *
	 * @param message the message to display
	 */
	public static void error(String message) {
		if (debug) {
			logger.log(Level.SEVERE, message);
		}
	}

	public static void debugOff() {
		debug = false;
	}
}
