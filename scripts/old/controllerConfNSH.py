import argparse
import requests, json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os
import pprint

controller = '192.168.100.105'
DEFAULT_PORT = '8181'

USERNAME = 'admin'
PASSWORD = 'admin'

SERVICE_FUNCTION = '/restconf/config/service-function:service-functions'
SERVICE_FUNCTION_FORWARDER = '/restconf/config/service-function-forwarder:service-function-forwarders'
SERVICE_FUNCTION_CHAIN ='/restconf/config/service-function-chain:service-function-chains'
SERVICE_FUNCTION_PATH = '/restconf/config/service-function-path:service-function-paths'
ACCESS_CONTROL_LIST='/restconf/config/ietf-access-control-list:access-lists'
SERVICE_RENDERED_PATH = '/restconf/operations/rendered-service-path:create-rendered-path'
SERVICE_CLASSIFICATION_FUNTION = '/restconf/config/service-function-classifier:service-function-classifiers'
SERVICE_RENDERED_PATH_DEL = '/restconf/operations/rendered-service-path:delete-rendered-path'


def get(host, port, uri):
    url = 'http://' + host + ":" + port + uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata = json.loads(r.text)
    pprint(jsondata)
    import pdb
    pdb.set_trace()
    print jsondata
    return jsondata

def getFlowOnJson(json):
    if 'flow-node-inventory:table' in json:
        for key1 in json['flow-node-inventory:table']:
            if 'flow' in key1:
                for key2 in key1['flow']:
                    if 'flow-name' in key2:
                        return key2
    return None


def searchAndDeleteMatchAny(json, uri, host, port):
    remaning = True
    while remaning:
        key2 = getFlowOnJson(json)
        if key2 != None and key2['flow-name'] == 'MatchAny':
            uri2 = uri + '/flow/' + key2['id']
            delete(host, port, uri2)
        else:
            remaning = False


def searchVlanID(json):
    vlanid = []
    if 'flow-node-inventory:table' in json:
        for key1 in json['flow-node-inventory:table']:
            if 'flow' in key1:
                for key2 in key1['flow']:
                    if 'flow-name' in key2:
                        if key2 != None and key2['flow-name'] == 'nextHop' and key2['priority'] == 450:
                            vlanid.append(key2['match']['vlan-match']['vlan-id']['vlan-id'])
    vl = min(int(s) for s in vlanid)
    print vl
    return vl

def searchAndDeleteVlanID(json, uri, host, port):
    remaning = True
    while remaning:
        key2 = getFlowOnJson(json)
        if key2 != None and key2['flow-name'] == 'nextHop' and key2['priority'] == 450:
            uri2 = uri + '/flow/' + key2['id']
            delete(host, port, uri2)
        else:
            remaning = False



def getVlanId(host, port, sw, table):
    uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/' + str(table)
    url = 'http://' + host + ":" + port + uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    j = json.loads(r.text)
    return searchVlanID(j)


def getAndDel(host, port, sw, table):
    uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/'+str(table)
    url = 'http://' + host + ":" + port + uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    j = json.loads(r.text)
    searchAndDeleteMatchAny(j, uri, host, port)

def delRemainingVlanId(host, port, sw, table):
    uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/'+str(table)
    url = 'http://' + host + ":" + port + uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    j = json.loads(r.text)
    searchAndDeleteVlanID(j, uri, host, port)


