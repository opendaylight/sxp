/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.core.service;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.exception.message.UpdateMessageCompositionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UpdateExportTask class contains logic for Binding export
 */
public final class UpdateExportTask implements Callable<Void> {

        protected static final Logger LOG = LoggerFactory.getLogger(UpdateExportTask.class.getName());

        private final SxpConnection connection;
        private final ByteBuf[] generatedMessages;
        private final MasterDatabase[] partitions;
        private final AtomicInteger messagesReleaseCounter;

        /**
         * Creates Task which will export provided bindings to remote peer
         *
         * @param connection             Connection on which will be export hold
         * @param generatedMessages      Pool of generated messages to export
         * @param partitions             Pool of bindings from which are messages generated
         * @param messagesReleaseCounter Monitor for releasing weak references of ByteBuf
         */
        public UpdateExportTask(SxpConnection connection, ByteBuf[] generatedMessages, MasterDatabase[] partitions,
                AtomicInteger messagesReleaseCounter) {
                this.connection = Preconditions.checkNotNull(connection);
                this.generatedMessages = Preconditions.checkNotNull(generatedMessages);
                this.partitions = Preconditions.checkNotNull(partitions);
                this.messagesReleaseCounter = Preconditions.checkNotNull(messagesReleaseCounter);
        }

        @Override public Void call() {
                //Generate messages
                for (int i = 0; i < partitions.length; i++) {
                        MasterDatabase data;
                        synchronized (partitions) {
                                data = partitions[i];
                                partitions[i] = null;
                        }
                        if (data != null) {
                                ByteBuf message;
                                try {
                                        message =
                                                connection.getContext().executeUpdateMessageStrategy(connection, data);
                                } catch (UpdateMessageCompositionException e) {
                                        LOG.error("{} Error creating update message {} ", connection, data, e);
                                        message = PooledByteBufAllocator.DEFAULT.buffer(0);
                                }
                                synchronized (generatedMessages) {
                                        generatedMessages[i] = message;
                                        generatedMessages.notifyAll();
                                }
                        }
                }
                //Wait for all messages to be generated and then write them to pipeline
                try {
                        for (int i = 0; i < generatedMessages.length; i++) {
                                ByteBuf message;
                                do {
                                        synchronized (generatedMessages) {
                                                if ((message = generatedMessages[i]) == null) {
                                                        generatedMessages.wait();
                                                }
                                        }
                                } while (message == null);
                                if (message.capacity() == 0) {
                                        LOG.warn("{} Cannot export empty message aborting export", connection);
                                        connection.resetUpdateExported();
                                        freeReferences();
                                        return null;
                                }
                        }
                        for (int i = 0; i < generatedMessages.length; i++) {
                                connection.getChannelHandlerContext(
                                        SxpConnection.ChannelHandlerContextType.SpeakerContext)
                                        .writeAndFlush(generatedMessages[i].duplicate().retain());
                                if (LOG.isTraceEnabled()) {
                                        LOG.trace("{} {} UPDATEv{}(" + (connection.isUpdateAllExported() ? "C" : "A")
                                                        + ") {}", connection, i, connection.getVersion().getIntValue(),
                                                MessageFactory.toString(generatedMessages[i]));
                                }
                        }
                        connection.setUpdateMessageExportTimestamp();
                        connection.setUpdateAllExported();
                } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                        LOG.debug("{} Cannot find context aborting bindings export.", connection, e);
                        connection.resetUpdateExported();
                } catch (InterruptedException e) {
                        connection.resetUpdateExported();
                }
                freeReferences();
                return null;
        }

        /**
         * Decrease weak references on ByteBuf and if reference is zero free content of buffer
         */
        public void freeReferences() {
                if (messagesReleaseCounter.decrementAndGet() == 0) {
                        for (int i = 0; i < generatedMessages.length; i++) {
                                if (generatedMessages[i] != null) {
                                        generatedMessages[i].release();
                                }
                        }
                }
        }
}
