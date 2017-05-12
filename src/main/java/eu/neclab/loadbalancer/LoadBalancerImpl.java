/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.io.IOException;
import java.util.Collection;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc1349Tos;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.IpV4TosPrecedence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public final class LoadBalancerImpl implements LoadBalancer{

    /**
     * Logger instance.
     */
    static final Logger LOG = LoggerFactory.getLogger(LoadBalancerImpl.class);
    /**
     * Server pool.
     */
    private final List<Server> serverPool;

    private int serverPoolSize;

    private final List<ConcurrentLinkedQueue> packetQueues;

    /**
     * Front-end interface of the load balancer.
     */
    //private final InetSocketAddress frontIf;

    //private final InetSocketAddress backIf;

    private IfHandler feHandler;
    private IfHandler beHandler;

    @Override
    public ConcurrentLinkedQueue<Packet> getPacketQueue(int index){
        return packetQueues.get(index);

    }

    @Override
    public int getQueueNum(){
        return  packetQueues.size();
    }

    PacketListener beListener = new PacketListener() {
        @Override
        public void gotPacket(Packet packet) {
            IpV4Packet ipv4Packet = packet.get(IpV4Packet.class);
            IpV4Rfc1349Tos tosBits = (IpV4Rfc1349Tos) ipv4Packet.getHeader().getTos();
            IpV4TosPrecedence precBits = tosBits.getPrecedence();
            int tosClass = precBits.value().intValue();
            Inet4Address srcAddr = ipv4Packet.getHeader().getSrcAddr();
            Inet4Address dstAddr = ipv4Packet.getHeader().getDstAddr();
            LOG.info("Got a packet src.ip={} dst.ip= {} prec ={}",
                    srcAddr.toString(), dstAddr.toString(), tosClass);

        }
      };


    PacketListener feListener = new PacketListener() {
        @Override
        public void gotPacket(Packet packet) {
            IpV4Packet ipv4Packet = packet.get(IpV4Packet.class);
            IpV4Rfc1349Tos tosBits = (IpV4Rfc1349Tos) ipv4Packet.getHeader().getTos();
            IpV4TosPrecedence precBits = tosBits.getPrecedence();
            int tosClass = precBits.value().intValue();
            Inet4Address srcAddr = ipv4Packet.getHeader().getSrcAddr();
            Inet4Address dstAddr = ipv4Packet.getHeader().getDstAddr();
            LOG.info("FE: src.ip={} dst.ip= {} prec ={}",
                    srcAddr.toString(), dstAddr.toString(), tosClass);
            //put a packet to a particular queue
            ConcurrentLinkedQueue<Packet> queue = getPacketQueue(tosClass);
            queue.add(packet);
        }
      };

    private InetSocketAddress bindSocket(String ipaddr, int port){
        InetSocketAddress sockaddr = null;
        if (ipaddr !=null){
            Socket so = new Socket();
            LOG.info("Binding to IP:{} port:{}", ipaddr, port);
            sockaddr = new InetSocketAddress(ipaddr, port);
            try {
                so.bind(sockaddr);
                //so.connect(new InetSocketAddress(ipaddr, 80));
            } catch (IOException e) {
                LOG.error("Can't bind to interface:{} ",ipaddr, e);
            }
        } else {
            LOG.error("Null IP address provided");
        }
        return sockaddr;
    }

    /**
     * Public constructor.
     */
    public LoadBalancerImpl(String feip, String beip, int port, String proto, int qnum){
        serverPool = new ArrayList<>();
        packetQueues = new ArrayList<>();
        serverPoolSize = 0;
        for (int i=0; i<qnum; i++){
            ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();
            packetQueues.add(queue);
        }

        //TODO Check IP syntax
        try {
            //start a front-end packet handler
            //String filter = new String("ip src net "+feip+ " and tcp dst port "+port);
            LOG.info("Starting front-end handler");
            String feFilter = new String("ip dst net "+feip+ " and "+proto);
            feHandler = new IfHandler(this, feip, feFilter, feListener);
            Thread feHandlerThread = new Thread(feHandler);
            feHandlerThread.setName("FrontEndThread");
            feHandlerThread.start();

            //start a back-end packet handler
            LOG.info("Starting back-end handler");
            String beFilter = new String("ip dst net "+beip);
            beHandler = new IfHandler(this, beip, beFilter, beListener);
            Thread beHandlerThread = new Thread(beHandler);
            beHandlerThread.setName("BackEndThread");
            beHandlerThread.start();


            //start load-balancer
            LOG.info("Starting core balancer");
            BalancerCore balancerCore = new BalancerCore(this, beip);
            Thread balancerCoreThread = new Thread(balancerCore);
            balancerCoreThread.setName("BalancerCoreThread");
            balancerCoreThread.start();

        } catch (PcapNativeException | NotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public String getServerPoolStr() {
        String pool = null;
        for (Server serv:serverPool){
            pool +=" "+serv.getAddress();
        }
        return pool;
    }

    @Override
    public boolean addServer(String iface) {
        if (iface != null){
            Server serv = new Server(iface);
            if (serverPool.contains(serv)){
                LOG.error("Server already in  the pool IP: {}", iface);
                return false;
            } else {
                //TODO check iface syntax
                serverPool.add(serv);
                LOG.info("Adding Server to the pool IP:{}", iface);
                serverPoolSize = serverPool.size();
                LOG.info("Server pool length: {}", serverPoolSize);
                //TODO rehash the balancer
                return true;
            }
        } else {
            LOG.error("Server IP address is Null", iface);
            return false;
        }
    }

    @Override
    public boolean removeServer(String iface) {
        Server serv = new Server(iface);
        if (serverPool.contains(serv)){
            LOG.info("Removing Server from the pool IP:{}", iface);
            serverPool.remove(serv);
            serverPoolSize = serverPool.size();
            LOG.info("Server pool size: {}", serverPoolSize);

        } else {
            LOG.info("No such server in the pool IP:{}", iface);
        };
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stopLoadBalancer() {
        LOG.info("Shutting down the load balancer..");
        //feHandler.stop();
        //beHandler.stop();
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public List<Server> getServerPool() {
        return serverPool;
    }

    @Override
    public int getServerPoolSize() {
        return serverPoolSize;
    }



}
