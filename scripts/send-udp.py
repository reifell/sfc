#! /usr/bin/env python
from scapy.all import *
import fcntl, socket, struct
from subprocess import call

print 'send udp packet ...'
pkt = sys.argv[1]
sf_tag = sys.argv[2]



pkt.show()
e, i, u, d = pkt, pkt['IP'], pkt['UDP'], pkt['Raw']
pkt['Raw'].load = str(d.load).strip()+','+sf_tag

p=Ether(src=e.dst,dst=e.src)/i #IP(src=i.src,dst=i.dst,tos=68)/UDP(sport=u.sport,dport=u.dport)/Raw(load=str(d)+','+tag) #/Dot1Q(vlan=1000)
print "Output:"
p.show()
sendp(p,iface="sf1-eth0")


