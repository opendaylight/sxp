package org.opendaylight.sxp.restconfclient;

import com.google.common.base.Optional;
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
    private final ModuleInfoBackedContext moduleInfoBackedCntxt;
    private final BindingToNormalizedNodeCodec codec;

    public JsonDeserializer() {
        List<YangModuleInfo> moduleInfos = loadModuleInfos();
        moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
        moduleInfoBackedCntxt.addModuleInfos(moduleInfos);

        Optional<SchemaContext> schemaContextOpt = moduleInfoBackedCntxt.tryToCreateSchemaContext();
        this.schemaContext = schemaContextOpt.get();
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
            NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> nn) {
        YangInstanceIdentifier yiid = YangInstanceIdentifier.create(nn.getIdentifier());
        LOG.info("Instance identifier: {}", yiid);
        return codec.fromNormalizedNode(yiid, nn);
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

//    /**
//     * Read json serialized normalized node root structure and parse them into normalized nodes
//     *
//     * @return artificial normalized node holding all the top level nodes from provided stream as children. In case the
//     *         stream is empty, empty artificial normalized node is returned
//     * @throws IllegalArgumentException if content in the provided input stream is not restore-able
//     */
//    public static ContainerNode readJsonRoot(SchemaContext schemaContext, InputStream stream) {
//        // if root node, parent schema == schema context
//        return readContainerFromJson(schemaContext, stream, schemaContext,
//                new YangInstanceIdentifier.NodeIdentifier(schemaContext.getQName()));
//    }
//
//    public static ContainerNode readContainerFromJson(SchemaContext schemaContext,
//            InputStream stream,
//            SchemaNode parentSchema,
//            YangInstanceIdentifier.NodeIdentifier nodeIdentifier) {
//        DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> builder
//                = Builders.containerBuilder().withNodeIdentifier(nodeIdentifier);
//
//        NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(builder);
//
//        try (JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext, parentSchema)) {
//            JsonReader reader = new JsonReader(new InputStreamReader(stream, Charsets.UTF_8));
//            jsonParser.parse(reader);
//        } catch (IOException e) {
//            LOG.warn("Unable to close json parser. Ignoring exception", e);
//        }
//
//        return builder.build();
//    }
//
//    public static MapEntryNode readListEntryFromJson(SchemaContext schemaContext,
//            InputStream stream,
//            SchemaNode parentSchema,
//            YangInstanceIdentifier.NodeIdentifierWithPredicates withPredicates) {
//        DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> nodeBuilder
//                = Builders.mapEntryBuilder().withNodeIdentifier(withPredicates);
//
//        NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(nodeBuilder);
//        try (JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext, parentSchema)) {
//            JsonReader reader = new JsonReader(new InputStreamReader(stream, Charsets.UTF_8));
//            jsonParser.parse(reader);
//        } catch (IOException e) {
//            LOG.warn("Unable to close json parser. Ignoring exception", e);
//        }
//
//        return nodeBuilder.build();
//    }
}
