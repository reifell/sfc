from scapy.all import *
from scapy.contrib.nsh import *
from scapy.layers.vxlan import *
from scapy.layers.l2 import *

import fcntl, socket, struct

print 'starting service function ...'
interface = sys.argv[1]
# call('iptables -F', shell=True)
# call('iptables -I INPUT -p udp -i sf1-eth0 -j DROP', shell=True)


def get_mac(iface=interface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac()
print 'mac = ' + mac
def udp_incoming(pkt):
    return pkt.dst == mac and NSH in pkt




def udp_go(pkt):
    pkt.show()
    pkt[NSH].NSI -= 1
    pkt[UDP][Ether].show()
    sendp(pkt[UDP][Ether], iface='vxlan0')




sniff(lfilter=udp_incoming, store=0, iface='eth1', prn=lambda x : udp_go(x))
