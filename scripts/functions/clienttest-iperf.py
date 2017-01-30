#!/usr/bin/python

from subprocess import call
from collections import defaultdict
import time
import subprocess
import signal
import os
import sys
from random import randint

for n in range(0,200):

    rate_limit = 20 + (n * 10)
    print "next bw %d" %rate_limit
    output1 = subprocess.check_output('iperf -u -c 10.0.0.2 --port 5010 -b %dM --time 40' % (rate_limit),
                                      shell=True, universal_newlines=True, stderr=subprocess.STDOUT,
                                      preexec_fn=os.setsid)
    print output1

exit(1)


