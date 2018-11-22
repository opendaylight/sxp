/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.restconfclient;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import javassist.ClassPool;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin Dindoffer
 */
public class JsonDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDeserializer.class);

    private final SchemaContext schemaContext;
    private final BindingToNormalizedNodeCodec codec;

    public JsonDeserializer() {
        List<YangModuleInfo> moduleInfos = loadModuleInfos();
        ModuleInfoBackedContext moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
        moduleInfoBackedCntxt.addModuleInfos(moduleInfos);

        this.schemaContext = moduleInfoBackedCntxt.getSchemaContext();
        LOG.info("Schema context created: {}", schemaContext);

        DataObjectSerializerGenerator serializerGenerator
                = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(serializerGenerator);

        BindingRuntimeContext ctx = BindingRuntimeContext.create(moduleInfoBackedCntxt, schemaContext);
        codecRegistry.onBindingRuntimeContextUpdated(ctx);
        codec = new BindingToNormalizedNodeCodec(moduleInfoBackedCntxt, codecRegistry);
        codec.onGlobalContextUpdated(schemaContext);
    }

    public Map.Entry<InstanceIdentifier<?>, DataObject> unmarshallNormalizedNode(
            NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> nn, YangInstanceIdentifier id) {
        LOG.info("Instance identifier for unmarshalling: {}", id);
        return codec.fromNormalizedNode(id, nn);
    }

    public NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> deserializeJson(
            String jsonString, SchemaPath parentPath) {
        return deserializeJson(new JsonReader(new StringReader(jsonString)), parentPath);
    }

    public NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> deserializeJson(
            JsonReader jsonReader, SchemaPath parentPath) {
        NormalizedNodeResult result = new NormalizedNodeResult();
        NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        SchemaNode parentSchema = SchemaContextUtil.findDataSchemaNode(schemaContext, parentPath);
        try (JsonParserStream parser = JsonParserStream.create(streamWriter, schemaContext, parentSchema)) {
            parser.parse(jsonReader);
        } catch (IOException ex) {
            LOG.warn("Failed to close json parser", ex);
        }
        return result.getResult();
    }

    private List<YangModuleInfo> loadModuleInfos() {
        List<YangModuleInfo> moduleInfos = new LinkedList<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            LOG.info("Processing yangModelProvider {} with module info {}",
                    yangModelBindingProvider, yangModelBindingProvider.getModuleInfo());
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        return moduleInfos;
    }
}
