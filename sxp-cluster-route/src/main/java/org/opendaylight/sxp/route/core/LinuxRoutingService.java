/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.route.core;

import static org.opendaylight.sxp.route.util.RouteUtil.addressToString;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opendaylight.sxp.route.spi.Routing;
import org.opendaylight.sxp.route.spi.SystemCall;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.cluster.route.rev161212.sxp.cluster.route.RoutingDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: reflect configuration of virtual address routing to network via Linux system calls
 */
public class LinuxRoutingService implements Routing {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxRoutingService.class);
    private final SystemCall processFunction;
    private final IpAddress virtualIp;
    private String interfaceName;
    private IpAddress netmask;
    private boolean isRouteActive = false;

    /**
     * @param systemCall  service providing system callbacks
     * @param initializer configured {@link RoutingDefinition}
     */
    public LinuxRoutingService(SystemCall systemCall, RoutingDefinition initializer) {
        Objects.requireNonNull(initializer);
        this.processFunction = Objects.requireNonNull(systemCall);
        this.virtualIp = Objects.requireNonNull(initializer.getIpAddress());
        this.netmask = Objects.requireNonNull(initializer.getNetmask());
        this.interfaceName = Objects.requireNonNull(initializer.getInterface());
    }

    /**
     * @param command Command that will be executed
     * @return Command stdout
     */
    public String executeCommand(String command) {
        LOG.debug("Execute CMD {}", command);
        try {
            final Process p = processFunction.execute(command);
            p.waitFor(3, TimeUnit.SECONDS);
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return buffer.lines().collect(Collectors.joining("\n"));
            }
        } catch (InterruptedException | IOException e) {
            return "";
        }
    }

    /**
     * @param command Command that will be executed
     * @return Exit value of executed command
     */
    public int executeCommandRC(String command) {
        LOG.debug("Execute CMD : {}", command);
        try {
            final Process p = processFunction.execute(command);
            p.waitFor(3, TimeUnit.SECONDS);
            LOG.debug("Executed CMD : {} --> RC={}", command, p.exitValue());
            return p.exitValue();
        } catch (InterruptedException | IOException e) {
            return 1;
        }
    }

    @Override
    public synchronized Routing setInterface(String inf) {
        if (Objects.requireNonNull(inf).equals(interfaceName)) {
            return this;
        }
        interfaceName = inf;
        return this;
    }

    @Override
    public Routing setNetmask(final IpAddress netmask) {
        if (Objects.requireNonNull(netmask).equals(this.netmask)) {
            return this;
        }
        this.netmask = netmask;
        return this;
    }

    @Override
    public IpAddress getNetmask() {
        return netmask;
    }

    @Override
    public synchronized String getInterface() {
        return interfaceName;
    }

    @Override
    public synchronized IpAddress getVirtualIp() {
        return virtualIp;
    }

    @Override
    public synchronized boolean isActive() {
        return isRouteActive;
    }

    @Override
    public synchronized boolean removeRouteForCurrentService() {
        boolean result = (executeCommandRC(createIfaceUnSetIpCmd()) == 0);
        if (result) {
            isRouteActive = false;
        }
        return result;
    }

    /**
     * @return Command for destroying virtual ip-address
     */
    @VisibleForTesting
    String createIfaceUnSetIpCmd() {
        return String.format("sudo ip addr del %s/%s dev %s",
                addressToString(virtualIp), addressToString(netmask), interfaceName);
    }

    @Override
    public synchronized boolean addRouteForCurrentService() {
        if (isRouteActive) {
            return true;
        } else if (executeCommand("sudo ip addr show").contains("inet " + addressToString(virtualIp))) {
            isRouteActive = true;
            return (true);
        }
        return (executeCommandRC(createIfaceSetIpCmd()) == 0);
    }

    @Override
    public boolean updateArpTableForCurrentService() {
        return executeCommandRC(createPingArpCmd()) == 0;
    }

    /**
     * @return Command for creating virtual ip-address
     */
    @VisibleForTesting
    String createIfaceSetIpCmd() {
        return String.format("sudo ip addr add %s/%s dev %s",
                addressToString(virtualIp), addressToString(netmask), interfaceName);
    }

    /**
     * @return Command for update of device arp table
     */
    @VisibleForTesting
    String createPingArpCmd() {
        // TODO: Replace by specific ip addresses
        return String.format("sudo arping -q -c 1 -S %s -i %s -B", addressToString(virtualIp),
                interfaceName.split(":")[0]);
    }

    @Override
    public String toString() {
        return "LinuxRoute [" + "virtualIp=" + addressToString(virtualIp) + ", interfaceName=" + interfaceName
                + ", netmask=" + addressToString(netmask) + ", active=" + isRouteActive + ']';
    }
}
