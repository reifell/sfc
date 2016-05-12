#! /usr/bin/env python
from scapy.all import *
import fcntl, socket, struct
from subprocess import call

# print 'starting new11 sf ...'
# tag = sys.argv[1]
# print 'tag = ' + tag
# logfile = open("/tmp/packets.log", "w")
# call('iptables -F', shell=True)
# #call('iptables -I INPUT -p udp -i sf1-eth0 -j DROP', shell=True)
#
#
# def get_mac(iface='sf1-eth0'):
#     s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#     info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
#     return ':'.join(['%02x' % ord(char) for char in info[18:24]])
#
# mac = get_mac()
# print 'mac = ' + mac
# def udp_incoming(pkt):
#     return pkt.dst == mac  and 'UDP' in pkt and 'Raw' in pkt
# j=0

p=Ether(src='00:00:00:00:00:12',dst='00:00:00:00:11:12')/Dot1Q(vlan=100)/IP(src='10.0.0.1',dst='10.0.0.2',tos=160)/UDP(sport=1000,dport=20000)/Raw() #/Dot1Q(vlan=1000)
print "Output:"
p.show()
sendp(p,iface="sf2-eth0")

