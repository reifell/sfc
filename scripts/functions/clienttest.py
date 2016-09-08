#!/usr/bin/python

from subprocess import call
from collections import defaultdict
import time
import subprocess
import signal
import os
import sys
from random import randint

ecn = sys.argv[1]

for n in range(1,7):
    ipId = randint(0,65535)
    p = subprocess.Popen('hping3 --udp -p 5010 -o %s --id %d  -S 10.0.0.2' %(ecn, ipId), shell=True, stdout=subprocess.PIPE, preexec_fn=os.setsid)
    ipId1 = randint(0, 65535)
    p1 = subprocess.Popen('hping3 --udp -p 5011 -o %s --id %d  -S 10.0.0.2' %(ecn, ipId1), shell=True, stdout=subprocess.PIPE, preexec_fn=os.setsid)

    print "sleep"
    time.sleep(90)
    print "sleep done"
    os.killpg(p.pid, signal.SIGTERM)
    os.killpg(p1.pid, signal.SIGTERM)
    time.sleep(2)

exit(1)


