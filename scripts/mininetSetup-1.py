#!/usr/bin/python

from subprocess import call
from mininetBase import sfc

if __name__ == "__main__":

    sfc = sfc()

    sw1 = sfc.addSw('1')
    sw2 = sfc.addSw('2')
    h1 = sfc.addHost('1', sw1)
    h2 = sfc.addHost('2', sw1)

    sf1 = sfc.addSf('1', sw2)
    sf2 = sfc.addSf('2', sw2)

    sfc.net.addLink(sw1, sw2)

    sfc.addGw(sw1)
    print "start"

    sfc.deployTopo()

    h1.cmd('ping 10.0.0.2 -n 3 &')
    h2.cmd('ping 10.0.0.30 -n 3 &')


    sfc.net.stop()