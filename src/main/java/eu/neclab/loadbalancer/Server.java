/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neclab.loadbalancer;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public class Server  {

  public void processInPacket() {};

  public void processOutPacket() {};

  private final String address;

  /**
   * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
   */
  public Server (String ipaddr) {
      address = ipaddr;
  }

  public String getAddress (){
      return address;
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
      if (!this.address.equals(other.getAddress())){
          return false;
      }
    return true;
  }

}
