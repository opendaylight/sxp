/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywordOverload;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing implementation of {@link KeywordExtractor}
 */
public class OverloadedKeywordExtractor implements KeywordExtractor<OverloadedKeyword> {

    protected static final Logger LOG = LoggerFactory.getLogger(OverloadedKeywordExtractor.class.getName());
    private static OverloadedKeywordExtractor singleton;

    /**
     * NOP Constructor
     */
    protected OverloadedKeywordExtractor() {
    }

    /**
     * @return Singleton instance of {@link KeywordExtractor} of {@link OverloadedKeyword}
     */
    public static synchronized OverloadedKeywordExtractor createInstance() {
        if (singleton == null) {
            singleton = new OverloadedKeywordExtractor();
        }
        return singleton;
    }

    /**
     * Extracts {@link Method} from provided {@link Class} and its implemented interfaces
     *
     * @param obj {@link Class} definition used for {@link Method} extraction
     * @return Extracted {@link Method}
     */
    public static Stream<Method> getMethods(Class<?> obj) {
        if (obj == null) {
            return Stream.empty();
        }
        Stream<Method> methods = Stream.of(obj.getMethods());
        for (Class<?> i : obj.getInterfaces()) {
            methods = Stream.concat(methods, getMethods(i));
        }
        return Stream.concat(methods, getMethods(obj.getSuperclass()));
    }

    /**
     * Converts {@link org.robotframework.javalib.keyword.Keyword} value to
     * Camelcase format
     *
     * @param s {@link String} that will be normalized
     * @return Normalized {@link String} to lower Camelcase
     */
    public static String normalize(String s) {
        if (!s.contains("_") && !s.contains(" ") && !s.contains("-")) {
            return s.trim();
        }
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, s.trim().replace(' ', '_').replace('-', '_'));
    }

    @Override public Map<String, OverloadedKeyword> extractKeywords(final RemoteLibrary keywordBean) {
        Objects.requireNonNull(keywordBean);
        final Map<String, OverloadedKeyword> overloadableKeywords = new HashMap<>();
        final Map<Method, String> methodToKW = new HashMap<>();
        final Set<String> inheritedKeywords = new HashSet<>(), inheritedKeywordsOverload = new HashSet<>();
        getMethods(keywordBean.getClass()).forEach(m -> {
            final RobotKeyword keyword = m.getAnnotation(RobotKeyword.class);
            if (Objects.nonNull(keyword)) {
                if (Objects.nonNull(Strings.emptyToNull(keyword.value()))) {
                    methodToKW.put(m, normalize(keyword.value()));
                }
                inheritedKeywords.add(methodToKW.getOrDefault(m, m.getName()));
            } else if (m.isAnnotationPresent(RobotKeywordOverload.class)) {
                inheritedKeywordsOverload.add(m.getName());
            }
        });
        final Method methods[] = keywordBean.getClass().getMethods();
        Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(RobotKeyword.class) && !m.isAnnotationPresent(
                        RobotKeywordOverload.class))
                .forEach(m -> {
                    final String kwName = methodToKW.getOrDefault(m, m.getName());
                    overloadableKeywords.put(kwName, new OverloadedKeywordImpl(keywordBean, m, kwName));
                    LOG.debug("Keyword {} extracted {}.", m.getName(), Arrays.toString(m.getParameterTypes()));
                });
        Arrays.stream(methods)
                .filter(m -> !m.isAnnotationPresent(RobotKeyword.class) && !m.isAnnotationPresent(
                        RobotKeywordOverload.class))
                .forEach(m -> {
                    final String kwName = methodToKW.getOrDefault(m, m.getName());
                    if (inheritedKeywords.contains(kwName)) {
                        if (overloadableKeywords.containsKey(kwName)) {
                            overloadableKeywords.get(kwName).addOverload(m);
                            LOG.warn("Keyword Overload {} added for in definition scope.", kwName);
                        } else {
                            overloadableKeywords.put(kwName, new OverloadedKeywordImpl(keywordBean, m, kwName));
                        }
                    }
                });
        /*
         * FIXME: KW_O may generate unexpected KW if KW method name collide with non KW method,
         *        but as it will be added as Overloaded KW during execution best match for KW method will be used.
         */
        Arrays.stream(methods).filter(m -> overloadableKeywords.containsKey(m.getName())).forEach(m -> {
            if (m.isAnnotationPresent(RobotKeywordOverload.class) || inheritedKeywordsOverload.contains(m.getName())) {
                overloadableKeywords.get(m.getName()).addOverload(m);
                LOG.debug("Keyword Overload {} extracted {}.", m.getName(), Arrays.toString(m.getParameterTypes()));
            }
        });
        return overloadableKeywords;
    }

}
