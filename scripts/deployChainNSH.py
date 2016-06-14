#!/usr/bin/python
from controllerConfigBase import ConfigBase
from subprocess import call
import time

class NSH(ConfigBase):
    def get_json_SF1(self):
        return {
            "service-function":
                {
                    "name": "SF1",
                    "ip-mgmt-address": "192.168.100.110",
                    "type": "service-function-type:test",
                    "nsh-aware": "false",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF1-plane",
                            "port": 6633,
                            "ip": "192.168.100.110",
                            "transport": "service-locator:vxlan-gpe",
                            "service-function-forwarder": "SFF0"
                        }
                    ]
                }
        }

    def get_json_SF2(self):
        return {
            "service-function":
                {
                    "name": "SF2",
                    "ip-mgmt-address": "192.168.100.111",
                    "type": "service-function-type:dpi",
                    "nsh-aware": "false",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF2-plane",
                            "port": 6633,
                            "ip": "192.168.100.111",
                            "transport": "service-locator:vxlan-gpe",
                            "service-function-forwarder": "SFF0"
                        }
                    ]
                }

        }

    def get_json_SSF1(self):
        return {
            "service-function-forwarder":
                {
                    "name": "SFF0",
                    "service-node": "openflow:2",
                    "service-function-forwarder-ovs:ovs-bridge": {
                        "bridge-name": "sw2",
                        "openflow-node-id": "openflow:1"
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
                                "ip": "192.168.100.104"
                            }
                        }
                    ],
                    "service-function-dictionary": [
                        {
                            "name": "SF2",
                            "sff-sf-data-plane-locator": {
                                "sf-dpl-name": "SF2-plane",
                                "sff-dpl-name": "sff0-dpl"
                            }
                        },
                        {
                            "name": "SF1",
                            "sff-sf-data-plane-locator": {
                                "sf-dpl-name": "SF1-plane",
                                "sff-dpl-name": "sff0-dpl"
                            }
                        }
                    ]
                }
        }

    def get_json_SSF2(self):
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

    def get_service_function_chains_uri(self):
        return "/restconf/config/service-function-chain:service-function-chains/"

    def get_json_sfc(self):
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

    def get_json_sfp(self):
        return {
            "service-function-path":
                {
                    "name": "c1-path",
                    "service-chain-name": "c1",
                    "classifier": "classifier1",
                    "symmetric-classifier": "classifier2",
                    "symmetric": "true",
                    "context-metadata": "NSH1"
                }
        }

    def get_json_acl(self):
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

    def get_json_acl2(self):
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

    def get_json_rend(self):
        return {
            "input": {
                "name": "c1-path-rend",
                "parent-service-function-path": "c1-path",
                "symmetric": "true"
            }
        }

    def get_json_rend_del(self):
        return {
            "input": {
                "name": "c1-path-rend",
            }
        }

    def get_json_rend_reverse_del(self):
        return {
            "input": {
                "name": "c1-path-rend-Reverse",
            }
        }

    def get_json_scf(self):
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

    def get_json_scf2(self):
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

    def get_metadata(self):
        return {
            "context-metadata":
                {
                    "name": "NSH1",
                    "context-header1": "1",
                    "context-header2": "1",
                    "context-header3": "1",
                    "context-header4": "1"
                }
        }

if __name__ == "__main__":

    deploy = NSH()
    deploy.readParameters()
    if deploy.deleteAll is True:
        print "delete all ", deploy.deleteAll
        deploy.deleteAllFlows(deploy.controller, deploy.DEFAULT_PORT, "openflow:2")
    else:
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_CLASSIFICATION_FUNTION, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.ACCESS_CONTROL_LIST, True)
        deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH_DEL, deploy.get_json_rend_del(), True)
        deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH_DEL, deploy.get_json_rend_reverse_del(), True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.CONTEXT_METADATA, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_PATH, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_CHAIN, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, True)

    deploy.getAndDel(deploy.controller, deploy.DEFAULT_PORT, "openflow:1", 0)

    print "============== All SFC configuration erased =============="
    time.sleep(2)

    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, deploy.get_json_SF1(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, deploy.get_json_SF2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, deploy.get_json_SSF1(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, deploy.get_json_SSF2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_CHAIN, deploy.get_json_sfc(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_PATH, deploy.get_json_sfp(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.ACCESS_CONTROL_LIST, deploy.get_json_acl(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.ACCESS_CONTROL_LIST, deploy.get_json_acl2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.CONTEXT_METADATA, deploy.get_metadata(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH, deploy.get_json_rend(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_CLASSIFICATION_FUNTION, deploy.get_json_scf(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_CLASSIFICATION_FUNTION, deploy.get_json_scf2(), True)

    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=99,actions=normal", shell=True)


