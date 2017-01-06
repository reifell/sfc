#!/usr/bin/python

from subprocess import call
from mininetBase import SFC
from odlConfGeneration import sfcEncap

if __name__ == "__main__":

    sfc = SFC(sfcEncap.VLAN)

    sw1 = sfc.addSw()
    sw2 = sfc.addSw()

    h1 = sfc.addHost(sw1)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSnort('1', sw2, 'ips1')
    sf2 = sfc.addSnort('2', sw2, 'ips2')

    sfc.addLink(sw1, sw2)
    sfc.addLink(sw2, sw1)

    sfc.addGw(sw1)
    # 'ips',
    chain = ['ips1', 'ips2']#, 'fw']#, 'fw1', 'fw2', 'fw3']

    # chain2 = ['test', 'dpi', 'dpi2', 'ips', 'ids', 'fw', ]#'fw1', 'fw2', 'fw3']

    sfc.addChain('c1', sw1, chain)

    # sfc.addChain('c2', sw1, chain2)

    print
    "start"

    sfc.deployTopo()

    sfc.net.stop()