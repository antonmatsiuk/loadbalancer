/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.pcap4j.util.MacAddress;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */

public class Server  {

  public void processInPacket() {};

  public void processOutPacket() {};

  private Inet4Address ipaddr;

  private MacAddress macaddr;

  /**
   * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
   */
  public Server (String ip, String mac) {
      ipaddr = null;
      macaddr = null;
      //TODO check ip, mac syntax
      try {
          ipaddr = (Inet4Address) Inet4Address.getByName(ip);
          setMacAddress(MacAddress.getByName(mac));
      } catch (UnknownHostException e) {
          e.printStackTrace();
      }

  }

  public Inet4Address getAddress (){
      return ipaddr;
  }

  public MacAddress getMacAddres(){
      return macaddr;
  }

  //TODO ARP expiration and server's mobility
  protected void setMacAddress(MacAddress mac){
          macaddr = mac;
  }

  @Override
  public boolean equals(Object obj){
      if (obj == null) {
          return false;
      }
      if (!Server.class.isAssignableFrom(obj.getClass())) {
          return false;
      }
      final Server other = (Server)obj;
      if (!this.ipaddr.equals(other.getAddress())){
          return false;
      }
    return true;
  }

}
