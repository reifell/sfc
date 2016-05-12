#!/usr/bin/python
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from subprocess import call

def emptyNet():
    YOUR_CONTROLLER_IP = '192.168.100.103'
    net = Mininet(controller=None)
    net.addController( 'c0', controller=RemoteController, ip=YOUR_CONTROLLER_IP, port=6633)
    h1 = net.addHost( 'h1' )
    h2 = net.addHost( 'h2' )
    sf1 = net.addHost( 'sf1')
    sf2 = net.addHost( 'sf2')
    sw1 = net.addSwitch( 'sw1' )
    sw2 = net.addSwitch( 'sw2' )
    net.addLink( h1, sw1 )
    net.addLink( h2, sw1 )

    net.addLink( sf1, sw2 )
    net.addLink( sf2, sw2 )
    net.addLink( sw1, sw2 )


    gw = net.addHost('gw')
    net.addLink(gw,sw1)

    net.start()

    h1.setMAC('00:00:00:00:00:01')
    h2.setMAC('00:00:00:00:00:02')
    h1.setIP('10.0.0.1')
    h2.setIP('10.0.0.2')

    gw.setMAC('00:00:00:00:00:FE')
    gw.setIP('10.0.0.30')
    #call('ovs-vsctl set port sw2-eth1 tag=111', shell=True)
    #call('ovs-vsctl set port sw2-eth2 tag=112', shell=True)

    call('ovs-vsctl set bridge sw1 protocols=OpenFlow13', shell=True)
    call('ovs-vsctl set bridge sw2 protocols=OpenFlow13', shell=True)

    # vlanId = 300
    # call("ovs-ofctl add-flow sw1 priority=1000,udp,nw_dst=10.0.0.2,actions=mod_vlan_vid:" + str(vlanId) + ",output:3", shell=True)
    # call("ovs-ofctl add-flow sw1 priority=1001,ip,dl_vlan=" + str(vlanId +1)+ ",actions=output:4", shell=True)
    # call("ovs-ofctl add-flow sw1 priority=1002,dl_src=00:00:00:00:00:fe,actions=normal", shell=True)
    # call("ovs-ofctl add-flow sw1 priority=99,actions=normal", shell=True)

    sf1.setMAC('00:00:00:00:00:11')
    sf2.setMAC('00:00:00:00:00:12')
    sf1.setIP('10.0.0.11')
    sf2.setIP('10.0.0.12')
    #res = net.pingAll()
    #print("ping result:  "+ res)

    #cmd1 = "ovs-vsctl set-controller s1 ssl:%s:6633" %(YOUR_CONTROLLER_IP)
    h1.cmd('ping 10.0.0.2 -n 3 &')
    h2.cmd('ping 10.0.0.30 -n 3 &')

    sf1.cmd('python sf_dummy.py sf1 sf1-eth0 &')
    sf2.cmd('python sf_dummy.py sf2 sf2-eth0 &')

    gw.cmd('python gw.py gw &')



    #docker config
    # call('docker rm -f sf1', shell=True)
    # call('docker run --name=sf1 -h=sf1 --privileged=true --net=none -v /home/rafael/code/:/nfv -dit 227085dee81b /bin/bash', shell=True)
    # call('ovs-docker add-port sw2 sf1-eth0 sf1 10.0.0.11/16', shell=True)
    # call('docker exec sf1 ip link set dev sf1-eth0 down', shell=True)
    # call('docker exec sf1 ip link set dev sf1-eth0 address 00:00:00:00:00:11', shell=True)
    # call('docker exec sf1 ip link set dev sf1-eth0 up', shell=True)

    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    emptyNet()
