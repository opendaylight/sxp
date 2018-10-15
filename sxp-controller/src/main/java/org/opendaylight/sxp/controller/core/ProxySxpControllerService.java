/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.controller.core;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.AddPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteConnectionTemplateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteDomainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeleteNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.DeletePeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetConnectionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetNodeBindingsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.GetPeerGroupsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.SxpControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.controller.rev141002.UpdateFilterOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

public class ProxySxpControllerService implements SxpControllerService {

    ActorRef master;

    @Override
    public ListenableFuture<RpcResult<GetConnectionsOutput>> getConnections(GetConnectionsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeletePeerGroupOutput>> deletePeerGroup(DeletePeerGroupInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectionOutput>> deleteConnection(DeleteConnectionInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddDomainOutput>> addDomain(AddDomainInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddFilterOutput>> addFilter(AddFilterInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddDomainFilterOutput>> addDomainFilter(AddDomainFilterInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteDomainOutput>> deleteDomain(DeleteDomainInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeBindingsOutput>> getNodeBindings(GetNodeBindingsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteFilterOutput>> deleteFilter(DeleteFilterInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteNodeOutput>> deleteNode(DeleteNodeInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetPeerGroupsOutput>> getPeerGroups(GetPeerGroupsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UpdateFilterOutput>> updateFilter(UpdateFilterInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddConnectionTemplateOutput>> addConnectionTemplate(
            AddConnectionTemplateInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddBindingsOutput>> addBindings(AddBindingsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteBindingsOutput>> deleteBindings(DeleteBindingsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddPeerGroupOutput>> addPeerGroup(AddPeerGroupInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectionTemplateOutput>> deleteConnectionTemplate(
            DeleteConnectionTemplateInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddConnectionOutput>> addConnection(AddConnectionInput input) {
        Future<Object> future = Patterns.ask(master, input, Timeout.zero());

        final SettableFuture<Optional<NormalizedNode<?, ?>>> settableFuture = SettableFuture.create();
        future.onComplete(new OnComplete<Object>() {
            @Override
/*            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.debug("{}: Read {} {} failed", id, store, path, failure);
                    settableFuture.setException(processFailure(failure));
                    return;
                }

                LOG.debug("{}: Read {} {} succeeded: {}", id, store, path, response);

                if (response instanceof EmptyReadResponse) {
                    settableFuture.set(Optional.absent());
                    return;
                }

                if (response instanceof NormalizedNodeMessage) {
                    final NormalizedNodeMessage data = (NormalizedNodeMessage) response;
                    settableFuture.set(Optional.of(data.getNode()));
                }
            }
        }, executionContext);*/

        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetPeerGroupOutput>> getPeerGroup(GetPeerGroupInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteDomainFilterOutput>> deleteDomainFilter(DeleteDomainFilterInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<AddNodeOutput>> addNode(AddNodeInput input) {
        return null;
    }
}
