#!/usr/bin/python
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from subprocess import call

def emptyNet():
    YOUR_CONTROLLER_IP = '192.168.100.105'
    net = Mininet(controller=None)
    net.addController( 'c0', controller=RemoteController, ip=YOUR_CONTROLLER_IP, port=6633)
    #h1 = net.addHost( 'h1' )
    #h2 = net.addHost( 'h2' )
    sf1 = net.addHost( 'sf1' )
    sf2 = net.addHost( 'sf2' )
    #sw1 = net.addSwitch( 'sw1' )
    sw2 = net.addSwitch( 'sw2' )
    #net.addLink( h1, sw1 )
    #net.addLink( h2, sw1 )

    #net.addLink( sf1, sw2 )
    #net.addLink( sf2, sw2 )
    #net.addLink( sw1, sw2 )

    net.start()
    #call('ip link add name sff1-dpl type veth peer name eth-test', shell=True)
    #call('ovs-vsctl add-port sw1 sff1-dpl -- set interface sff1-dpl type=vxlan options:remote_ip=flow options:dst_port=6500 options:nshc1=flow options:nshc2=flow options:nshc3=flow options:nshc4=flow options:nsp=flow options:nsi=flow options:key=flow', shell=True)
    call('ovs-vsctl add-port sw2 sff0-dpl -- set interface sff0-dpl type=vxlan options:remote_ip=flow options:dst_port=6633 options:nshc1=flow options:nshc2=flow options:nshc3=flow options:nshc4=flow options:nsp=flow options:nsi=flow options:key=flow', shell=True)
#    cmd1 = "ovs-vsctl set-controller s1 ssl:%s:6633" %(YOUR_CONTROLLER_IP)
#    cmd2 = "ovs-vsctl set-controller s2 ss2:%s:6633" %(YOUR_CONTROLLER_IP)
    #s1.cmd(cmd1)
    #s2.cmd(cmd2)
    #h1.setMAC('00:00:00:00:00:01')
    #h2.setMAC('00:00:00:00:00:02')
    #h1.setIP('10.0.0.1')
    #h2.setIP('10.0.0.2')


    #sf1.setMAC('00:00:00:00:00:11')
    #sf2.setMAC('00:00:00:00:00:12')
    #sf1.setIP('10.0.0.11')
    #sf2.setIP('10.0.0.12')
    #res = net.pingAll()
    #print("ping result:  "+ res)
    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    emptyNet()
