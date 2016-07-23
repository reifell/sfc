#! /usr/bin/env python
import argparse
import logging
logging.getLogger("scapy.runtime").setLevel(logging.ERROR)
from scapy.all import *
import fcntl, socket, sys, struct

SRC_IP = '10.0.0.1'

def def_args():
    parser = argparse.ArgumentParser(description='Command line parser')
    parser.add_argument('-dst', '--dst', type=str, help='Destination IP', required=True)
    parser.add_argument('-i', '--iface', type=str, help='source interface', required=True)
    parser.add_argument('-d', '--dport', type=int, help='Destination Port', required=True)
    args = parser.parse_args()

    ip = args.dst
    iface = args.iface
    dport = args.dport

    return ip, iface, dport



ip, iface, dport = def_args()
msg = "hello"

def get_mac(iface=iface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac()
print 'mac = ' + mac


def send_pkt_dummy():
    p = IP(src='10.0.0.1', dst='10.0.0.2',tos=3) / UDP(sport=1000, dport=20000) / Raw(load='probe')  # /Dot1Q(vlan=1000)
    print "Output:"
    p.show()
    send(p)
    print "-------------------------------------"



sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

if (20000):
    sock.bind((SRC_IP, 20000))
    print "sending '%s' from %s:%d to %s:%d" % (msg, SRC_IP, 20000, ip, dport)
else:
    print "sending '%s' to %s:%d" % (msg, ip, dport)

#sock.sendto(msg, (ip, dport))
send_pkt_dummy()

def udp_incoming(pkt):
    return pkt.dst == mac and 'UDP' in pkt
def udp_callback(pkt):
    print "packet captured (tags): ", str(pkt[Raw])
    print "---"
    exit(0)

sniff(lfilter=udp_incoming, store=0, iface=iface, prn=udp_callback)
