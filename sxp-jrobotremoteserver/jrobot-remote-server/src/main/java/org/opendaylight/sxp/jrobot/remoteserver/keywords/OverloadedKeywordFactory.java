/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.jrobot.remoteserver.keywords;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.robotframework.javalib.factory.KeywordFactory;
import org.robotframework.javalib.util.IKeywordNameNormalizer;
import org.robotframework.javalib.util.KeywordNameNormalizer;
import org.opendaylight.sxp.jrobot.remoteserver.library.RemoteLibrary;

/**
 * Class providing implementation of {@link KeywordFactory}
 */
public class OverloadedKeywordFactory implements KeywordFactory<OverloadedKeyword> {

    private final KeywordExtractor<OverloadedKeyword> extractor;
    private final Map<String, OverloadedKeyword> keywords = new HashMap<>();
    private final IKeywordNameNormalizer keywordNameNormalizer = new KeywordNameNormalizer();

    /**
     * Constructor creating Factory used for generating {@link OverloadedKeyword}
     *
     * @param keywordBean Instance providing {@link OverloadedKeyword} definitions
     * @param extractor   Extractor used by factory for {@link OverloadedKeyword} extraction from {@link RemoteLibrary}
     */
    public OverloadedKeywordFactory(RemoteLibrary keywordBean, KeywordExtractor<OverloadedKeyword> extractor) {
        this.extractor = Objects.requireNonNull(extractor);
        extractKeywordsFromKeywordBean(Objects.requireNonNull(keywordBean));
    }

    @Override public OverloadedKeyword createKeyword(String keywordName) {
        return keywords.get(keywordNameNormalizer.normalize(keywordName));
    }

    @Override public String[] getKeywordNames() {
        return keywords.keySet().toArray(new String[keywords.size()]);
    }

    /**
     * Extract {@link OverloadedKeyword} from provided {@link RemoteLibrary} object
     *
     * @param keywordBean Instance providing {@link OverloadedKeyword} definitions
     */
    protected void extractKeywordsFromKeywordBean(RemoteLibrary keywordBean) {
        Map<String, OverloadedKeyword> extractedKeywords = extractor.extractKeywords(keywordBean);
        for (String keywordName : extractedKeywords.keySet()) {
            if (keywords.containsKey(keywordNameNormalizer.normalize(keywordName))) {
                throw new RuntimeException("Two keywords with name '" + keywordName + "' found!");
            }
            keywords.put(keywordNameNormalizer.normalize(keywordName), extractedKeywords.get(keywordName));
        }
    }
}
