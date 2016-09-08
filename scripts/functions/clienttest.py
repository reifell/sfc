#!/usr/bin/python

from subprocess import call
from collections import defaultdict
import time
import subprocess
import signal
import os
import sys
import re
from random import randint

def parsePing(output):
    output = output.split(',')
    res = output[3].split("\n")
    if (len(output) < 4):
        return None
    if (' 0% packet loss' not in res):
        print res
        return None
    ping_data = res[1].split('=')
    non_decimal = re.compile(r'[^\d.]+')
    return (float(non_decimal.sub('', ping_data[1].split("/")[0])), float(non_decimal.sub('', ping_data[1].split("/")[0])), float(non_decimal.sub('', ping_data[1].split("/")[0])))


ecn = sys.argv[1]
id = []
c1Min = []
c1Max = []
c1Avg = []
c2Min = []
c2Max = []
c2Avg = []
for n in range(1,200):
    ipId = randint(0,65500)
    while ipId in id:
        ipId = randint(0, 65500)
    # p = subprocess.Popen('hping3 --udp -p 5010 -o %s --id %d  -S 10.0.0.2 -c 1' %(ecn, ipId), shell=True, stdout=subprocess.PIPE, preexec_fn=os.setsid)
    # ipId1 = randint(0, 65500)
    # p.wait()
    ipId1 = randint(0, 65500)
    while ipId1 in id:
        ipId1 = randint(0, 65500)
    # p1 = subprocess.Popen('hping3 --udp -p 5011 -o %s --id %d  -S 10.0.0.2 -c 1' %(ecn, ipId1), shell=True, stdout=subprocess.PIPE, preexec_fn=os.setsid)
    # p1.wait()
    id.append(ipId)
    id.append(ipId1)

    output1 = subprocess.check_output('hping3 --udp -p 5010 -o %s --id %d  -S 10.0.0.2 -c 1 || true' %(ecn, ipId), shell=True,  universal_newlines=True, stderr=subprocess.STDOUT, preexec_fn=os.setsid)

    output2 = subprocess.check_output('hping3 --udp -p 5011 -o %s --id %d  -S 10.0.0.2 -c 1 || true' %(ecn, ipId1), shell=True,  universal_newlines=True, stderr=subprocess.STDOUT, preexec_fn=os.setsid)
    res = parsePing(output1)
    if (res != None):

        c1Avg.append(res[1])
    res = parsePing(output2)
    if (res != None):
        c2Avg.append(res[1])


avg = float(sum(c1Avg)) / float(max(len(c1Avg), 1))
print "c1 avg %f for %d itens" %(avg, len(c1Avg))

avg = float(sum(c2Avg)) / float(max(len(c2Avg), 1))
print "c2 avg %f for %d itens" %(avg, len(c2Avg))
exit(1)