def delete(host, port, uri, debug=False):
    '''Perform a DELETE rest operation, using the URL and data provided'''

    url = 'http://' + host + ":" + port + uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "DELETE %s" % url
    r = requests.delete(url, headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
        print r.text
    r.raise_for_status()


def put(host, port, uri, data, debug=False):
    '''Perform a PUT rest operation, using the URL and data provided'''

    url = 'http://' + host + ":" + port + uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()
    time.sleep(2)


def post(host, port, uri, data, debug=False):
    '''Perform a POST rest operation, using the URL and data provided'''

    url = 'http://' + host + ":" + port + uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "POST %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()
    time.sleep(2)


# def get_json_SF1():
#     return {
#         "service-functions": {
#             "service-function": [
#                 {
#                     "name": "SF1",
#                     "ip-mgmt-address": "10.0.0.1",
#                     "type": "service-function-type:test",
#                     "nsh-aware": "false",
#                     "sf-data-plane-locator": [
#                         {
#                             "name": "SF1-plane",
#                             "port": 6002,
#                             "ip": "10.0.0.1",
#                             "transport": "service-locator:vxlan-gpe",
#                             "service-function-forwarder": "SFF0"
#                         }
#                     ]
#                 }
#             ]
#         }
#     }


# def get_json_SF2():
#     return {
#         "service-functions": {
#             "service-function": [
#                 {
#                     "name": "SF2",
#                     "ip-mgmt-address": "10.0.0.2",
#                     "type": "service-function-type:dpi",
#                     "nsh-aware": "false",
#                     "sf-data-plane-locator": [
#                         {
#                             "name": "SF1-plane",
#                             "port": 6002,
#                             "ip": "10.0.0.2",
#                             "transport": "service-locator:vxlan-gpe",
#                             "service-function-forwarder": "SFF0"
#                         }
#                     ]
#                 }
#             ]
#         }
#     }


def get_json_SF1():
    return {
        "service-function":
            {
                "name": "SF1",
                "type": "service-function-type:test",
                "nsh-aware": "true",
                "ip-mgmt-address": "10.0.0.11",
                "sf-data-plane-locator": [
                    {
                        "name": "SF1-plane",
                        "service-function-forwarder": "SFF0",
                        "port": 4789,
                        "ip": "10.0.0.11",
                        "transport": "service-locator:vxlan-gpe",
                    }
                ]
            }
    }
def get_json_SF2():
    return {
        "service-function":
            {
                "name": "SF2",
                "type": "service-function-type:dpi",
                "nsh-aware": "true",
                "ip-mgmt-address": "10.0.0.12",
                "sf-data-plane-locator": [
                    {
                        "name": "SF2-plane",
                        "service-function-forwarder": "SFF0",
                        "port": 4789,
                        "ip": "10.0.0.12",
                        "transport": "service-locator:vxlan-gpe",
                    }
                ]
            }

    }

def get_json_SSF1():
    return {
        "service-function-forwarder":
            {
                "name": "SFF0",
                "service-node": "openflow:2",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "sw2",
                    "openflow-node-id": "openflow:2"
                },
                "service-function-forwarder-ovs:ovs-node": {
                    "node-id": "/opendaylight-inventory:nodes/opendaylight-inventory:node[opendaylight-inventory:id='openflow:2']"
                },
                "sff-data-plane-locator": [

                    {
                        "name": "sff0-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 6633,
                            "ip": "192.168.100.105"
                        }
                    },

                    {
                        "name": "sff0-dpl-1",
                        "data-plane-locator":
                            {
                                "ip": "10.0.0.11",
                                "port": 4789,
                                "transport": "service-locator:vxlan-gpe"
                            },
                        "service-function-forwarder-ofs:ofs-port":
                            {
                                "port-id": "1"  # "1"
                            }
                    },
                    {
                        "name": "sff0-dpl-2",
                        "data-plane-locator":
                            {
                                "ip": "10.0.0.12",
                                "port": 4789,
                                "transport": "service-locator:vxlan-gpe"
                            },
                        "service-function-forwarder-ofs:ofs-port":
                            {
                                "port-id": "2"  # "1"
                            }
                    },

                ],

                "service-function-dictionary": [
                    {
                        "name": "SF2",
                        "sff-sf-data-plane-locator": {
                            "sf-dpl-name": "SF2-plane",
                            "sff-dpl-name": "sff0-dpl-2"
                        }
                    },
                    {
                        "name": "SF1",
                        "sff-sf-data-plane-locator": {
                            "sf-dpl-name": "SF1-plane",
                            "sff-dpl-name": "sff0-dpl-1"
                        }
                    }
                ]
            }
    }

def get_json_SSF2():
    return {
                "service-function-forwarder":
            {
                "name": "SFF1",
                "service-node": "openflow:1",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "sw1",
                    "openflow-node-id": "openflow:1"
                },
                "sff-data-plane-locator": [
                    {
                        "name": "sw1-eth1",
                        "data-plane-locator": {
                            "transport": "service-locator:mac"
                        }
                    },
                    {
                        "name": "sw1-eth2",
                        "data-plane-locator": {
                            "transport": "service-locator:mac"
                        }
                    },
                    {
                        "name": "sff1-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 6633,
                            "ip": "192.168.100.105"
                        }
                    }
                ]
            }
    }


def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_json_sfc():
    return {
    "service-function-chain":
        {
            "name": "c1",
            "sfc-service-function": [
                {
                    "name": "test",
                    "type": "service-function-type:test",
                    "order": 0
                },
                {
                    "name": "dpi",
                    "type": "service-function-type:dpi",
                    "order": "1"
                }
            ]
        }
}


def get_json_sfp():
    return {
    "service-function-path":
      {
        "name": "c1-path",
        "service-chain-name": "c1",
        "classifier": "classifier1",
        "symmetric-classifier": "classifier2",
        "symmetric": "false",
        "transport-type": "service-locator:vxlan-gpe",
        "context-metadata": "NSH1"
      }
}


