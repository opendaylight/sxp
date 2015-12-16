/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.buffer.ByteBuf;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.ThreadsWorker;
import org.opendaylight.sxp.util.ExportKey;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseInf;
import org.opendaylight.sxp.util.exception.node.DatabaseAccessException;
import org.opendaylight.sxp.util.exception.unknown.UnknownPrefixException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SXP Speaker represents the server/provider side of the distributed
 * application, because it provides data to a SXP Listener, which is the client,
 * a service requester.
 */
public final class BindingDispatcher extends Service<Void> {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingDispatcher.class.getName());

    private final AtomicInteger partitionSize = new AtomicInteger(0);

    /**
     * Default constructor that sets SxpNode
     * 
     * @param owner SxpNode to be set
     */
    public BindingDispatcher(SxpNode owner) {
        super(owner);
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

    /**
     * Exports change from MasterDatabase to all On SxpConnections
     */
    public void dispatch() {
        if (!owner.isEnabled()) {
            return;
        }
        List<SxpConnection> connections = owner.getAllOnSpeakerConnections();
        try {
            synchronized (getBindingMasterDatabase()) {
                if (!connections.isEmpty()) {
                    LOG.debug(owner + " Starting {}", getClass().getSimpleName());
                    // Expand bindings.
                    if (owner.getExpansionQuantity() > 0) {
                        getBindingMasterDatabase().expandBindings(owner.getExpansionQuantity());
                    }
                    LOG.info("Export on dispatch {} {}", owner.getNodeId(), connections.size());
                    for (SxpConnection connection : connections) {
                        connection.resetUpdateExported();
                    }
                    processUpdateSequence(getBindingMasterDatabase(), connections);
                }
                getBindingMasterDatabase().purgeAllDeletedBindings();
                getBindingMasterDatabase().resetModified();
            }
        } catch (UnknownPrefixException | UnknownHostException | DatabaseAccessException e) {
            LOG.warn(owner + " Processing export {}", e.getClass().getSimpleName(), e);
        }
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
     * Prepares data for propagation to listeners and afterwards
     * send them to peers
     *
     * @param masterDatabase MasterDatabaseProvider containing Bindings to be exported
     * @param connections    SxpConnections on which the export will be performed
     */
    private void processUpdateSequence(MasterDatabaseInf masterDatabase, List<SxpConnection> connections)
            throws DatabaseAccessException {

        // Compose and send new messages bundles.

        Map<ExportKey, MasterDatabase[]> dataPool = new HashMap<>(4);
        Map<ExportKey, ByteBuf[]> messagesPool = new HashMap<>(4);
        Map<ExportKey, AtomicInteger> releaseCounterPool = new HashMap<>(4);
        Map<SxpConnection, Callable> taskPool = new HashMap<>(connections.size());

        for (final SxpConnection connection : connections) {
            if (connection.isUpdateExported()) {
                continue;
            }
            /*
             * Database partition: Message export quantity should be at
             * least 2! If a binding is moved to a different group on a
             * device, device does not export delete attribute, i.e. during
             * the master database partition, we have the old binding marked
             * as deleted and the new one as added. Both these changes
             * should be in one message. If 2 messages will be exported, one
             * with the added binding and the second with deleted binding
             * (delete attribute doesn't contain SGT tag), the second update
             * message will delete previously added binding on a device. If
             * 1 message is exported, delete attributes are written before
             * added attributes.
             */
            ExportKey
                    key =
                    new ExportKey(connection.getVersion(), connection.isUpdateAllExported(),
                            connection.getGroupName(FilterType.Outbound));
            if (dataPool.get(key) == null) {
                List<MasterDatabase> partitions;
                partitions =
                        masterDatabase.partition(getPartitionSize(), connection.isUpdateAllExported(),
                                connection.getFilter(FilterType.Outbound));
                dataPool.put(key, partitions.toArray(new MasterDatabase[partitions.size()]));
                messagesPool.put(key, new ByteBuf[partitions.size()]);
                releaseCounterPool.put(key, new AtomicInteger(0));
            }

            AtomicInteger releaseCounter = releaseCounterPool.get(key);
            releaseCounter.incrementAndGet();
            connection.setUpdateExported();
            Callable task = new UpdateExportTask(connection, messagesPool.get(key), dataPool.get(key), releaseCounter);
            taskPool.put(connection, task);
        }
        //Start exporting messages
        for (Map.Entry<SxpConnection, Callable> entry : taskPool.entrySet()) {
            if (entry.getKey().getOutboundMonitor().getAndIncrement() == 0) {
                startExportPerConnection(entry.getValue(), entry.getKey());
            } else {
                entry.getKey().pushUpdateMessageOutbound(entry.getValue());
            }
        }
    }

    /**
     * Execute new Task exporting Update Messages to listeners
     * and after execution check if new Update Messages were added to export,
     * if so start exporting again.
     *
     * @param task       Task containing logic for exporting changes to SXP-DB
     * @param connection Connection on which Update Messages was received
     */
    private static void startExportPerConnection(Callable<?> task, final SxpConnection connection) {
        if (task == null) {
            return;
        }
        ListenableFuture
                future =
                connection.getOwner().getWorker().executeTask(task, ThreadsWorker.WorkerType.OUTBOUND);
        connection.getOwner().getWorker().addListener(future, new Runnable() {

            @Override public void run() {
                if (connection.getOutboundMonitor().decrementAndGet() > 0) {
                    startExportPerConnection(connection.pollUpdateMessageOutbound(), connection);
                }
            }
        });
    }

    @Override
    public Void call() {
        List<SxpConnection> connections = owner.getAllOnSpeakerConnections();
        if (!owner.isEnabled() || connections.isEmpty()) {
            return null;
        }
        LOG.debug(owner + " Starting {}", getClass().getSimpleName());
        // At least one connection wasn't exported (a connection was
        // down and it's up again).
        synchronized (getBindingMasterDatabase()) {
            List<SxpConnection> resumedConnections = new ArrayList<>();
            for (SxpConnection connection : connections) {
                if (!connection.isUpdateAllExported() && connection.getOutboundMonitor().get() == 0) {
                    resumedConnections.add(connection);
                }
            }
            if (!resumedConnections.isEmpty()) {
                LOG.info("Export on demand {} {}/{} ", owner.getNodeId(), resumedConnections.size(),
                        connections.size());
                try {
                    processUpdateSequence(getBindingMasterDatabase(), resumedConnections);
                } catch (DatabaseAccessException e) {
                    LOG.warn("{} Processing export ", owner, e);
                }
            }
        }
        return null;
    }
}
