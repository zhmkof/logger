package com.orhanobut.logger;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Logger is a wrapper of {@link Log}
 * But more pretty, simple and powerful
 *
 * @author Orhan Obut
 */
final class LoggerPrinter implements Printer {

    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;

    /**
     * It is used for json pretty print
     */
    private static final int JSON_INDENT = 4;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 3;

    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private static final Settings settings = new Settings();

    /**
     * TAG is used for the Log, the name is a little different
     * in order to differentiate the logs easily with the filter
     */
    protected static String TAG = "DefaultTagName";

    /**
     * Localize single tag and method count for each thread
     */
    private static final ThreadLocal<String> LOCAL_TAG = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LOCAL_METHOD_COUNT = new ThreadLocal<>();

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in Logger
     */
    @Override
    public Settings init(String tag) {
        if (tag == null) {
            throw new NullPointerException("tag may not be null");
        }
        if (tag.trim().length() == 0) {
            throw new IllegalStateException("tag may not be empty");
        }
        LoggerPrinter.TAG = tag;
        return settings;
    }

    @Override
    public Printer t(String tag, int methodCount) {
        if (tag != null) {
            LOCAL_TAG.set(tag);
        }
        LOCAL_METHOD_COUNT.set(methodCount);
        return this;
    }

    @Override
    public void d(boolean multiLines, String message, Object... args) {
        log(multiLines, Log.DEBUG, message, args);
    }

    @Override
    public void e(String message, Object... args) {
        e(null, message, args);
    }

    @Override
    public void e(boolean multiLines, Throwable throwable, String message, Object... args) {
        if (throwable != null && message != null) {
            message += " : " + throwable.toString();
        }
        if (throwable != null && message == null) {
            message = throwable.toString();
        }
        if (message == null) {
            message = "No message/exception is set";
        }
        log(multiLines, Log.ERROR, message, args);
    }

    @Override
    public void w(boolean multiLines, String message, Object... args) {
        log(multiLines, Log.WARN, message, args);
    }

    @Override
    public void i(boolean multiLines, String message, Object... args) {
        log(multiLines, Log.INFO, message, args);
    }

    @Override
    public void v(boolean multiLines, String message, Object... args) {
        log(multiLines, Log.VERBOSE, message, args);
    }

    @Override
    public void wtf(boolean multiLines, String message, Object... args) {
        log(multiLines, Log.ASSERT, message, args);
    }

