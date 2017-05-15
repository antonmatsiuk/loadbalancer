# Load Balancer
This is an implementation of a simple Load balancer performing ip.src hashing algorithm and strict priority ToS queueing
Simple modifications to the core load balancer algorithm will enable WFQ and other hashing strategies.

## Prerequisites
1. JVM 1.7+ 
2. Maven 3.1.1+: `sudo apt-get install maven`
3. Libpcap: `sudo apt-get install libpcap-dev`
 
# Building 
1. For hardcoded interface values skip the step 2
2. If you want to use an interactive menu, uncomment the upper block in: `src/main/java/eu/neclab/loadbalancer/LoadBalancerApp.java`
3. build the project: `mvn clean install`

## Running
1. Create a back-end virtual interface by running `create_interface.sh`.
2. Start the load balancer: `sudo mvn exec:java`
3. For debug mode: `sudo mvn exec:java -X`
4. Enter a front-end IP (e.g. the eth0 IP address with any active tcp connections)
5. Dump the back-end interface to observe the load-balanced traffic: `sudo tcpdump -i s1-veth1`

# License
This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html.

# Contact
E-mail: Anton Matsiuk (anton.matsiuk at gmail.com)

 

