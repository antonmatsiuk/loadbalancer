/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfHandler implements Runnable {

    /**
     * Logger instance.
     */
    static final Logger LOG = LoggerFactory.getLogger(IfHandler.class);

    private final String ipAddr;
    private final String filter;

    private final PcapHandle handler;

    private final PacketListener listener;

    private final LoadBalancer lbProvider;

    private boolean stop = false;

    private void stop(){
       stop = true;
    }

    public IfHandler(LoadBalancer lb, String ipadd ,String filt, PacketListener listnr)
            throws PcapNativeException, NotOpenException {
        lbProvider = lb;
        filter= filt;
        ipAddr = ipadd;
        listener= listnr;
        handler = getIPv4Handler(ipAddr);
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

    private PcapHandle getIPv4Handler(String ipaddr) {
        InetAddress addr = null;
        PcapNetworkInterface nif;
        PcapHandle handle =null;
        try {
            addr = InetAddress.getByName(ipaddr);
            nif = Pcaps.getDevByAddress(addr);
            int snapLen = 65536;
            PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
            int timeout = 10;
            handle = nif.openLive(snapLen, mode, timeout);
        } catch (UnknownHostException | PcapNativeException e) {
            LOG.error("Cannot bind to interface:{} "
                    ,ipaddr, e);
        }
        return handle;
    }

    @Override
    public void run() {
        LOG.info ("Starting IfHandler on iface:{} filter:{}",
                ipAddr,filter);
        loopReceiver();
        // TODO Auto-generated method stub

    }
}
