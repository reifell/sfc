#!/usr/bin/python
from subprocess import call
import time, sys, subprocess

scf = sys.argv[1]
vlanId = sys.argv[2]
pid1 = sys.argv[3]
#pid2 = sys.argv[4]

initial_time = int(time.time())
curr_time = 0
cpuLimited = False

while curr_time  < initial_time + 400:
    print "install rule ..."  #%s hard_timeout=1,
    call('sudo ovs-ofctl -OOpenFlow13 add-flow %s hard_timeout=1,priority=1002,ip,tcp,tp_dst=5040,nw_dst=10.0.0.2,actions=mod_nw_ecn=2,mod_vlan_vid:%s,output:3' % (
    scf, str(vlanId)), shell=True)  # enter chain upstream
    call('sudo ovs-ofctl -OOpenFlow13 add-flow %s hard_timeout=1,priority=1002,ip,tcp,tp_src=5040,nw_dst=10.0.0.1,actions=mod_nw_ecn=2,mod_vlan_vid:%s,output:4' % (
       scf, str(int(vlanId) + 100)), shell=True)  # enter chain downstream

    if ((curr_time  > initial_time + 100) and (curr_time  < initial_time + 200) and not cpuLimited):
        cpuLimited = True
        print "cpu limited"
        p1 = subprocess.Popen('exec cpulimit -p %s -l 10' % (str(pid1)), shell=True)
        #p2 = subprocess.Popen('sudo cpulimit -p %s -l 10 &' % (str(pid2)), shell=True)

    if (cpuLimited and curr_time  > initial_time + 200):
        print "cpu normal", p1.pid
        call("kill %d" % p1.pid, shell=True)
        #p2.kill()
        cpuLimited = False

    time.sleep(5)
    curr_time = int(time.time())
