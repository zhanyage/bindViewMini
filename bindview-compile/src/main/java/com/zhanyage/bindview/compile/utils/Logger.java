package com.zhanyage.bindview.compile.utils;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;


/**
 * Created by andya on 2019/4/12
 * Describe: log util
 */
public class Logger {

    private Messager msg;


    public Logger(Messager msg) {
        this.msg = msg;
    }

    /**
     * Print info log
     */
    public void info(CharSequence infoCh) {
        if (StringUtils.isNotEmpty(infoCh)) {
            msg.printMessage(Diagnostic.Kind.NOTE, Constants.PREFIX_OF_LOGGER + infoCh);
        }
    }

    /**
     * Print error log
     * @param errorCh
     */
    public void error(CharSequence errorCh) {
        if (StringUtils.isNotEmpty(errorCh)) {
            msg.printMessage(Diagnostic.Kind.ERROR, Constants.PREFIX_OF_LOGGER + "A error is occur," + errorCh);
        }
    }

    /**
     * Print error log
     */
    public void error(Throwable errorThrowable) {
        if (null != errorThrowable) {
            msg.printMessage(Diagnostic.Kind.ERROR, Constants.PREFIX_OF_LOGGER + "An exception is encountered, [" + errorThrowable.getMessage() + "]" + "\n" + formatStackTrace(errorThrowable.getStackTrace()));
        }
    }

    /**
     * Print warn log
     * @param warnCh
     */
    public void warning(CharSequence warnCh) {
        if (StringUtils.isNotEmpty(warnCh)) {
            msg.printMessage(Diagnostic.Kind.WARNING, Constants.PREFIX_OF_LOGGER + warnCh);
        }
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("    at ").append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
