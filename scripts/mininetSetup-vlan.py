#!/usr/bin/python

from subprocess import call
from mininetBase import SFC
from odlConfGeneration import sfcEncap
import sys


if __name__ == "__main__":

    sfc = SFC(sfcEncap.MAC_CHAIN, sys.argv[1])

    sw1 = sfc.addSw()
    sw2 = sfc.addSw()
    sw3 = sfc.addSw()
    #sw4 = sfc.addSw()

    h1 = sfc.addHost(sw1)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSf('1', sw2, 'test')
    sf2 = sfc.addSf('2', sw2, 'dpi')
    #sf3 = sfc.addSnort('3', sw2, 'ips')

    sf4 = sfc.addSf('4', sw3, 'ips2')
    sf5 = sfc.addSf('5', sw3, 'fw')
    #sf6 = sfc.addSf('6', sw3, 'fw1')

    #sf7 = sfc.addSf('7', sw4, 'fw1')
    #sf8 = sfc.addSf('8', sw4, 'fw2')
    #sf9 = sfc.addSf('9', sw4, 'fw3')

    sfc.addLink(sw1, sw2)
    sfc.addLink(sw2, sw3)
    sfc.addLink(sw3, sw1)
    #sfc.addLink(sw4, sw1)

    sfc.addGw(sw1)
    #'ips',
    chain = ['test', 'dpi', 'ips2', 'fw']#, 'fw1', 'fw2']#, 'fw']#, 'fw1', 'fw2', 'fw3']

    #chain2 = ['test', 'dpi', 'dpi2', 'ips', 'ids', 'fw', ]#'fw1', 'fw2', 'fw3']

    sfc.addChain('c1', sw1, chain)

    #sfc.addChain('c2', sw1, chain2)

    print "start"

    sfc.deployTopo()


    sfc.net.stop()
