#!/usr/bin/python

from subprocess import call
from collections import defaultdict
import time
import subprocess
import signal
import os
import sys

ecn = sys.argv[1]

for n in range(1,7):
    p = subprocess.Popen('hping3 --udp -p 5555 -o %s  -S 10.0.0.2' %(ecn), shell=True, stdout=subprocess.PIPE, preexec_fn=os.setsid)
    print "sleep"
    time.sleep(90)
    print "sleep done"
    os.killpg(p.pid, signal.SIGTERM)
    time.sleep(2)

exit(1)


