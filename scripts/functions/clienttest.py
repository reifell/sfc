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
total = []
for i in range(1,3):
    for n in range(1,20):
        ipId = randint(0,65500)
        while ipId in id:
            ipId = randint(0, 65500)

        #ipId1 = randint(0, 65500)
        #while ipId1 in id:
        #    ipId1 = randint(0, 65500)

        #id.append(ipId)
        #id.append(ipId1)

        #output1 = subprocess.check_output('hping3 --udp -p 5010 -o %s --id %d  -S 10.0.0.2 -c 1 || true' %(ecn, ipId), shell=True,  universal_newlines=True, stderr=subprocess.STDOUT, preexec_fn=os.setsid)

        output1 = subprocess.check_output('hping3 --udp -p 5010 -o %s --id %d  -S 10.0.0.2 -c 1 || true' %(ecn, ipId), shell=True,  universal_newlines=True, stderr=subprocess.STDOUT, preexec_fn=os.setsid)
        time.sleep(1)
        res = parsePing(output1)
        if (res != None):
            c1Min.append(res[0])
            c1Avg.append(res[1])
            c1Max.append(res[2])

        #res = parsePing(output2)
        #if (res != None):
        #    c2Avg.append(res[1])

    avg = float(sum(c1Avg)) / float(max(len(c1Avg), 1))
    print "c1 avg %f/%f/%f for %d itens" %(min(c1Min), avg, max(c1Max), len(c1Avg))
    total.append(avg)
    c1Min = []
    c1Max = []
    c1Avg = []
    #avg = float(sum(c2Avg)) / float(max(len(c2Avg), 1))
    #print "c2 avg %f for %d itens" %(avg, len(c2Avg))

final = float(sum(total)) / float(max(len(total), 1))
print "final avg %f" %final
exit(1)


