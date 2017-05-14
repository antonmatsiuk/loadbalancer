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
import org.pcap4j.packet.IpV4Packet.Builder;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.MacAddress;
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

    private Server balanceIpv4Packet(IpV4Packet pack){
        Server serv = null;
        int poolsize = lbProvider.getServerPoolSize();
        //implementing src.ip hashing
        //TODO implement (src.ip, src.port) hashing
        if (poolsize != 0){
            Inet4Address srcip  = pack.getHeader().getSrcAddr();
            int srcHash  = srcip.hashCode();
            long unsrcHash = srcHash & 0x00000000ffffffffL;
            int servNum = (int) (unsrcHash % poolsize);
            serv = lbProvider.
                    getServerPool().get(servNum);
        } else {
            LOG.error("Empty server pool cannot load-balance");
        }
    return serv;
}

    @Override
    public void run() {

        int numq = lbProvider.getQueueNum();
        while (true){
            //Implements strict queuing
            //TODO implement WFQ (get n packet from n-th queue in a cycle)
            IfHandler behandler = lbProvider.getBeHandler();
            MacAddress srcmac = behandler.getIfHandlerMac();
            //get packets from queues
            for ( int i=0; i<numq; i++){
                ConcurrentLinkedQueue<Packet> queue = lbProvider.getPacketQueue(i);
                while (!queue.isEmpty()){
                    Packet pack = queue.poll();
                    if (pack != null){
                        LOG.info("Got packet from the queue: {}", i);
                        IpV4Packet ipv4pack = pack.get(IpV4Packet.class);
                        //get server
                        Server serv = balanceIpv4Packet(ipv4pack);
                        LOG.info("LB: ip.src: {} => dst.server: {}",
                                ipv4pack.getHeader().getSrcAddr(), serv.getAddress().toString());

                        //rewriting headers
                        Builder outIpv4PackBuilder = ipv4pack.getBuilder();
                        outIpv4PackBuilder.dstAddr(serv.getAddress());
                        org.pcap4j.packet.EthernetPacket.Builder outEtherBuilder =
                                (org.pcap4j.packet.EthernetPacket.Builder) pack.getBuilder();
                        outEtherBuilder.srcAddr(srcmac);
                        outEtherBuilder.dstAddr(serv.getMacAddres());
                        outEtherBuilder.payloadBuilder(outIpv4PackBuilder);
                        //send out of back-end interface
                        behandler.sendPacket(outEtherBuilder.build());
                    }
                }
            }
        }
    }
}
