/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import java.util.Map;
import org.robotframework.javalib.keyword.Keyword;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

/**
 * Provides extraction of {@link Keyword} from {@link RemoteLibrary} containing definitions of Keywords
 *
 * @param <T> Specific Keyword type
 */
public interface KeywordExtractor<T extends Keyword> {

    /**
     * Extract {@link Keyword} and verify their correctness
     *
     * @param keywordBean Instance containing definitions of Keywords
     * @return Names of {@link Keyword}  associated with their implementation
     */
    Map<String, T> extractKeywords(RemoteLibrary keywordBean);
}
