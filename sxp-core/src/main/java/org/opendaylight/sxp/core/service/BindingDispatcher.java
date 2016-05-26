/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.core.threading.ThreadsWorker;
import org.opendaylight.sxp.util.ExportKey;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.opendaylight.sxp.util.inet.Search.expandBindings;

/**
 * SXP Speaker represents the server/provider side of the distributed
 * application, because it provides data to a SXP Listener, which is the client,
 * a service requester.
 */
public final class BindingDispatcher {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingDispatcher.class.getName());

    private final AtomicInteger partitionSize = new AtomicInteger(0);
    private final ThreadsWorker worker;
    private final SxpNode owner;

    /**
     * Default constructor that sets SxpNode
     *
     * @param owner SxpNode to be set
     */
    public BindingDispatcher(SxpNode owner) {
        Preconditions.checkNotNull(owner);
        this.owner = Preconditions.checkNotNull(owner);
        worker = Preconditions.checkNotNull(owner.getWorker());
    }

    /**
     * Set max number of attributes exported in each Update Message.
     *
     * @param partitionSize Size which will be used for partitioning
     * @throws IllegalArgumentException If size of partitioning is bellow 2 or above 150
     */
    public void setPartitionSize(int partitionSize) throws IllegalArgumentException {
        if (partitionSize > 1 && partitionSize < 151) {
            this.partitionSize.set(partitionSize);
        } else {
            throw new IllegalArgumentException("Partition size must be between 2-150. Current value: " + partitionSize);
        }
    }

    private <T extends SxpBindingFields> BiFunction<SxpConnection, SxpBindingFilter, ByteBuf> generatePart(
            List<T> deleteBindings, List<T> addBindings) {
        return (connection, bindingFilter) -> {
            try {
                return connection.getContext()
                        .executeUpdateMessageStrategy(connection, deleteBindings, addBindings, bindingFilter);
            } catch (UpdateMessageCompositionException e) {
                LOG.error("{} Error creating update message {} {}", connection, deleteBindings, addBindings, e);
                return PooledByteBufAllocator.DEFAULT.buffer(0);
            }
        };
    }

    /**
     * Gets current partition size.
     *
     * @return value of partition size
     */
    private int getPartitionSize() {
        if (partitionSize.get() == 0) {
            return Math.max(2, Configuration.getConstants().getMessagesExportQuantity());
        }
        return partitionSize.get();
    }

    /**
     * Partition data based on pre configured value, expands bindings for legacy connections
     *
     * @param connection     SxpConnection for which data will be partitioned
     * @param deleteBindings Bindings for delete
     * @param addBindings    Bindings for add
     * @param <T>            Any type extending SxpBindingFields
     * @return List of Functions that will generate Byte representation of data
     */
    <T extends SxpBindingFields> List<BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>> partitionBindings(
            SxpConnection connection, List<T> deleteBindings, List<T> addBindings) {
        List<BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>> partitions = new ArrayList<>();
        List<T> lastPartition = null;
        //Prefix Expansion for legacy versions
        if (connection.isVersion4() && !connection.getCapabilitiesRemote().contains(CapabilityType.SubnetBindings)
                || connection.getVersion().getIntValue() < 3) {
            expandBindings(deleteBindings, owner.getExpansionQuantity());
            expandBindings(addBindings, owner.getExpansionQuantity());
        }
        //Split Delete Bindings
        if (deleteBindings != null && !deleteBindings.isEmpty()) {
            for (List<T> partition : Lists.partition(deleteBindings, getPartitionSize())) {
                partitions.add(generatePart(partition, null));
                lastPartition = partition;
            }
        }
        //Split Add Bindings and rest of Delete Bindings
        if (addBindings != null && !addBindings.isEmpty()) {
            int splitFactor = 0;
            if (lastPartition != null) {
                splitFactor = getPartitionSize() - lastPartition.size();
                partitions.set(partitions.size() - 1, generatePart(lastPartition,
                        splitFactor > addBindings.size() ? addBindings : addBindings.subList(0, splitFactor)));
            }
            for (List<T> partition : Lists.partition(
                    addBindings.subList(splitFactor > addBindings.size() ? 0 : splitFactor, addBindings.size()),
                    getPartitionSize())) {
                partitions.add(generatePart(null, partition));
            }
        }
        return partitions;
    }

    /**
     * Prepares data for propagation to listeners and afterwards
     * send them to peers
     *
     * @param connections SxpConnections on which the export will be performed
     */
    public <T extends SxpBindingFields> void propagateUpdate(List<T> deleteBindings, List<T> addBindings,
            List<SxpConnection> connections) {
        if ((deleteBindings == null || deleteBindings.isEmpty()) && (addBindings == null || addBindings.isEmpty()) || (
                connections == null || connections.isEmpty())) {
            return;
        }

        Map<ExportKey, BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>[]> dataPool = new HashMap<>(4);
        Map<ExportKey, ByteBuf[]> messagesPool = new HashMap<>(4);
        Map<ExportKey, AtomicInteger> releaseCounterPool = new HashMap<>(4);

        List<UpdateExportTask> exportTasks = new ArrayList<>();
        for (SxpConnection connection : connections) {
            if (!connection.isStateOn() || !connection.isModeSpeaker()) {
                continue;
            }
            ExportKey key = new ExportKey(connection);
            if (dataPool.get(key) == null) {
                List<BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>>
                        partitions =
                        partitionBindings(connection, deleteBindings, addBindings);
                //Set export data
                dataPool.put(key, partitions.toArray(new BiFunction[partitions.size()]));
                messagesPool.put(key, new ByteBuf[partitions.size()]);
                releaseCounterPool.put(key, new AtomicInteger(0));
            }

            AtomicInteger releaseCounter = releaseCounterPool.get(key);
            releaseCounter.incrementAndGet();
            exportTasks.add(new UpdateExportTask(connection, messagesPool.get(key), dataPool.get(key), releaseCounter));
        }
        exportTasks.stream()
                .forEach(e -> worker.executeTaskInSequence(e, ThreadsWorker.WorkerType.OUTBOUND, e.getConnection()));
    }

    /**
     * Add PurgeAll to queue and afterwards sends it
     *
     * @param connection SxpConnection for which PurgeAll will be send
     */
    public static ListenableFuture<Boolean> sendPurgeAllMessage(final SxpConnection connection) {
        return Preconditions.checkNotNull(connection).getOwner().getWorker().executeTaskInSequence(() -> {
            try {
                LOG.info("{} Sending PurgeAll {}", connection, connection.getNodeIdRemote());
                connection.getChannelHandlerContext(SxpConnection.ChannelHandlerContextType.SpeakerContext)
                        .writeAndFlush(MessageFactory.createPurgeAll());
                return true;
            } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                LOG.error(connection + " Cannot send PURGE ALL message | {} | ", e.getClass().getSimpleName());
                return false;
            }
        }, ThreadsWorker.WorkerType.OUTBOUND, connection);
    }
}
