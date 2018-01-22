/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.servlet;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.robotframework.javalib.util.StdStreamRedirecter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the XML-RPC methods that implement the remote library interface.
 *
 * @author David Luu
 */
public class ServerMethods implements JRobotServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(ServerMethods.class.getName());
    private static final List<String>
            genericExceptions =
            Arrays.asList("AssertionError", "AssertionFailedError", "Exception", "Error", "RuntimeError",
                    "RuntimeException", "DataError", "TimeoutError", "RemoteError");
    private static final String[] logLevelPrefixes = new String[] {"*TRACE*", "*DEBUG*", "*INFO*", "*HTML*", "*WARN*"};
    private final RemoteServerServlet servlet;

    /**
     * Constructor creating {@link JRobotServlet}
     *
     * @param servlet Instance containing Robot-framework remote libraries implementations
     */
    public ServerMethods(RemoteServerServlet servlet) {
        this.servlet = servlet;
    }

    @Override public String[] get_keyword_names() {
        final String[] names = servlet.getLibrary().getKeywordNames();
        if (names == null || names.length == 0)
            throw new RuntimeException("No keywords found in the test library");
        return names;
    }

    @Override public Map<String, Object> run_keyword(String keyword, Object[] args, Map<String, Object> kwargs) {
        Map<String, Object> result = new HashMap<>();
        StdStreamRedirecter redirector = new StdStreamRedirecter();
        redirector.redirectStdStreams();
        try {
            result.put("status", "PASS");
            Object retObj;
            try {
                retObj = servlet.getLibrary().runKeyword(keyword, args, kwargs);
            } catch (Exception e) {
                if (illegalArgumentIn(e)) {
                    for (int i = 0; i < args.length; i++)
                        args[i] = arraysToLists(args[i]);
                    retObj = servlet.getLibrary().runKeyword(keyword, args, kwargs);
                } else {
                    throw (e);
                }
            }
            if (retObj != null && !retObj.equals("")) {
                result.put("return", retObj);
            }
        } catch (Throwable e) {
            result.put("status", "FAIL");
            Throwable thrown = e.getCause() == null ? e : e.getCause();
            result.put("error", getError(thrown));
            result.put("traceback", Throwables.getStackTraceAsString(thrown));
            boolean continuable = isFlagSet("ROBOT_CONTINUE_ON_FAILURE", thrown);
            if (continuable) {
                result.put("continuable", true);
            }
            boolean fatal = isFlagSet("ROBOT_EXIT_ON_FAILURE", thrown);
            if (fatal) {
                result.put("fatal", true);
            }
        } finally {
            String stdOut = Strings.nullToEmpty(redirector.getStdOutAsString());
            String stdErr = Strings.nullToEmpty(redirector.getStdErrAsString());
            if (!stdOut.isEmpty() || !stdErr.isEmpty()) {
                StringBuilder output = new StringBuilder(stdOut);
                if (!stdOut.isEmpty() && !stdErr.isEmpty()) {
                    if (!stdOut.endsWith("\n")) {
                        output.append("\n");
                    }
                    boolean addLevel = true;
                    for (String prefix : logLevelPrefixes) {
                        if (stdErr.startsWith(prefix)) {
                            addLevel = false;
                            break;
                        }
                    }
                    if (addLevel) {
                        output.append("*INFO*");
                    }
                }
                result.put("output", output.append(stdErr).toString());
            }
            redirector.resetStdStreams();
        }
        return result;
    }

    @Override public Map<String, Object> run_keyword(String keyword, Object[] args) {
        Map<String, Object> kwargs = new HashMap<>();
        // If '=' is at the beginning of argument declaration or at the end argument is not kwarg candidate
        Arrays.stream(Objects.requireNonNull(args)).map(Object::toString).forEach(argument -> {
            final int split = argument.indexOf("=");
            if (split > 0 && split < argument.length() - 1) {
                kwargs.put(argument.substring(0, split), argument.substring(split + 1));
            }
        });
        return run_keyword(keyword, args, kwargs);
    }

    @Override public String[] get_keyword_arguments(String keyword) {
        final String[] args = servlet.getLibrary().getKeywordArguments(keyword);
        return args == null ? new String[0] : args;
    }

    @Override public String[] get_keyword_tags(String keyword) {
        final String[] args = servlet.getLibrary().getKeywordTags(keyword);
        return args == null ? new String[0] : args;
    }

    @Override public String get_keyword_documentation(String keyword) {
        final String doc = servlet.getLibrary().getKeywordDocumentation(keyword);
        return doc == null ? "" : doc;
    }

    /**
     * Extract error message from provided {@link Throwable}
     *
     * @param thrown Instance of {@link Throwable} that is used for extraction
     * @return Extracted error
     */
    private String getError(Throwable thrown) {
        final String simpleName = thrown.getClass().getSimpleName();
        if (genericExceptions.contains(simpleName) || isFlagSet("ROBOT_SUPPRESS_NAME", thrown)) {
            return thrown.getMessage() == null || thrown.getMessage().isEmpty() ? simpleName : thrown.getMessage();
        } else {
            return String.format("%s: %s", thrown.getClass().getName(), thrown.getMessage());
        }
    }

    /**
     * @param name   Name of {@link java.lang.reflect.Field}
     * @param thrown Instance of {@link Throwable} that will be checked
     * @return Checks if {@link java.lang.reflect.Field} for provided name is set
     */
    private boolean isFlagSet(String name, Throwable thrown) {
        boolean flag = false;
        try {
            flag = thrown.getClass().getField(name).getBoolean(thrown);
        } catch (Exception ignore) {
        }
        return flag;
    }

    /**
     * Recursively converts {@link java.lang.reflect.Array} to {@link List},
     * or re-wrap {@link Map}
     *
     * @param arg Instance of {@link Object} that will be converted
     * @return Converted result
     */
    protected Object arraysToLists(Object arg) {
        if (arg instanceof Object[]) {
            Object[] array = (Object[]) arg;
            List<Object> list = Arrays.asList(array);
            for (int i = 0; i < list.size(); i++)
                list.set(i, arraysToLists(list.get(i)));
            return list;
        } else if (arg instanceof Map<?, ?>) {
            Map<?, ?> oldMap = (Map<?, ?>) arg;
            Map<Object, Object> newMap = new HashMap<>();
            for (Object key : oldMap.keySet())
                newMap.put(key, arraysToLists(oldMap.get(key)));
            return newMap;
        } else
            return arg;
    }

    /**
     * @param t Instance of {@link Throwable} that will be checked
     * @return If cause of {@link Throwable} is due to {@link IllegalArgumentException}
     */
    private boolean illegalArgumentIn(Throwable t) {
        if (Objects.isNull(t) || t.getClass().equals(IllegalArgumentException.class)) {
            return true;
        }
        Throwable inner = t;
        while (inner.getCause() != null) {
            inner = inner.getCause();
            if (inner.getClass().equals(IllegalArgumentException.class)) {
                return true;
            }
        }
        return false;
    }
}
