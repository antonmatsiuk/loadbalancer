/*
 * Copyright (c) 2017 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package eu.neclab.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Scanner;

/**
 * @author Anton Matsiuk (anton.matsiuk@neclab.eu)
 */
public class LoadBalancerApp {

    private static void  menu(){
        System.out.println("================================");
        System.out.println(" Choose a command: ");
        System.out.println(" 1 - add server to the pool");
        System.out.println(" 2 - remove server from the pool");
        System.out.println(" 3 - get server pool");
        System.out.println(" 0 - break and exit");
        System.out.println("================================");
    }

    public static void main(String[] args) {
        Logger LOG = LoggerFactory.getLogger(LoadBalancerApp.class);

        /**
         * Uncomment the block below to use interactive menu
         */
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter front-end Load Balancer's IP: ");
        String fiface = scanner.next();
        System.out.print("Enter Front-end Port: ");
        int feport = Integer.parseInt(scanner.next());
        System.out.print("Enter back-end Load-balancer's IP: ");
        String biface = scanner.next();
        System.out.print("Enter Front-end GW MAC: ");
        String gwmac  = scanner.next();
        System.out.print("Enter 1st Server IP to add: ");
        String servip1 = scanner.next();
        System.out.print("Enter 1st Server MAC to add: ");
        String servmac1 = scanner.next();
        //Hardcoding qnum;
        int qnum = 8;
        String proto = "tcp";
        LOG.info("Creating Load Balancer: front-end IP:{} back-end IP: {} proto: {} port:{} qnum:{}"
                ,fiface, biface, proto, feport, qnum);

        LoadBalancer loadBalancer = new LoadBalancerImpl(fiface, biface, gwmac, feport, proto, qnum);
        loadBalancer.addServer(servip1, servmac1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        menu();
        int command = Integer.parseInt(scanner.next());
        while (command !=  0){
            if (command == 1){
                System.out.print("Enter Server IP to add: ");
                String servip = scanner.next();
                System.out.print("Enter Server MAC to add: ");
                String servmac = scanner.next();
                loadBalancer.addServer(servip, servmac);
            } else if (command == 2){
                System.out.print("Enter Server IP to remove: ");
                String servip = scanner.next();
                loadBalancer.removeServer(servip);
            } else if (command == 3){
                System.out.print("Active servers in the pool: ");
                //loadBalancer.getServerPool();
            } else if (command == 0){
                System.out.print("Exiting LoadBalancer app...");
                break;
            }
            else {
                System.out.print("Unknown command, try again");
            }
            menu();
            command = Integer.parseInt(scanner.next());
        }
        loadBalancer.stopLoadBalancer();
    }
}
