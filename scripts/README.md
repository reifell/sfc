

Demo Tutorial



### initial setup
cd ~/local/sfc-monitor/   # code dir
git reset head --hard SFC-trace-monitor
git pull origin SFC-trace-monitor


cd ~/local/sfc-monitor/scripts/functions

### SFs compilation
gcc sf_dummy-icmp.c -o sf_dummy-icmp
gcc sf_dummy-udptcp.c -o sf_dummy-udptcp
gcc gw.c -o gw


cd ~/local/sfc-monitor/
./sfc-karaf/target/assembly/bin/karaf clean debug    # run ODL

# on karaf

feature:install odl-sfc-monitor   # install sfc-monitor
log:tail 						  # whach ODL log


# another screen to run experiments

cd ~/local/sfc-monitor/scripts/
sudo python mininetSetup-3.py <controller IP>  # set up topology and configure chain based on file ~/local/sfc-monitor/scripts/mininetSetup-3.py

# currently 2 chain
# classifier rules:
# udp:5010 - chain 1
# udp:5020 - chain 2
# tcp:5050 - chain 1
# tcp:5040 - chain 2

# chain load examples on mininet (h1 -> h2)

h1 hping3 --udp -p 5020 -o 2  -S 10.0.0.2 -c 1  # (-o 2) to trace otherwise not trace
h1 hping3 --udp -p 5010 -o 2  -S 10.0.0.2 -c 1

h1 curl 10.0.0.2:5050/index.html > dummy 	# all traffic being traced
h1 curl 10.0.0.2:5040/index.html > dummy


# RPC to get trace
curl -u admin:admin  -X POST http://<controller IP>:8181/restconf/operations/sfc-monitor-impl:trace-sfc
# RPC to clean trace
curl -u admin:admin  -X POST http://<controller IP>:8181/restconf/operations/sfc-monitor-impl:clean-trace-sfc

