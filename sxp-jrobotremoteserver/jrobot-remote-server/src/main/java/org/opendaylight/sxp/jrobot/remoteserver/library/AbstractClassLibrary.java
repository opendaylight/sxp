/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.library;

import java.util.Map;
import java.util.Objects;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.factory.KeywordFactory;
import org.robotframework.javalib.library.KeywordFactoryBasedLibrary;
import org.opendaylight.sxp.jrobot.remoteserver.RemoteServer;
import org.opendaylight.sxp.jrobot.remoteserver.keywords.OverloadedKeyword;
import org.opendaylight.sxp.jrobot.remoteserver.keywords.OverloadedKeywordExtractor;
import org.opendaylight.sxp.jrobot.remoteserver.keywords.OverloadedKeywordFactory;

/**
 * Represent extension of {@link KeywordFactoryBasedLibrary} providing
 * base on which Robot-framework libraries can be build
 */
@RobotKeywords public abstract class AbstractClassLibrary extends KeywordFactoryBasedLibrary<OverloadedKeyword>
        implements RemoteLibrary {

    private KeywordFactory<OverloadedKeyword> keywordFactory;

    /**
     * @param server Server used for registration of Robot-framework remote library
     */
    protected AbstractClassLibrary(RemoteServer server) {
        Objects.requireNonNull(server).putLibrary("/" + getURI().trim().replace(" ", "_"), this);
    }

    @Override protected synchronized KeywordFactory<OverloadedKeyword> createKeywordFactory() {
        if (keywordFactory == null) {
            keywordFactory = new OverloadedKeywordFactory(this, OverloadedKeywordExtractor.createInstance());
        }
        return keywordFactory;
    }

    @Override public synchronized Object runKeyword(String keywordName, Object[] args, Map<String, Object> kwargs) {
        if (Objects.nonNull(kwargs) && !kwargs.isEmpty()) {
            String[] argsNames = getKeywordArguments(keywordName);
            Object[] argsNew = new Object[argsNames.length];
            for (int i = 0; i < argsNames.length; i++) {
                if (kwargs.containsKey(argsNames[i])) {
                    argsNew[i] = kwargs.get(argsNames[i]);
                } else if (i < args.length) {
                    argsNew[i] = args[i];
                }
            }
            return runKeyword(keywordName, argsNew);
        }
        return runKeyword(keywordName, args);
    }

    @Override public synchronized String[] getKeywordArguments(String keywordName) {
        return createKeywordFactory().createKeyword(keywordName).getArgumentNames();
    }

    @Override public synchronized String[] getKeywordTags(String keywordName) {
        return createKeywordFactory().createKeyword(keywordName).getTags();
    }

    @Override public synchronized String getKeywordDocumentation(String keywordName) {
        return createKeywordFactory().createKeyword(keywordName).getDocumentation();
    }

    @Override public abstract String getURI();

    @RobotKeyword public void libraryCleanup() {
        close();
    }
}
