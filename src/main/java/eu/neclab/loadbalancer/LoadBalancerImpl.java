/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc1349Tos;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.IpV4Packet.Builder;
import org.pcap4j.packet.namednumber.IpV4TosPrecedence;
import org.pcap4j.util.MacAddress;
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

    private InetAddress feipaddr;


    private InetAddress beipaddr;

    private final MacAddress gwmac;


    private final Hashtable<InetAddress , MacAddress>  feMacCache =
            new Hashtable<>();

    /**
     * Front-end IF handler.
     */
    private IfHandler feHandler;
    /**
     * Back-end IF handler.
     */
    private IfHandler beHandler;


    @Override
    public IfHandler getFeHandler() {
        if (feHandler != null){
            return feHandler;
        } else {
            throw new NullPointerException("Front-end IF handler is null");
        }
    }

    @Override
    public IfHandler getBeHandler() {
        if (beHandler != null){
            return beHandler;
        } else {
            throw new NullPointerException("Back-end IF handler is null");
        }
    }

    @Override
    public ConcurrentLinkedQueue<Packet> getPacketQueue(int index){
        return packetQueues.get(index);

    }

    @Override
    public int getQueueNum(){
        return  packetQueues.size();
    }

    @Override
    public boolean removeServer(String ip) {
        Server serv = new Server(ip, null);
        if (serverPool.contains(serv)){
            LOG.info("Removing Server from the pool IP:{}", ip);
            serverPool.remove(serv);
            serverPoolSize = serverPool.size();
            LOG.info("Server pool size: {}", serverPoolSize);
        } else {
            LOG.error("No such server in the pool IP:{}", ip);
        };
        return false;
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
    public boolean addServer(String ip, String mac) {
        Server serv = new Server(ip, mac);
        if (serverPool.contains(serv)){
            LOG.error("Server already in  the pool IP: {}", ip);
            return false;
        } else {
            serverPool.add(serv);
            LOG.info("Adding Server to the pool IP:{}", ip);
            serverPoolSize = serverPool.size();
            LOG.info("Server pool length: {}", serverPoolSize);
            return true;
        }
    }

    @Override
    public List<Server> getServerPool() {
        return serverPool;
    }

    @Override
    public int getServerPoolSize() {
        return serverPool.size();
    }

    @Override
    public boolean stopLoadBalancer() {
        LOG.info("Shutting down the load balancer..");
        //feHandler.stop();
        //beHandler.stop();
        // TODO Auto-generated method stub
        return true;
    }
    //packet handler for the back-end interface
    PacketListener beListener = new PacketListener() {
        @Override
        public void gotPacket(Packet packet) {
            IpV4Packet ipv4Packet = packet.get(IpV4Packet.class);
            IpV4Rfc1349Tos tosBits = (IpV4Rfc1349Tos) ipv4Packet.getHeader().getTos();
            IpV4TosPrecedence precBits = tosBits.getPrecedence();
            int tosClass = precBits.value().intValue();
            Inet4Address srcAddr = ipv4Packet.getHeader().getSrcAddr();
            Inet4Address dstAddr = ipv4Packet.getHeader().getDstAddr();
            LOG.info("BE: src.ip={} dst.ip= {} prec ={}",
                    srcAddr.toString(), dstAddr.toString(), tosClass);
            //rewrite src.ip and src/dst.mac
            Builder outIpv4PackBuilder = ipv4Packet.getBuilder();
            outIpv4PackBuilder.srcAddr((Inet4Address) feipaddr);
            org.pcap4j.packet.EthernetPacket.Builder outEtherBuilder =
                    (org.pcap4j.packet.EthernetPacket.Builder) packet.getBuilder();
            outEtherBuilder.srcAddr(feHandler.getIfHandlerMac());
            outEtherBuilder.dstAddr(gwmac);
            outEtherBuilder.payloadBuilder(outIpv4PackBuilder);
            //send out of the front-end interface
            feHandler.sendPacket(outEtherBuilder.build());
        }
    };

    //packet handler for the  front-end interface
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
            //put a packet to a particular CoS queue ->  process in BalancerCore
            ConcurrentLinkedQueue<Packet> queue = getPacketQueue(tosClass);
            queue.add(packet);
        }
      };

    /**
     * Public constructor.
     */
    public LoadBalancerImpl(String feip, String beip, String gwmc, int port, String proto, int qnum){
        serverPool = new ArrayList<>();
        packetQueues = new ArrayList<>();
        serverPoolSize = 0;
        gwmac = MacAddress.getByName(gwmc);
        try {
            feipaddr = InetAddress.getByName(feip);
            beipaddr = InetAddress.getByName(feip);
        } catch (UnknownHostException e) {
            LOG.error("Wrong IP address provided", e);
            e.printStackTrace();
        }
        for (int i=0; i<qnum; i++){
            ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();
            packetQueues.add(queue);
        }

        try {
            //start a front-end If packet handler
            //String filter = new String("ip src net "+feip+ " and tcp dst port "+port);
            LOG.info("Starting front-end handler");
            String feFilter = new String("ip dst net "+feip+ " and "+proto);
            feHandler = new IfHandler(feipaddr, feFilter, feListener);
            Thread feHandlerThread = new Thread(feHandler);
            feHandlerThread.setName("FrontEndThread");
            feHandlerThread.start();

            //start a back-end If packet handler
            LOG.info("Starting back-end handler");
            String beFilter = new String("ip dst net "+beip);
            beHandler = new IfHandler(beipaddr, beFilter, beListener);
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

}
