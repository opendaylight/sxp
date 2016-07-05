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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.messaging.MessageFactory;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextDiscrepancyException;
import org.opendaylight.sxp.util.exception.connection.ChannelHandlerContextNotFoundException;
import org.opendaylight.sxp.util.filtering.SxpBindingFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UpdateExportTask class contains logic for Binding export
 */
public final class  UpdateExportTask implements Callable<Void> {

        protected static final Logger LOG = LoggerFactory.getLogger(UpdateExportTask.class.getName());

        private final SxpConnection connection;
        private final ByteBuf[] generatedMessages;
        private final BiFunction<SxpConnection,SxpBindingFilter,ByteBuf>[] partitions;
        private final AtomicInteger messagesReleaseCounter;

        /**
         * Creates Task which will export provided bindings to remote peer
         *
         * @param connection             Connection on which will be export hold
         * @param generatedMessages      Pool of generated messages to export
         * @param partitions             Pool of bindings from which are messages generated
         * @param messagesReleaseCounter Monitor for releasing weak references of ByteBuf
         */
        public UpdateExportTask(SxpConnection connection, ByteBuf[] generatedMessages,
                BiFunction<SxpConnection, SxpBindingFilter, ByteBuf>[] partitions,
                AtomicInteger messagesReleaseCounter) {
                this.connection = Preconditions.checkNotNull(connection);
                this.generatedMessages = Preconditions.checkNotNull(generatedMessages);
                this.partitions = Preconditions.checkNotNull(partitions);
                this.messagesReleaseCounter = Preconditions.checkNotNull(messagesReleaseCounter);
        }

        public SxpConnection getConnection(){
                return connection;
        }

        @Override public Void call() {
                //Generate messages
                for (int i = 0; i < partitions.length; i++) {
                        BiFunction<SxpConnection,SxpBindingFilter,ByteBuf> data;
                        synchronized (partitions) {
                                data = partitions[i];
                                partitions[i] = null;
                        }
                        if (data != null) {
                                ByteBuf message = data.apply(connection, connection.getFilter(FilterType.Outbound));
                                if (message == null) {
                                        LOG.error("{} Generated empty partition.", connection);
                                        return null;
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
                                        freeReferences();
                                        return null;
                                }
                        }
                        for (int i = 0; i < generatedMessages.length; i++) {
                                connection.getChannelHandlerContext(
                                        SxpConnection.ChannelHandlerContextType.SpeakerContext)
                                        .writeAndFlush(generatedMessages[i].duplicate().retain());
                                if (LOG.isTraceEnabled()) {
                                        LOG.trace("{} {} UPDATEv{} {}", connection, i, connection.getVersion().getIntValue(),
                                                MessageFactory.toString(generatedMessages[i]));
                                }
                        }
                        connection.setUpdateOrKeepaliveMessageTimestamp();
                } catch (ChannelHandlerContextNotFoundException | ChannelHandlerContextDiscrepancyException e) {
                        LOG.warn("{} Cannot find context aborting bindings export.", connection);
                } catch (InterruptedException e) {
                        LOG.warn("{} Bindings export canceled.", connection, e);
                }
                freeReferences();
                return null;
        }

        /**
         * Decrease weak references on ByteBuf and if reference is zero free content of buffer
         */
        public void freeReferences() {
                if (messagesReleaseCounter.decrementAndGet() == 0) {
                        for (ByteBuf generatedMessage : generatedMessages) {
                                if (generatedMessage != null) {
                                        generatedMessage.release();
                                }
                        }
                }
        }
}