    /**
     * Formats the json content and print it
     *
     * @param json the json content
     */
    @Override
    public void json(String json) {
        if (TextUtils.isEmpty(json)) {
            d(true, "Empty/Null json content");
            return;
        }
        try {
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String message = jsonObject.toString(JSON_INDENT);
                d(true, message);
                return;
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String message = jsonArray.toString(JSON_INDENT);
                d(true, message);
            }
        } catch (JSONException e) {
            e(e.getCause().getMessage() + "\n" + json);
        }
    }

    /**
     * Formats the json content and print it
     *
     * @param xml the xml content
     */
    @Override
    public void xml(String xml) {
        if (TextUtils.isEmpty(xml)) {
            d(true, "Empty/Null xml content");
            return;
        }
        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StreamResult xmlOutput = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(xmlInput, xmlOutput);
            d(true, xmlOutput.getWriter().toString().replaceFirst(">", ">\n"));
        } catch (TransformerException e) {
            e(e.getCause().getMessage() + "\n" + xml);
        }
    }

    /**
     * This method is synchronized in order to avoid messy of logs' order.
     */
    private synchronized void log(boolean multiLines, int logType, String msg, Object... args) {
        if (settings.getLogLevel() == LogLevel.NONE) {
            return;
        }

        String tag = getTag();
        int methodCount = getMethodCount();
		String message = createMessage(msg, args);
		byte[] bytes = message.getBytes();
		int length = bytes.length;

		if (multiLines) {
			logHeaderContent(logType, tag, methodCount);
			//get bytes of message with system's default charset (which is UTF-8 for Android)
			if (length <= CHUNK_SIZE) {
				logContent(logType, tag, message);
				return;
			}
			for (int i = 0; i < length; i += CHUNK_SIZE) {
				int count = Math.min(length - i, CHUNK_SIZE);
				//create a new String with system's default charset (which is UTF-8 for Android)
				logContent(logType, tag, new String(bytes, i, count));
			}
		} else {
			String header = logHeaderContent(methodCount);
			String full = header + message;
			logContent(logType, tag, full);
		}
    }

	private String logHeaderContent(int methodCount) {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		int stackOffset = getStackOffset(trace);

		//corresponding method count with the current stack may exceeds the stack trace. Trims the count
		if (methodCount + stackOffset > trace.length) {
			methodCount = trace.length - stackOffset - 1;
		}

		StringBuilder builder = new StringBuilder();
		String level = "";
		for (int i = methodCount; i > 0; i--) {
			int stackIndex = i + stackOffset;
			if (stackIndex >= trace.length) {
				continue;
			}
			builder.append(level)
					.append("(")
					.append(trace[stackIndex].getFileName())
					.append(":")
					.append(trace[stackIndex].getLineNumber())
					.append(")")
//					.append(getSimpleClassName(trace[stackIndex].getClassName()))
					.append("=>")
					.append(trace[stackIndex].getMethodName())
					.append(" | Thread: ")
					.append(Thread.currentThread().getName())
					.append(" | ");
			level += "   ";
		}
		return builder.toString();
	}

    private void logHeaderContent(int logType, String tag, int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        int stackOffset = getStackOffset(trace);

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

		StringBuilder builder;
		String level = "";
        for (int i = methodCount; i > 0; i--) {
			builder = new StringBuilder();
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            builder.append(level)
					.append("(")
					.append(trace[stackIndex].getFileName())
					.append(":")
					.append(trace[stackIndex].getLineNumber())
					.append(")")
//					.append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append("=>")
                    .append(trace[stackIndex].getMethodName())
					.append(" | Thread: ")
					.append(Thread.currentThread().getName())
					.append(" | ");
            level += "   ";
            logChunk(logType, tag, builder.toString());
        }
    }

    private void logContent(int logType, String tag, String chunk) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, line);
        }
    }

    private void logChunk(int logType, String tag, String chunk) {
        String finalTag = formatTag(tag);
        switch (logType) {
            case Log.ERROR:
                Log.e(finalTag, chunk);
                break;
            case Log.INFO:
                Log.i(finalTag, chunk);
                break;
            case Log.VERBOSE:
                Log.v(finalTag, chunk);
                break;
            case Log.WARN:
                Log.w(finalTag, chunk);
                break;
            case Log.ASSERT:
                Log.wtf(finalTag, chunk);
                break;
            case Log.DEBUG:
                // Fall through, log debug by default
            default:
                Log.d(finalTag, chunk);
                break;
        }
    }

    private String formatTag(String tag) {
        if (!TextUtils.isEmpty(tag) && !TextUtils.equals(TAG, tag)) {
            return TAG + "-" + tag;
        }
        return TAG;
    }

    /**
     * @return the appropriate tag based on local or global
     */
    private String getTag() {
        String tag = LOCAL_TAG.get();
        if (tag != null) {
            LOCAL_TAG.remove();
            return tag;
        }
        return TAG;
    }

    private String createMessage(String message, Object... args) {
        return args.length == 0 ? message : String.format(message, args);
    }

    private int getMethodCount() {
        Integer count = LOCAL_METHOD_COUNT.get();
        int result = settings.getMethodCount();
        if (count != null) {
            LOCAL_METHOD_COUNT.remove();
            result = count;
        }
        if (result < 0) {
            throw new IllegalStateException("methodCount cannot be negative");
        }
        return result;
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(LoggerPrinter.class.getName()) && !name.equals(Logger.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

}
