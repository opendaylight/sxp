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
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SXP Speaker represents the server/provider side of the distributed
 * application, because it provides data to a SXP Listener, which is the client,
 * a service requester.
 */
public final class BindingDispatcher extends Service<Void> {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingDispatcher.class.getName());

    private long updateSequenceId = 0;
    private AtomicInteger partitionSize = new AtomicInteger(0);

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
                        LOG.info("Export on dispatch");
                        for (SxpConnection connection : connections) {
                            connection.resetUpdateExported();
                        }
                        processUpdateSequence(masterDatabase, connections);
                        try {
                            masterDatabase.purgeAllDeletedBindings();
                            masterDatabase.resetModified();
                        } catch (Exception e) {
                            LOG.warn(owner + " Dispatcher clearing failed {} | {}", e.getClass().getSimpleName(),
                                    e.getMessage());
                            e.printStackTrace();
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
    }

    /**
     * Execute new Task exporting Update Messages to listeners
     * and after execution check if new Update Messages were added to export,
     * if so start again exporting.
     *
     * @param task       Task containing logic for exporting changes to SXP-DB
     * @param connection Connection on which Update Messages was received
     */
    private static void startExportPerConnection(Callable<?> task, final SxpConnection connection) {
        if (task == null) {
            return;
        }
        ListenableFuture<?> future = connection.getOutboundUpdateMessageExecutor().getAndSet(null);
        if ((future == null && !connection.hasUpdateMessageOutbound()) || (future != null || !future.isDone())) {
            ListenableFuture<?>
                    futureNew =
                    connection.getOwner().getWorker().executeTask(task, ThreadsWorker.WorkerType.OutBound);
            connection.getOwner().getWorker().addListener(futureNew, new Runnable() {

                @Override public void run() {
                    startExportPerConnection(connection.pollUpdateMessageOutbound(), connection);
                }
            });
            connection.getOutboundUpdateMessageExecutor().set(futureNew);
        } else {
            connection.pushUpdateMessageOutbound(task);
        }
    }

    private int getPartitionSize() {
        if (partitionSize.get() == 0) {
            return Math.max(2, Configuration.getConstants().getMessagesExportQuantity());
        }
        return partitionSize.get();
    }

    private void processUpdateSequence(MasterDatabaseProvider masterDatabase, List<SxpConnection> connections)
            throws Exception {

        // Compose and send new messages bundles.
        ++updateSequenceId;
        List<MasterDatabase> masterDatabasesAll = null, masterDatabasesChanged = null;
        for (final SxpConnection connection : connections) {
            if (connection.isUpdateExported()) {
                continue;

            } else if (connection.isModeBoth() && !connection.isBidirectionalBoth()) {
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
             * (delete attribute doen't contain SGT tag), the second update
             * message will delete previously added binding on a device. If
             * 1 message is exported, delete attributes are written before
             * added attributes.
             */
            final List<MasterDatabase> _masterDatabases;
            if (connection.isUpdateAllExported()) {
                if (masterDatabasesAll == null) {
                    synchronized (masterDatabase) {
                        masterDatabasesAll =
                                masterDatabase.partition(getPartitionSize(), connection.isUpdateAllExported());
                    }
                }
                _masterDatabases = masterDatabasesAll;
            } else {
                if (masterDatabasesChanged == null) {
                    synchronized (masterDatabase) {
                        masterDatabasesChanged =
                                masterDatabase.partition(getPartitionSize(), connection.isUpdateAllExported());
                    }
                }
                _masterDatabases = masterDatabasesChanged;
            }
            startExportPerConnection(new Callable<Void>() {

                @Override public Void call() throws Exception {
                    int updatePartId = 0;
                    // Get connection context.
                    ChannelHandlerContext
                            ctx =
                            connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
                    for (MasterDatabase _masterDatabase : _masterDatabases) {
                        ByteBuf
                                message =
                                connection.getContext().executeUpdateMessageStrategy(ctx, connection, _masterDatabase);

                        if (message != null) {
                            ctx.write(message);

                            ++updatePartId;

                            LOG.info("{} {}/{} UPDATEv{}(" + (connection.isUpdateAllExported() ? "C" : "A") + ") {}",
                                    connection, updateSequenceId, updatePartId, connection.getVersion().getIntValue(),
                                    MessageFactory.toString(message));
                        } else {
                            // No bindings for export in a specific version, e.g. if we
                            // are exporting subsets in legacy version 1 and 2
                            ++updatePartId;
                        }
                    }
                    // At least one message was constructed.
                    if (updatePartId > 0) {
                        ctx.flush();
                        connection.setUpdateMessageExportTimestamp();
                        connection.setUpdateAllExported();
                        connection.setUpdateExported();
                    }
                    return null;
                }
            }, connection);
        }
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
                            if (!connection.isUpdateExported()) {
                                resumedConnections.add(connection);
                            }
                        }
                        if (!resumedConnections.isEmpty()) {
                            LOG.info("Export on demand");
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
