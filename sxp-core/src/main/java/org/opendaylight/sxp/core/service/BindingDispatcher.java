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
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.ThreadsWorker;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.database.spi.MasterDatabaseProvider;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SXP Speaker represents the server/provider side of the distributed
 * application, because it provides data to a SXP Listener, which is the client,
 * a service requester.
 */
public final class BindingDispatcher extends Service<Void> {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingDispatcher.class.getName());

    private final AtomicInteger partitionSize = new AtomicInteger(0);

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

    public void dispatch() {
        LOG.debug(owner + " Starting {}", getClass().getSimpleName());

        List<SxpConnection> connections;
        MasterDatabaseProvider masterDatabase = null;
        if (owner.isEnabled()) {
            connections = owner.getAllOnSpeakerConnections();
            if (!connections.isEmpty()) {
                try {
                    masterDatabase = getBindingMasterDatabase();
                    synchronized (masterDatabase) {
                        // Expand bindings.
                        if (owner.getExpansionQuantity() > 0) {
                            masterDatabase.expandBindings(owner.getExpansionQuantity());
                        }
                        LOG.info("Export on dispatch {}", connections.size());
                        for (SxpConnection connection : connections) {
                            connection.resetUpdateExported();
                        }
                        processUpdateSequence(masterDatabase, connections);
                        try {
                            masterDatabase.purgeAllDeletedBindings();
                            masterDatabase.resetModified();
                        } catch (Exception e) {
                            LOG.warn(owner + " Dispatcher clearing failed {}", e.getClass().getSimpleName(), e);
                        }
                    }
                } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                    LOG.warn(owner + " Processing export {} | Waiting..", e.getClass().getSimpleName());
                } catch (Exception e) {
                    LOG.warn(owner + " Processing export {}", e.getClass().getSimpleName(), e);
                }
            }
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

    private void processUpdateSequence(MasterDatabaseProvider masterDatabase, List<SxpConnection> connections)
            throws Exception {

        // Compose and send new messages bundles.

        List<MasterDatabase> partitionsAllBindings = null, partitionsOnlyChangedBindings = null;
        Map<Version, MasterDatabase[]> dataPool = new HashMap<>(4);
        Map<Version, ByteBuf[]> messagesPool = new HashMap<>(4);
        Map<Version, AtomicInteger> releaseCounterPool = new HashMap<>(4);
        Map<SxpConnection, Callable> taskPool = new HashMap<>(connections.size());

        for (final SxpConnection connection : connections) {
            if (connection.isUpdateExported()) {
                continue;
            } else if (connection.isModeBoth() && !connection.isBidirectionalBoth()) {
                continue;
            }
            final Version connectionVersion = connection.getVersion();
            /*
             * Database partition: Message export quantity should be at
             * least 2! If a binding is moved to a different group on a
             * device, device does not export delete attribute, i.e. during
             * the master database partition, we have the old binding marked
             * as deleted and the new one as added. Both these changes
             * should be in one message. If 2 messages will be exported, one
             * with the added binding and the second with deleted binding
             * (delete attribute doen't contain SGT tag), the second update
             * message will delete previously added binding on a device. If
             * 1 message is exported, delete attributes are written before
             * added attributes.
             */
            if (connection.isUpdateAllExported()) {
                if (partitionsAllBindings == null) {
                    synchronized (masterDatabase) {
                        partitionsAllBindings =
                                masterDatabase.partition(getPartitionSize(), connection.isUpdateAllExported());
                    }
                }
                if (partitionsAllBindings.isEmpty()) {
                    continue;
                }
                if (dataPool.get(connectionVersion) == null) {
                    dataPool.put(connectionVersion,
                            partitionsAllBindings.toArray(new MasterDatabase[partitionsAllBindings.size()]));
                    messagesPool.put(connectionVersion, new ByteBuf[partitionsAllBindings.size()]);
                    releaseCounterPool.put(connectionVersion, new AtomicInteger(0));
                }
            } else {
                if (partitionsOnlyChangedBindings == null) {
                    synchronized (masterDatabase) {
                        partitionsOnlyChangedBindings =
                                masterDatabase.partition(getPartitionSize(), connection.isUpdateAllExported());
                    }
                }
                if (partitionsOnlyChangedBindings.isEmpty()) {
                    continue;
                }
                if (dataPool.get(connectionVersion) == null) {
                    dataPool.put(connectionVersion, partitionsOnlyChangedBindings.toArray(
                            new MasterDatabase[partitionsOnlyChangedBindings.size()]));
                    messagesPool.put(connectionVersion, new ByteBuf[partitionsOnlyChangedBindings.size()]);
                    releaseCounterPool.put(connectionVersion, new AtomicInteger(0));
                }
            }

            final AtomicInteger releaseCounter = releaseCounterPool.get(connectionVersion);

            releaseCounter.incrementAndGet();
            connection.setUpdateExported();
            Callable
                    task =
                    createNewExportTask(connection, messagesPool.get(connectionVersion), dataPool.get(connectionVersion),
                            releaseCounter);
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

    private Callable createNewExportTask(final SxpConnection connection, final ByteBuf[] generatedMessages,
            final MasterDatabase[] partitions, final AtomicInteger messagesReleaseCounter) {
        return new Callable() {

            @Override public Void call() throws Exception {
                // Get connection context.
                ChannelHandlerContext
                        ctx =
                        connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
                //Generate messages
                for (int i = 0; i < partitions.length; i++) {
                    MasterDatabase data;
                    synchronized (partitions) {
                        data = partitions[i];
                        partitions[i] = null;
                    }
                    if (data != null) {
                        ByteBuf message = connection.getContext().executeUpdateMessageStrategy(ctx, connection, data);
                        synchronized (generatedMessages) {
                            generatedMessages[i] = message;
                            generatedMessages.notifyAll();
                        }
                    }
                }
                //Wait for all messages to be generated and then write them to pipeline
                for (int i = 0; i < generatedMessages.length; i++) {
                    ByteBuf message;
                    do {
                        synchronized (generatedMessages) {
                            if ((message = generatedMessages[i]) == null) {
                                generatedMessages.wait();
                            }
                        }
                    } while (message == null);
                    ctx.write(message.retain());
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("{} {} UPDATEv{}(" + (connection.isUpdateAllExported() ? "C" : "A") + ") {}",
                                connection, i, connection.getVersion().getIntValue(), MessageFactory.toString(message));
                    }
                }
                //Send all messages
                try {
                    ctx.flush();
                    connection.setUpdateMessageExportTimestamp();
                    connection.setUpdateAllExported();
                } catch (Exception e) {
                    connection.resetUpdateExported();
                }
                //Free weak references
                if (messagesReleaseCounter.decrementAndGet() == 0) {
                    for (int i = 0; i < generatedMessages.length; i++) {
                        generatedMessages[i].release();
                    }
                }
                return null;
            }
        };
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
        LOG.debug(owner + " Starting {}", getClass().getSimpleName());

        List<SxpConnection> connections;
        MasterDatabaseProvider masterDatabase = null;

        if (owner.isEnabled()) {
            connections = owner.getAllOnSpeakerConnections();
            if (!connections.isEmpty()) {
                try {
                    masterDatabase = getBindingMasterDatabase();
                    // At least one connection wasn't exported (a connection was
                    // down and it's up again).
                    synchronized (masterDatabase) {
                        List<SxpConnection> resumedConnections = new ArrayList<SxpConnection>();
                        for (SxpConnection connection : connections) {
                            if (!connection.isUpdateAllExported() && connection.getOutboundMonitor().get() == 0) {
                                resumedConnections.add(connection);
                            }
                        }
                        if (!resumedConnections.isEmpty()) {
                            LOG.info("Export on demand {}/{} ", resumedConnections.size(), connections.size());
                            processUpdateSequence(masterDatabase, resumedConnections);
                        }
                    }
                } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                    LOG.warn(owner + " Processing export {} | Waiting..", e.getClass().getSimpleName());
                } catch (Exception e) {
                    LOG.warn(owner + " Processing export {} | {}", e.getClass().getSimpleName(), e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
