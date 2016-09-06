#!/usr/bin/python

from subprocess import call
from mininetBase import sfc

if __name__ == "__main__":

    sfc = sfc()

    sw1 = sfc.addSw()
    sw2 = sfc.addSw()
    sw3 = sfc.addSw()
    #sw4 = sfc.addSw()

    h1 = sfc.addHost(sw1)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSf('1', sw2, 'test')
    sf2 = sfc.addSf('2', sw2, 'dpi')
    sf3 = sfc.addSf('3', sw2, 'dpi2')

    sf4 = sfc.addSf('4', sw3, 'ips')
    #sf5 = sfc.addSf('5', sw3, 'ids')
    #sf6 = sfc.addSf('6', sw3, 'fw')

    #sf7 = sfc.addSf('7', sw4, 'fw1')
    #sf8 = sfc.addSf('8', sw4, 'fw2')
    #sf9 = sfc.addSf('9', sw4, 'fw3')

    sfc.addLink(sw1, sw2)
    sfc.addLink(sw2, sw3)
    sfc.addLink(sw3, sw1)
    #sfc.addLink(sw4, sw1)

    sfc.addGw(sw1)

    chain = ['test', 'dpi', 'dpi2', 'ips']#, 'ids', 'fw', 'fw1', 'fw2', 'fw3']

    sfc.addChain('c1', sw1, chain)

    print "start"

    sfc.deployTopo()


    sfc.net.stop()