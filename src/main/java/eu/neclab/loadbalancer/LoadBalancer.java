/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.NetworkInterface;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcap4j.packet.Packet;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public interface LoadBalancer {

    /**
     * Add the given VTN data flow.
     *
     * @param iface A {@link NetworkInterface} instance of the
     * interface to be attached.
     * @return  boolean operation result
     */
    public boolean addServer(String iface);

    public boolean removeServer(String iface);

    public String getServerPoolStr();

    public List<Server> getServerPool();

    public int getQueueNum();

    public ConcurrentLinkedQueue<Packet> getPacketQueue(int index);

    public boolean stopLoadBalancer();


}
