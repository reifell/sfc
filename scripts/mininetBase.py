#!/usr/bin/python
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from subprocess import call
from collections import defaultdict

class sfc():
    YOUR_CONTROLLER_IP = '192.168.100.103'
    net = None
    callBackConfs = {'host': [], 'sf': [], 'sw': [] }
    def deployTopo(self):
        self.net.start()
        self.deploySwConf()
        self.deployHostConf()
        self.deploySfConf()
        CLI(self.net)

    def __init__(self):
        self.net = Mininet(controller=None)
        self.net.addController('c0', controller=RemoteController, ip=self.YOUR_CONTROLLER_IP, port=6633)

    def addHost(self, num, sw):
        host = self.net.addHost("h%s" %(num))
        self.net.addLink(host, sw)
        hostConf = {}
        hostConf[host] = {}
        hostConf[host]['IP'] = '10.0.0.'+(num)
        hostConf[host]['MAC'] = '00:00:00:00:00:0%s' %(num)
        self.callBackConfs['host'].append(hostConf)
        return host


    def addSf(self, num, sw):
        sf = self.net.addHost("sf%s" % (num))
        link = self.net.addLink(sf, sw)
        sfConf = {}
        sfConf[sf] = {}
        sfConf[sf]['IP'] = '10.0.0.1%s' % (num)
        sfConf[sf]['MAC'] = '00:00:00:00:00:1%s' % (num)
        tag = 300 + int(num)
        sfConf[sf]['CMD'] = 'python ./functions/sf_dummy.py sf%s sf%s-eth0 %s &'% (num, num, str(tag))
        self.callBackConfs['sf'].append(sfConf)
        print sf.name
        print link.intf2
        return sf


    def addSw(self, num):
        sw = self.net.addSwitch('sw%s' %(num))
        swConf = {}
        swConf[sw] = {}
        swConf[sw]['CMD'] = 'ovs-vsctl set bridge sw%s protocols=OpenFlow13'%(num)
        self.callBackConfs['sw'].append(swConf)
        return sw

    def addGw(self, sw):
        gw = self.net.addHost('gw')
        self.net.addLink(gw, sw)
        gwConf = {}
        gwConf[gw] = {}
        gwConf[gw]['IP'] = '10.0.0.30'
        gwConf[gw]['MAC'] = '00:00:00:00:00:FE'
        gwConf[gw]['CMD'] = 'python ./functions/gw.py gw &'
        self.callBackConfs['sf'].append(gwConf)

    def deployHostConf(self):
        for host in self.callBackConfs['host']:
            for hostTopo, conf in host.iteritems():
                hostTopo.setIP(conf['IP'])
                hostTopo.setMAC(conf['MAC'])
                if hostTopo.name == 'h2':
                    hostTopo.cmd('python ./functions/server.py 10.0.0.1 h2-eth0 &')

    def deploySfConf(self):
        for sf in self.callBackConfs['sf']:
            for sfTopo, conf in sf.iteritems():
                sfTopo.setIP(conf['IP'])
                sfTopo.setMAC(conf['MAC'])
                sfTopo.cmd(conf['CMD'])

    def deploySwConf(self):
        for sw in self.callBackConfs['sw']:
            for swTopo, conf in sw.iteritems():
                call(conf['CMD'], shell=True)




