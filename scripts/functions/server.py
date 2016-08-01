#! /usr/bin/env python
from scapy.all import *
import fcntl, socket, struct

logfile = open("/tmp/packets.log", "w")
catchIpSrc = ""

if len(sys.argv) > 1:
    catchIpSrc = sys.argv[1]
    interface = sys.argv[2]
else:
    print "missing target src ip"
    exit()

if interface is None:
    print "missing interface"


def get_mac(iface=interface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac()
print 'mac = ' + mac
def udp_incoming(pkt):
    if 'UDP' in pkt:
        print pkt.dst
    return pkt.dst == mac and 'UDP' in pkt

i = 0
def arp_monitor_callback(pkt):
    global i
    if 'IP' in pkt:
        srcIp = pkt[IP].src
        dstIp = pkt[IP].dst
        i = i + 1
        if catchIpSrc == srcIp:
            print ("count: " + str(i) + " src " + srcIp + " > dst " + dstIp + "\n")
            time.sleep(1)

            packet = IP(dst=srcIp, tos=pkt[IP].tos)/UDP(sport=int(pkt['UDP'].dport), dport=int(pkt['UDP'].sport))/Raw(load=str(pkt['Raw'])+',server')
            send(packet)

sniff(lfilter=udp_incoming, prn=arp_monitor_callback, store=0)
