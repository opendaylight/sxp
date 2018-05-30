package org.opendaylight.sxp.core.hazelcast;

import java.util.List;
import org.opendaylight.sxp.core.SxpConnection;
import org.opendaylight.sxp.core.SxpDomain;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

public class MasterDBListener {

    private final BindingDispatcher bindingDispatcher;
    private final SxpDomain domain;

    public MasterDBListener(BindingDispatcher bindingDispatcher, SxpDomain domain) {
        this.bindingDispatcher = bindingDispatcher;
        this.domain = domain;
    }

    public void onBindingsAdded(List<MasterDatabaseBinding> added) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(null, added, allOnSpeakerConnections);
    }

    public void onBindingsRemoved(List<MasterDatabaseBinding> removed) {
        List<SxpConnection> allOnSpeakerConnections = bindingDispatcher.getOwner()
                .getAllOnSpeakerConnections(domain.getName());
        bindingDispatcher.propagateUpdate(removed, null, allOnSpeakerConnections);
    }

}
