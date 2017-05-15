#!/bin/bash
#a test script for creation of back-end interfaces

echo '========== Create a back-end interface =========='

sudo ip link add veth1 type veth peer name s1-veth1
sudo ifconfig veth1 up
sudo ifconfig s1-veth1 up
sudo ifconfig veth1 10.10.10.1

#create static arp for backend servers
sudo arp  -i veth1 -s 10.0.10.2 bb:bb:bb:bb:bb:bb
sudo arp  -i veth1 -s 10.0.10.3 cc:cc:cc:cc:cc:cc


