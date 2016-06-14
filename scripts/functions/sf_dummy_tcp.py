#! /usr/bin/env python
from scapy.all import *
import time
import fcntl, socket, struct
from subprocess import call

print 'starting service function ...'
tag = sys.argv[1]
interface = sys.argv[2]
print 'tag = ' + tag
# call('iptables -F', shell=True)
# call('iptables -I INPUT -p udp -i sf1-eth0 -j DROP', shell=True)


def get_mac(iface=interface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac()
print 'mac = ' + mac


def send_pkt(pkt, sf_tag):
    e, i, u, d = pkt, pkt['IP'], pkt['TCP'], pkt['Raw']
    pkt['Raw'] = Raw(load=str(d).strip()+','+sf_tag)
    p = Ether(src=e.dst, dst=e.src)/Dot1Q(vlan=100)/i  # IP(src=i.src,dst=i.dst,tos=68)/UDP(sport=u.sport,dport=u.dport)/Raw(load=str(d)+','+tag) #/Dot1Q(vlan=1000)
    print "Output:"
    p.show()
    sendp(p, iface=interface)
    print "-------------------------------------"


def send_pkt_dummy():
    p = Ether(src='00:00:00:00:00:11', dst='00:00:00:00:11:11') / Dot1Q(vlan=100) / IP(src='10.0.0.1', dst='10.0.0.2',
                                                                                       tos=32) / UDP(sport=1000,
                                                                                                     dport=20000) / Raw()  # /Dot1Q(vlan=1000)
    print "Output:"
    p.show()
    sendp(p, iface=interface)
    print "-------------------------------------"

def udp_incoming(pkt):
    return pkt.dst == mac and 'TCP' in pkt and 'IP' in pkt and 'Raw' in pkt

def udp_go(pkt):
    print ("Input:")
    pkt.show()
    time.sleep(1)
    send_pkt(pkt, tag)
    #send_pkt_dummy()



sniff(lfilter=udp_incoming, store=0, iface=interface, prn=udp_go)


