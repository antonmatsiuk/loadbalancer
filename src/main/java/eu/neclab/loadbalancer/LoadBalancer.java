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
import org.pcap4j.util.MacAddress;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public interface LoadBalancer {

    /**
     * @param ip An {@link String} IP address of the server
     * @param mac A {@link String} MAC address of the server
     * @return boolean operation result
     */
    public boolean addServer(String ip, String mac);

    public boolean removeServer(String iface);

    public String getServerPoolStr();

    public List<Server> getServerPool();

    public int getServerPoolSize();

    /**
     * A method to return front-end IF handler
     * @return {@link IfHandler}
     */
    public IfHandler getFeHandler();

    /**
     * A method to return back-end IF handler
     * @return {@link IfHandler}
     */
    public IfHandler getBeHandler();

    /**
     * A method to return number of QoS queues
     * @return a {@link int} number of queues
     */
    public int getQueueNum();

    /**
     * A method to return a QoS queue
     * @param a {@link int} DSCP prec value
     * @return a {@link int} number of queues
     */
    public ConcurrentLinkedQueue<Packet> getPacketQueue(int index);

    public boolean stopLoadBalancer();


}
