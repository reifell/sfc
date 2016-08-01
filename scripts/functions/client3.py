#! /usr/bin/env python
import argparse
import logging
logging.getLogger("scapy.runtime").setLevel(logging.ERROR)
from scapy.all import *
import fcntl, socket, sys, struct, time
import numpy

SRC_IP = '10.0.0.1'

def def_args():
    parser = argparse.ArgumentParser(description='Command line parser')
    parser.add_argument('-dst', '--dst', type=str, help='Destination IP', required=True)
    parser.add_argument('-i', '--iface', type=str, help='source interface', required=True)
    parser.add_argument('-d', '--dport', type=int, help='Destination Port', required=True)
    parser.add_argument('-pr', '--probe', type=int, help='probeflag', required=True)
    args = parser.parse_args()

    ip = args.dst
    iface = args.iface
    dport = args.dport
    probe = args.probe

    return ip, iface, dport, probe



ip, iface, dport, probe = def_args()
msg = "hello"

def get_mac(iface=iface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])


mac = get_mac()
print 'mac = ' + mac


def send_pkt_dummy():
    p = IP(src='10.0.0.1', dst=ip,tos=probe) / UDP(sport=1000, dport=dport) / Raw(load='probe')  # /Dot1Q(vlan=1000)
    send(p)



# sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#
# if (20000):
#     sock.bind((SRC_IP, 20000))
#     print "sending '%s' from %s:%d to %s:%d" % (msg, SRC_IP, 20000, ip, dport)
# else:
#     print "sending '%s' to %s:%d" % (msg, ip, dport)

#sock.sendto(msg, (ip, dport))


values = []
t1 = int(round(time.time() * 1000))
send_pkt_dummy()

def udp_incoming(pkt):
    return pkt.dst == mac and 'UDP' in pkt
def udp_callback(pkt):
    t2 = int(round(time.time() * 1000))
    global values, t1
    delta = t2 - t1
    print "delta time is -- " , delta
    values.append(delta)
    t1 = int(round(time.time() * 1000))
    send_pkt_dummy()
    if len(values) > 50:
        median = numpy.median(values)
        print "median time is -- ",median
        exit(0)


sniff(lfilter=udp_incoming, store=0, iface=iface, prn=udp_callback)

