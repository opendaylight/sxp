/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpConnection.ChannelHandlerContextType;
import org.opendaylight.sxp.core.SxpNode;
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
public final class BindingDispatcher extends Service {

    protected static final Logger LOG = LoggerFactory.getLogger(BindingDispatcher.class.getName());

    private AtomicBoolean dispatch = new AtomicBoolean();

    private long updateSequenceId = 0;

    public BindingDispatcher(SxpNode owner) {
        super(owner);
    }

    public void dispatch() {
        this.dispatch.set(true);
    }

    private void processUpdateSequence(MasterDatabaseProvider masterDatabase, List<SxpConnection> connections)
            throws Exception {

        // Compose and send new messages bundles.
        ++updateSequenceId;
        for (SxpConnection connection : connections) {
            if (connection.isUpdateExported()) {
                continue;

            } else if (connection.isModeBoth() && !connection.isBidirectionalBoth()) {
                continue;
            }

            // Database partition.
            List<MasterDatabase> _masterDatabases;

            // Database.
            synchronized (masterDatabase) {
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
                int quantity = Configuration.getConstants().getMessagesExportQuantity();
                if (quantity < 2) {
                    quantity = 2;
                }

                _masterDatabases = masterDatabase.partition(quantity, connection.isUpdateAllExported());
            }

            int updatePartId = 0;
            // Get connection context.
            ChannelHandlerContext ctx = null;
            if (connection.isModeBoth()) {
                ctx = connection.getChannelHandlerContext(ChannelHandlerContextType.SpeakerContext);
            } else {
                ctx = connection.getChannelHandlerContext();
            }

            for (MasterDatabase _masterDatabase : _masterDatabases) {
                ByteBuf message = connection.getContext()
                        .executeUpdateMessageStrategy(ctx, connection, _masterDatabase);

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
        }
    }

    @Override
    public void run() {
        LOG.debug(owner + " Starting {}", getClass().getSimpleName());

        List<SxpConnection> connections;
        MasterDatabaseProvider masterDatabase = null;

        while (!finished) {
            try {
                Thread.sleep(THREAD_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            if (!owner.isEnabled()) {
                continue;
            }

            connections = owner.getAllOnSpeakerConnections();
            if (connections.isEmpty()) {
                continue;
            }

            int exportedAmount = -1;
            // Detect database modification.
            try {
                // Notified or the first run.
                if (dispatch.get() || masterDatabase == null) {
                    dispatch.set(false);

                    masterDatabase = getBindingMasterDatabase();
                    synchronized (masterDatabase) {
                        exportedAmount = masterDatabase.readBindings().size();

                        // Expand bindings.
                        if (owner.getExpansionQuantity() > 0) {
                            masterDatabase.expandBindings(owner.getExpansionQuantity());
                        }

                        LOG.info("Export on dispatch");
                        for (SxpConnection connection : connections) {
                            connection.resetUpdateExported();
                        }

                        processUpdateSequence(masterDatabase, connections);
                    }
                }
                // At least one connection wasn't exported (a connection was
                // down and it's up again).
                if (masterDatabase != null) {
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
                }

            } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                LOG.warn(owner + " Processing export {} | Waiting..", e.getClass().getSimpleName());
                dispatch.set(true);
                continue;
            } catch (Exception e) {
                LOG.warn(owner + " Processing export {} | {}", e.getClass().getSimpleName(), e.getMessage());
                e.printStackTrace();
                continue;
            }

            // Speaker databases clearing: Remove deleted bindings.
            boolean databaseClearing = true;
            for (SxpConnection connection : connections) {
                if (!connection.isUpdateExported()) {
                    databaseClearing = false;
                }
            }
            if (databaseClearing && masterDatabase != null) {
                try {
                    synchronized (masterDatabase) {
                        int clearingAmount = masterDatabase.readBindings().size();
                        // In some cases the master database is updated
                        // after an export, i.e. wait until the next export
                        // finishes and amount of bindings will be the same.
                        if (exportedAmount == clearingAmount) {
                            masterDatabase.purgeAllDeletedBindings();
                            masterDatabase.resetModified();
                        }
                    }
                } catch (Exception e) {
                    LOG.warn(owner + " Dispatcher clearing failed {} | {}", e.getClass().getSimpleName(),
                            e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
        }
        LOG.info(owner + " Shutdown {}", getClass().getSimpleName());
    }
}