def get_json_acl():
    return {
        "acl": {
            "access-list-entries": {
                "ace": [
                    {
                        "matches": {
                            "destination-ipv4-network": "10.0.0.2/0",
                            "protocol": "17",
                            "source-port-range": {
                                "lower-port": "0",
                                "upper-port": "65000"
                            },
                            "destination-port-range": {
                                "lower-port": "0",
                                "upper-port": "65000"
                            }
                        },
                        "actions": {
                            "service-function-acl:rendered-service-path": "c1-path-rend"
                        },
                        "rule-name": "rule1"
                    }
                ]
            },
            "acl-name": "acl1"
        }
    }


def get_json_acl2():
    return {
        "acl": {
            "access-list-entries": {
                "ace": [
                    {
                        "matches": {
                            "destination-ipv4-network": "10.0.0.1/0",
                            "protocol": "17",
                            "source-port-range": {
                                "lower-port": "0",
                                "upper-port": "65000"
                            },
                            "destination-port-range": {
                                "lower-port": "0",
                                "upper-port": "65000"
                            }
                        },
                        "actions": {
                            "service-function-acl:rendered-service-path": "c1-path-rend-Reverse"
                        },
                        "rule-name": "rule2"
                    }
                ]
            },
            "acl-name": "acl2"
        }
    }


def get_json_rend():
    return {
        "input": {
            "name": "c1-path-rend",
            "parent-service-function-path": "c1-path",
            "symmetric": "false",
        }
    }

def get_json_rend_del():
    return {
        "input": {
            "name": "c1-path-rend",
        }
    }

def get_json_rend_reverse_del():
    return {
        "input": {
            "name": "c1-path-rend-Reverse",
        }
    }

def get_json_scf():
    return {
        "service-function-classifier":
            {
                "name": "classifier1",
                "access-list": "acl1",
                "scl-service-function-forwarder": [
                    {
                        "name": "SFF1",
                        "interface": "sw1-eth1"
                    }
                ]
            }

    }

def get_json_scf2():
    return {
        "service-function-classifier":
            {
                "name": "classifier2",
                "access-list": "acl2",
                "scl-service-function-forwarder": [
                    {
                        "name": "SFF1",
                        "interface": "sw1-eth2"
                    }
                ]
            }

    }

if __name__ == "__main__":

    #post(controller, DEFAULT_PORT, SERVICE_FUNCTION, get_json_SF1(), True)
    #get(controller, DEFAULT_PORT, SERVICE_FUNCTION)


    #delete(controller, DEFAULT_PORT, SERVICE_CLASSIFICATION_FUNTION, True)
    delete(controller, DEFAULT_PORT, ACCESS_CONTROL_LIST, True)
    post(controller, DEFAULT_PORT, SERVICE_RENDERED_PATH_DEL, get_json_rend_del(), True)
    post(controller, DEFAULT_PORT, SERVICE_RENDERED_PATH_DEL, get_json_rend_reverse_del(), True)
    delete(controller, DEFAULT_PORT, SERVICE_FUNCTION_PATH, True)
    delete(controller, DEFAULT_PORT, SERVICE_FUNCTION_CHAIN, True)
    delete(controller, DEFAULT_PORT, SERVICE_FUNCTION_FORWARDER, True)
    delete(controller, DEFAULT_PORT, SERVICE_FUNCTION, True)

    getAndDel(controller, DEFAULT_PORT, "openflow:1", 0)
    #delRemainingVlanId(controller, DEFAULT_PORT, "openflow:2", 2)

    print "============== All SFC configuration erased =============="
    time.sleep(2)

    post(controller, DEFAULT_PORT, SERVICE_FUNCTION, get_json_SF1(), True)
    post(controller, DEFAULT_PORT, SERVICE_FUNCTION, get_json_SF2(), True)
    post(controller, DEFAULT_PORT, SERVICE_FUNCTION_FORWARDER, get_json_SSF1(), True)
    post(controller, DEFAULT_PORT, SERVICE_FUNCTION_FORWARDER, get_json_SSF2(), True)
    post(controller, DEFAULT_PORT, SERVICE_FUNCTION_CHAIN, get_json_sfc(), True)
    post(controller, DEFAULT_PORT, SERVICE_FUNCTION_PATH, get_json_sfp(), True)
    post(controller, DEFAULT_PORT, ACCESS_CONTROL_LIST, get_json_acl(), True)
    post(controller, DEFAULT_PORT, ACCESS_CONTROL_LIST, get_json_acl2(), True)
    post(controller, DEFAULT_PORT, SERVICE_RENDERED_PATH, get_json_rend(), True)
    #post(controller, DEFAULT_PORT, SERVICE_CLASSIFICATION_FUNTION, get_json_scf(), True)
    #post(controller, DEFAULT_PORT, SERVICE_CLASSIFICATION_FUNTION, get_json_scf2(), True)




