/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public class BalancerCore implements Runnable {

    /**
     * Logger instance.
     */
    static final Logger LOG = LoggerFactory.getLogger(BalancerCore.class);

    private final String beIface;

    private final LoadBalancer lbProvider;

    public BalancerCore(LoadBalancer lb, String iface){
        beIface = iface;
        lbProvider = lb;

    }

    @Override
    public void run() {

        int numq = lbProvider.getQueueNum();
        while (true){
            for ( int i=0; i<numq; i++){
                ConcurrentLinkedQueue<Packet> queue = lbProvider.getPacketQueue(i);
                Packet pack = queue.poll();
                if (pack != null){
                    LOG.info("Got packet from the queue: {} packet: {}",i, pack.toString());
                }
            }
        }
    }
}
