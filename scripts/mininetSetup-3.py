#!/usr/bin/python

from subprocess import call
from mininetBase import sfc

if __name__ == "__main__":

    sfc = sfc()

    sw1 = sfc.addSw()
    sw2 = sfc.addSw()
    sw3 = sfc.addSw()
    sw4 = sfc.addSw()

    h1 = sfc.addHost(sw1)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSf('1', sw2, 'test')
    sf2 = sfc.addSf('2', sw2, 'dpi')

    sf3 = sfc.addSf('3', sw3, 'ips')
    sf4 = sfc.addSf('4', sw4, 'ids')
    sf5 = sfc.addSf('5', sw4, 'fw')

    sfc.addLink(sw1, sw2)
    sfc.addLink(sw2, sw3)
    sfc.addLink(sw3, sw4)
    sfc.addLink(sw4, sw1)

    sfc.addGw(sw1)

    chain = ['test', 'dpi', 'ips', 'ids', 'fw']

    sfc.addChain('c1', sw1, chain)

    print "start"

    sfc.deployTopo()


    sfc.net.stop()