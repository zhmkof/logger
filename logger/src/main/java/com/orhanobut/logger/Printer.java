package com.orhanobut.logger;

/**
 * @author Orhan Obut
 */
public interface Printer {

    Printer t(String tag, int methodCount);

    Settings init(String tag);

    void d(boolean multiLines, String message, Object... args);

    void e(String message, Object... args);

    void e(boolean multiLines, Throwable throwable, String message, Object... args);

    void w(boolean multiLines, String message, Object... args);

    void i(boolean multiLines, String message, Object... args);

    void v(boolean multiLines, String message, Object... args);

    void wtf(boolean multiLines, String message, Object... args);

    void json(String json);

    void xml(String xml);
}
