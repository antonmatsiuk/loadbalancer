/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfHandler implements Runnable {

    static final Logger LOG = LoggerFactory.getLogger(IfHandler.class);

    /**
     * IP interface to apply the pcap handler.
     */
    private final InetAddress ipv4addr;

    /**
     * Pcap interface filter.
     */
    private final String filter;

    /**
     * Pcap interface handler.
     */
    private final PcapHandle handler;

    /**
     * PacketListener implements interface-specific
     * packet parsing and processing
     */
    private final PacketListener listener;

    private final boolean stop = false;

    public void stop (){
        try {
            handler.breakLoop();
        } catch (NotOpenException e) {
            e.printStackTrace();
        }
    }

    public IfHandler(InetAddress ip ,String filt, PacketListener listnr)
            throws PcapNativeException, NotOpenException {
        filter= filt;
        ipv4addr = ip;
        listener= listnr;
        handler = getIPv4Handler();
        if (filter.length() != 0) {
            handler.setFilter(
              filter,
              BpfCompileMode.OPTIMIZE);
          }
    }

    private void loopReceiver(){
        try {
            //starting infinite packet capturing loop
            handler.loop(-1, listener);
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (PcapNativeException | NotOpenException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }
    }

    public void sendPacket(Packet pack){
        try {
            handler.sendPacket(pack);
        } catch (PcapNativeException | NotOpenException e) {
            LOG.error("Error sending the packet {},", pack.toString());
            e.printStackTrace();
        }
    }

    public MacAddress getIfHandlerMac() {
        MacAddress macaddr = null;
        NetworkInterface network;
        try {
            network = NetworkInterface.getByInetAddress(ipv4addr);
            macaddr = MacAddress.getByAddress(network.getHardwareAddress());
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return macaddr;
    }

    private PcapHandle getIPv4Handler() {
        PcapNetworkInterface nif;
        PcapHandle handle = null;
        try {
            nif = Pcaps.getDevByAddress(ipv4addr);
            int snapLen = 1500;
            PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
            int timeout = 10;
            handle = nif.openLive(snapLen, mode, timeout);
        } catch (PcapNativeException e) {
            LOG.error("Cannot bind to interface:{} "
                    ,ipv4addr.toString(), e);
        }
        return handle;
    }

    @Override
    public void run() {
        LOG.info ("Starting IfHandler on iface:{} filter:{}",
                ipv4addr.toString(),filter);
        loopReceiver();
        LOG.info("Terminating IF handler...");
    }
}
