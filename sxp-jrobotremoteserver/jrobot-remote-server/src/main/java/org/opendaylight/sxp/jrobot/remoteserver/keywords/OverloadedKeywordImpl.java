/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import com.google.common.collect.Iterables;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing implementation of {@link OverloadedKeyword}
 */
public class OverloadedKeywordImpl implements OverloadedKeyword {

    protected static final Logger LOG = LoggerFactory.getLogger(OverloadedKeywordImpl.class.getName());

    private final Map<Integer, List<CheckedKeyword>> keywordMap = new HashMap<>();
    private final String keywordName;
    private final Object keywordClass;

    /**
     * Constructor creating {@link OverloadedKeyword} providing {@link Object} and {@link Method},
     * that are associated together
     *
     * @param keywordClass {@link Object} instance used for execution of {@link Method}
     * @param method       {@link Method} providing execution routine
     */
    public OverloadedKeywordImpl(Object keywordClass, Method method) {
        this(keywordClass, method, method.getName());
    }

    /**
     * Constructor creating {@link OverloadedKeyword} providing {@link Object} and {@link Method},
     * that are associated together
     *
     * @param keywordClass {@link Object} instance used for execution of {@link Method}
     * @param method       {@link Method} providing execution routine
     * @param name         Name of current {@link OverloadedKeyword}
     */
    public OverloadedKeywordImpl(Object keywordClass, Method method, String name) {
        this.keywordName = Objects.requireNonNull(name);
        this.keywordClass = Objects.requireNonNull(keywordClass);
        addOverload(method);
    }

    @Override public Object execute(Object[] arguments) {
        final int argCount = arguments.length;
        if (keywordMap.containsKey(argCount)) {
            for (CheckedKeyword checkedKeyword : keywordMap.get(argCount)) {
                if (checkedKeyword.canExecute(arguments)) {
                    LOG.debug("EXECUTED {} args{}", keywordName, checkedKeyword.getArgumentNames().length);
                    return checkedKeyword.execute(arguments);
                }
                LOG.debug("EXECUTION SKIPPED {} args {}", keywordName, checkedKeyword.getArgumentNames().length);
            }
            throw new IllegalArgumentException(
                    String.format("%s cannot be executed with args %s.", keywordName, Arrays.toString(arguments)));
        } else if (keywordMap.size() == 1) {
            throw new IllegalArgumentException(String.format("%s takes %d argument(s), received %d.", keywordName,
                    Iterables.get(keywordMap.keySet(), 0), argCount));
        }
        throw new IllegalArgumentException(
                String.format("No overload of %s takes %d argument(s).", keywordName, argCount));
    }

    @Override public void addOverload(Method method) {
        final int argCount = method.getParameterTypes().length;
        if (hasVariableArgs(method)) {
            LOG.warn("Overloads with variable arguments not supported. Ignoring overload {}", method);
        } else if (!keywordMap.containsKey(argCount)) {
            keywordMap.put(argCount, new ArrayList<>());
            keywordMap.get(argCount).add(new CheckedKeywordImpl(keywordClass, method));
        } else {
            keywordMap.get(argCount).add(new CheckedKeywordImpl(keywordClass, method));
            keywordMap.get(argCount).sort(Comparator.comparingLong(t -> computeKeywordRank(t.getArguments())));
        }
    }

    /**
     * Computes Rank of arguments used in {@link Method},
     * the lower the rank is the better
     *
     * @param args {@link Method} arguments used in ranking
     * @return Rank of arguments
     */
    private long computeKeywordRank(final Class<?>[] args) {
        // TODO: specify more complex ranking for method arguments that will consider prioritizing floats, doubles, etc.
        long rank = 0;
        for (int i = 0; i < args.length; i++) {
            if (String.class.equals(args[i])) {
                rank += Math.pow(2, args.length - i);
            }
        }
        LOG.debug("{} {} rank {}", keywordName, Arrays.toString(args), rank);
        return rank;
    }

    @Override public String[] getArgumentNames() {
        final int min = Collections.min(keywordMap.keySet());
        final int max = Collections.max(keywordMap.keySet());
        final String[] arguments = new String[max], minNames = Iterables.get(keywordMap.get(min), 0).getArgumentNames(),
                maxNames =
                        Iterables.get(keywordMap.get(max), 0).getArgumentNames();
        for (int i = 0; i < max; i++) {
            if (i < min)
                arguments[i] = minNames[i];
            else
                arguments[i] = maxNames[i] + "=";
        }
        return arguments;
    }

    /**
     * Tests if {@link Method} has defined VARARGS argument
     *
     * @param method {@link Method} that will be tested
     * @return If {@link Method} arguments consists of VARARGS
     */
    private boolean hasVariableArgs(Method method) {
        final int argCount = method.getParameterTypes().length;
        return (argCount > 0 && method.getParameterTypes()[argCount - 1].isArray());
    }

    @Override public String getDocumentation() {
        for (Collection<CheckedKeyword> keywords : keywordMap.values()) {
            for (CheckedKeyword keyword : keywords) {
                if (!keyword.getDocumentation().isEmpty()) {
                    return keyword.getDocumentation();
                }
            }
        }
        return "";
    }

    @Override public String[] getTags() {
        Set<String> tags = new HashSet<>();
        for (Collection<CheckedKeyword> keywords : keywordMap.values()) {
            for (CheckedKeyword keyword : keywords) {
                Arrays.stream(keyword.getTags()).filter(Objects::nonNull).collect(Collectors.toCollection(() -> tags));
            }
        }
        return tags.toArray(new String[tags.size()]);
    }
}
