package com.orhanobut.logger;

/**
 * Logger is a wrapper of {@link android.util.Log}
 * But more pretty, simple and powerful
 *
 * @author Orhan Obut
 */
public final class Logger {

    private static final Printer printer = new LoggerPrinter();
    private static final int DEFAULT_METHOD_COUNT = 2;
    private static final String DEFAULT_TAG = "PRETTYLOGGER";

    //no instance
    private Logger() {
    }

    /**
     * It is used to get the settings object in order to change settings
     *
     * @return the settings object
     */
    public static Settings init() {
        return printer.init(DEFAULT_TAG);
    }

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in Logger
     */
    public static Settings init(String tag) {
        return printer.init(tag);
    }

    public static Printer t(String tag) {
        return printer.t(tag, DEFAULT_METHOD_COUNT);
    }

    public static Printer t(int methodCount) {
        return printer.t(null, methodCount);
    }

    public static Printer t(String tag, int methodCount) {
        return printer.t(tag, methodCount);
    }

    public static void d(String message, Object... args) {
        printer.d(true, message, args);
    }

    public static void e(String message, Object... args) {
        printer.e(null, message, args);
    }

    public static void e(Throwable throwable, String message, Object... args) {
        printer.e(true, throwable, message, args);
    }

    public static void i(String message, Object... args) {
        printer.i(true, message, args);
    }

    public static void v(String message, Object... args) {
        printer.v(true, message, args);
    }

    public static void w(String message, Object... args) {
        printer.w(true, message, args);
    }

    public static void wtf(String message, Object... args) {
        printer.wtf(true, message, args);
    }

	/**
	 * single line
	 */
	public static void d(String tag, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.d(false, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

	/**
	 * single line
	 */
	public static void e(String tag, Throwable throwable, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.e(false, throwable, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

	/**
	 * single line
	 */
	public static void i(String tag, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.i(false, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

	/**
	 * single line
	 */
	public static void v(String tag, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.v(false, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

	/**
	 * single line
	 */
	public static void w(String tag, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.w(false, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

	/**
	 * single line
	 */
	public static void wtf(String tag, String message) {
		String tmp = LoggerPrinter.TAG;
		Logger.init(tag).setMethodCount(1);
		printer.wtf(false, message, new Object(){});
		Logger.init(tmp).setMethodCount(2);
	}

    /**
     * Formats the json content and print it
     *
     * @param json the json content
     */
    public static void json(String json) {
        printer.json(json);
    }

    /**
     * Formats the json content and print it
     *
     * @param xml the xml content
     */
    public static void xml(String xml) {
        printer.xml(xml);
    }

}
