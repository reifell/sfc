#!/usr/bin/python

from subprocess import call
from mininetBase import SFC
from odlConfGeneration import sfcEncap
import sys


if __name__ == "__main__":

    sfc = SFC(sfcEncap.VLAN, sys.argv[1])     #(1)

    sw1 = sfc.addSw()                         #(2)
    sw2 = sfc.addSw()
    sw3 = sfc.addSw()

    h1 = sfc.addHost(sw1)                     #(3)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSf('1', sw2, 'fw')                #(4)
    sf2 = sfc.addSf('2', sw2, 'dpi')
    sf3 = sfc.addSf('3', sw3, 'fw1')
    sf4 = sfc.addSf('4', sw3, 'nat')

    sfc.addLink(sw1, sw2)                     #(5)
    sfc.addLink(sw2, sw3)
    sfc.addLink(sw3, sw1)

    sfc.addGw(sw1)                            #(6)

    chain = ['fw', 'dpi', 'fw1', 'nat']       #(7)

    sfc.addChain('c1', sw1, chain)            #(8)

    sfc.deployTopo()                          #(9)

