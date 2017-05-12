/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.Inet4Address;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcap4j.packet.IpV4Packet;
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
    private void balanceIpv4Packet(IpV4Packet pack){
        int poolsize = lbProvider.getServerPoolSize();
        if (poolsize != 0){
        //implementing src.ip hashing
        //TODO implement (src.ip, src.port) hashing
        Inet4Address srcip  = pack.getHeader().getSrcAddr();
        int srcHash  = srcip.hashCode();
        long unsrcHash = srcHash & 0x00000000ffffffffL;
        int servNum = (int) (unsrcHash % poolsize);
//        LOG.info("Accessing server number: {} pool size: {} srcHash {}",
//                servNum, poolsize, unsrcHash);
        String ServDstIp = lbProvider.
                getServerPool().get(servNum).getAddress();
        LOG.info("LB: ip.src: {} => dst.server: {}", srcip, ServDstIp);
        //IpV4Packet newpack = IpV4Packet();


    } else {
        LOG.error("Empty server pool cannot load-balance");
    }
}
    @Override
    public void run() {

        int numq = lbProvider.getQueueNum();
        while (true){
            //Implementing strict queuing
            //TODO implement WFQ
            for ( int i=0; i<numq; i++){
                ConcurrentLinkedQueue<Packet> queue = lbProvider.getPacketQueue(i);
                while (!queue.isEmpty()){
                    Packet pack = queue.poll();
                    if (pack != null){
                        LOG.info("Got packet from the queue: {} ", i);
                        balanceIpv4Packet(pack.get(IpV4Packet.class));
                    }
                }
            }
        }
    }
}
