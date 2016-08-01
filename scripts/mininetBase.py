#!/usr/bin/python
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.link import Intf
from mininet.log import setLogLevel, info
from subprocess import call
from collections import defaultdict
from odlConfGeneration import odlConf
import json
import time
class sfc():
    YOUR_CONTROLLER_IP = '192.168.100.103'
    net = None
    callBackConfs = {'host': [], 'sf': [], 'sw': [], 'chain':[] }
    odl = odlConf()
    topo = None
    sffs = []
    hosts = []
    sfs = []

    popens = {}

    def deployTopo(self):


        self.topo = self.net.start()
        time.sleep(1)
        # clean previous rules
        self.odl.clean(self.callBackConfs['chain'], self.callBackConfs['sw'])
        time.sleep(1)

        self.deploySwConf()
        self.deployHostConf()
        self.deploySfConf()

        self.deployODLConf()

        #deploy chain
        self.deploySfc()

        CLI(self.net)

        self.cleanProcess()

    def __init__(self):
        self.odl.readParameters()
        self.net = Mininet(controller=None)
        self.net.addController('c0', controller=RemoteController, ip=self.odl.controller, port=6633)

    def addHost(self, sw):

        num = str(len(self.sfs) + 1)
        host = self.net.addHost("h%s" % (num))

        self.sfs.append(host)
        self.net.addLink(host, sw)
        hostConf = {}
        hostConf[host] = {}
        hostConf[host]['IP'] = '10.0.0.'+(num)
        hostConf[host]['MAC'] = '00:00:00:00:00:0%s' %(num)
        self.callBackConfs['host'].append(hostConf)
        return host


    def addSf(self, num, sw, type):
        sf = self.net.addHost("sf%s" % (num))
        link = self.net.addLink(sf, sw)
        port =  sw.ports[link.intf2]
        sfConf = {}
        sfConf[sf] = {}
        sfConf[sf]['IP'] = '10.0.0.1%s' % (num)
        sfConf[sf]['MAC'] = '00:00:00:00:00:1%s' % (num)
        tag = 300 + int(num)
        sfConf[sf]['CMD'] = []
        sfConf[sf]['CMD'].append("vconfig add sf%s-eth0 %s" % (num, str(tag)))
        sfConf[sf]['CMD'].append("ip link set up sf%s-eth0.%s" % (num, str(tag)))
        sfConf[sf]['CMD'].append("./functions/sf_dummy sf%s-eth0.%s > ./sf%s.out 2>&1 &" % (num, str(tag), num))

        sfConf[sf]['CONF'] = self.odl.sfConf(sf.name, num, type, sfConf[sf]['IP'], sw.name, tag, sfConf[sf]['MAC'], port, self.getODLSwConf(sw))
        self.callBackConfs['sf'].append(sfConf)

        return sf


    def addSw(self):
        num = str(len(self.sffs) + 1)
        sw = self.net.addSwitch('SFF%s' %num)

        self.sffs.append(sw)

        swConf = {}
        swConf[sw] = {}
        swConf[sw]['CMD'] = []
        swConf[sw]['CMD'].append('ovs-vsctl set bridge %s protocols=OpenFlow13'%(sw.name))
        swConf[sw]['CMD'].append('ovs-ofctl -OOpenFlow13 add-flow %s cookie=0xFF22FF,table=11,priority=760,ip,ip_ecn=3,actions=CONTROLLER'%(sw.name))
        swConf[sw]['CMD'].append('ovs-ofctl -OOpenFlow13 add-flow %s cookie=0xFF44FF,table=11,priority=760,ip,ip_ecn=2,actions=CONTROLLER'%(sw.name))
        swConf[sw]['CONF'] = self.odl.sffConfBase(sw.name, num)
        self.callBackConfs['sw'].append(swConf)
        return sw

    def addGw(self, sw):
        gw = self.net.addHost('gw')
        self.net.addLink(gw, sw)
        gwConf = {}
        gwConf[gw] = {}
        gwConf[gw]['IP'] = '10.0.0.30'
        gwConf[gw]['MAC'] = '00:00:00:00:00:FE'
        gwConf[gw]['CMD'] = []
        gwConf[gw]['CMD'].append("./functions/gw gw-eth0 > ./gw.out 2>&1 &")
        self.callBackConfs['sf'].append(gwConf)

    def addLink(self, sw1, sw2):
        link = self.net.addLink(sw1, sw2)
        portSw2 = sw2.ports[link.intf2]
        portSw1 = sw1.ports[link.intf1]
        self.odl.appendSffConf(self.getODLSwConf(sw1), portSw1, str(self.sffs.index(sw1) +1), self.getODLSwConf(sw2), portSw2, str(self.sffs.index(sw2) +1))

    def addChain(self, name, sw1, chain):
        chainConf = {}
        chainConf[name] = {}
        chainConf[name]['sfc'] = self.odl.setChain(name, chain)
        chainConf[name]['sfp'] = self.odl.setChainPath(name, sw1.name)
        chainConf[name]['scf'] = self.odl.setClassifier(sw1.name)
        chainConf[name]['rsp'] = self.odl.rederedRPC(chainConf[name]['sfp']['service-function-path']['name'])

        self.callBackConfs['chain'].append(chainConf)

    def getODLSwConf(self, sff):
        for sw in self.callBackConfs['sw']:
            for swTopo, conf in sw.iteritems():
                if swTopo == sff:
                    return conf['CONF']
        return None

    def deployHostConf(self):
        for host in self.callBackConfs['host']:
            for hostTopo, conf in host.iteritems():
                hostTopo.setIP(conf['IP'])
                hostTopo.setMAC(conf['MAC'])
                if hostTopo.name == 'h2':
                    hostTopo.cmd("python ./functions/server.py 10.0.0.1 h2-eth0 > ./server.out 2>&1 &")
                    self.popens[hostTopo] = int(hostTopo.cmd('echo $!'))

    def deploySfConf(self):
        for sf in self.callBackConfs['sf']:
            for sfTopo, conf in sf.iteritems():
                sfTopo.setIP(conf['IP'])
                sfTopo.setMAC(conf['MAC'])
                for cmd in conf['CMD']:
                    print cmd
                    sfTopo.cmd(cmd)
                    pid = sfTopo.cmd('echo $!')
                    if  bool(pid.strip()):
                        self.popens[sfTopo] = int(pid)
                if sfTopo.name is not 'gw':
                    print "post odl conf:"
                    self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_FUNCTION, conf['CONF'], True)

    def deploySwConf(self):
        for sw in self.callBackConfs['sw']:
            for swTopo, conf in sw.iteritems():
                print swTopo.name
                for cmd in conf['CMD']:
                    print cmd
                    call(cmd, shell=True)

    def deployODLConf(self):
        for sw in self.callBackConfs['sw']:
            for swTopo, conf in sw.iteritems():
                self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_FUNCTION_FORWARDER, conf['CONF'], True)

    def deploySfc(self):
        for chain in self.callBackConfs['chain']:
            for chainName, conf in chain.iteritems():
                self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_FUNCTION_CHAIN, conf['sfc'], True)
                self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_FUNCTION_PATH, conf['sfp'], True)
                self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_CLASSIFICATION_FUNTION, conf['scf'], True)
                self.odl.post(self.odl.controller, self.odl.DEFAULT_PORT, self.odl.SERVICE_RENDERED_PATH, conf['rsp'], True)

        scf = conf['scf']['service-function-classifier']['scl-service-function-forwarder'][0]['name']
        print scf
        numberOfSFFs = (len(self.sffs) -1) #considering one classifier
        ############# classifier rules ################################
        for sw in self.callBackConfs['sw']:
            for swTopo, conf in sw.iteritems():
                if swTopo.name == scf:
                    vlanId = self.odl.getVlanId(self.odl.controller, self.odl.DEFAULT_PORT, "openflow:2", 2)
                    call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1000,ip,nw_dst=10.0.0.2,actions=mod_vlan_vid:%s,output:3'%(scf, str(vlanId)), shell=True) # enter chain upstream
                    call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1001,ip,dl_vlan=%s,actions=pop_vlan,mod_dl_dst=00:00:00:00:00:FE,output:5' %(scf, str(vlanId+numberOfSFFs)), shell=True) # forward to gateway
                    call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1002,dl_src=00:00:00:00:00:fe,actions=mod_dl_src=00:00:00:00:00:01,normal'%(scf), shell=True) # forwarding packet from gateway
                    call('ovs-ofctl -OOpenFlow13 add-flow %s priority=99,actions=normal'%(scf), shell=True) #normal traffic from no chain
                    #bidirectional rules
                    #call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1000,udp,nw_dst=10.0.0.1,actions=mod_vlan_vid:%s,output:4' % (scf, str(vlanId+100)), shell=True)  # enter chain downstream
                    #call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1001,ip,dl_vlan=%s,actions=pop_vlan,mod_dl_dst=00:00:00:00:00:FE,output:5' % (scf, str(vlanId+100+numberOfSFFs)), shell=True)  # forward to gateway
       #             call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1001,udp,nw_dst=10.0.0.2,udp_dst=5522,actions=mod_nw_ecn=2,mod_vlan_vid:%s,output:3'%(scf, str(vlanId)), shell=True) # enter chain upstream
                    call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1001,icmp,nw_src=10.0.0.1,nw_dst=10.0.0.2,actions=mod_nw_ecn=2,mod_vlan_vid:%s,output:3'%(scf, str(vlanId)), shell=True) # enter chain upstream
       #             call('ovs-ofctl -OOpenFlow13 add-flow %s priority=1001,udp,nw_dst=10.0.0.2,udp_dst=5533,actions=mod_nw_ecn=3,mod_vlan_vid:%s,output:3'%(scf, str(vlanId)), shell=True) # enter chain upstream
                    call('ovs-ofctl -OOpenFlow13 add-flow %s cookie=0xFF22FF,table=0,priority=1004,in_port=2,dl_dst=00:00:00:00:00:01,actions=output:1' %(scf), shell=True)
    def cleanProcess(self):
        for p in self.popens.values():
            call('kill %d' %(p), shell=True) #SIGINT

