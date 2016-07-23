#!/usr/bin/python
from controllerConfigBase import ConfigBase
from subprocess import call
import time

class SfcOfL2(ConfigBase):
    def get_json_SF1(self):
        return {
            "service-function":
                {
                    "name": "SF1",
                    "type": "service-function-type:test",
                    "nsh-aware": "false",
                    "ip-mgmt-address": "10.0.0.1",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF1-plane",
                            "service-function-forwarder": "SFF2",
                            "vlan-id": 301,
                                "mac": "00:00:00:00:00:11",
                            "transport": "service-locator:mac"
                        }
                    ]
                }
        }

    def get_json_SF2(self):
        return {
            "service-function":
                {
                    "name": "SF2",
                    "type": "service-function-type:dpi",  #"service-function-type:tcp-proxy", #
                    "nsh-aware": "false",
                    "ip-mgmt-address": "10.0.0.2",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF2-plane",
                            "service-function-forwarder": "SFF2",
                            "vlan-id": 302,
                            "mac": "00:00:00:00:00:12",
                            "transport": "service-locator:mac"
                        }
                    ]
                }

        }

    def get_json_SF3(self):
        return {
            "service-function":
                {
                    "name": "SF3",
                    "type": "service-function-type:ips",  # "service-function-type:tcp-proxy", #
                    "nsh-aware": "false",
                    "ip-mgmt-address": "10.0.0.3",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF3-plane",
                            "service-function-forwarder": "SFF3",
                            "vlan-id": 303,
                            "mac": "00:00:00:00:00:13",
                            "transport": "service-locator:mac"
                        }
                    ]
                }

        }

    def get_json_SF4(self):
        return {
            "service-function":
                {
                    "name": "SF4",
                    "type": "service-function-type:fw",  # "service-function-type:tcp-proxy", #
                    "nsh-aware": "false",
                    "ip-mgmt-address": "10.0.0.4",
                    "sf-data-plane-locator": [
                        {
                            "name": "SF4-plane",
                            "service-function-forwarder": "SFF4",
                            "vlan-id": 304,
                            "mac": "00:00:00:00:00:14",
                            "transport": "service-locator:mac"
                        }
                    ]
                }

        }


    def get_json_SSF2(self):
        return {
            "service-function-forwarder":
                {
                    "name": "SFF2",
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
                            "name": "sff2-ip",
                            "data-plane-locator": {
                                "transport": "service-locator:vxlan-gpe",
                                "port": 6633,
                                "ip": self.controller
                            }
                        },
                        {
                            "name": "toSFF1", #"name": "egress",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 21,
                                    "mac": "00:00:00:00:00:EE"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "3"  # "1"
                                }
                        },
                        {
                            "name": "toSFF3",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 23,
                                    "mac": "33:00:00:00:33:33"
                                },
                                "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "4"#"1"
                                }
                        },
                        {
                            "name": "sff2-dpl-1",
                            "data-plane-locator":
                                {
                                    "vlan-id": 100,
                                    "mac": "00:00:00:11:11:11",
                                    "transport": "service-locator:mac"
                                },
                                     "service-function-forwarder-ofs:ofs-port":
                                 {
                                     #"mac": "33:33:33:33:33:33",
                                     "port-id": "1"#"3"
                                 }

                        },
                        {
                            "name": "sff2-dpl-2",
                            "data-plane-locator":
                                {
                                    "vlan-id": 200,
                                    "mac": "00:00:00:22:22:22",
                                    "transport": "service-locator:mac"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                   # "mac": "33:33:33:33:33:33",
                                    "port-id": "2"#"1"
                                }

                        }

                    ],
                   "service-function-dictionary": [
                        {
                            "name": "SF2",
                            "sff-sf-data-plane-locator": {
                                "sf-dpl-name": "SF2-plane",
                                "sff-dpl-name": "sff2-dpl-2"
                            }
                        },
                        {
                            "name": "SF1",
                            "sff-sf-data-plane-locator": {
                                "sf-dpl-name": "SF1-plane",
                                "sff-dpl-name": "sff2-dpl-1"
                            }
                        }
                    ]
                }
        }


    def get_json_SSF3(self):
        return {
            "service-function-forwarder":
                {
                    "name": "SFF3",
                    "service-node": "openflow:3",
                    "service-function-forwarder-ovs:ovs-bridge": {
                        "bridge-name": "sw3",
                        "openflow-node-id": "openflow:3"
                    },
                    "service-function-forwarder-ovs:ovs-node": {
                        "node-id": "/opendaylight-inventory:nodes/opendaylight-inventory:node[opendaylight-inventory:id='openflow:3']"
                    },
                    "sff-data-plane-locator": [
                        {
                            "name": "sff3-ip",
                            "data-plane-locator": {
                                "transport": "service-locator:vxlan-gpe",
                                "port": 6633,
                                "ip": self.controller
                            }
                        },
                        {
                            "name": "sff3-dpl-1",
                            "data-plane-locator":
                                {
                                    "vlan-id": 300,
                                    "mac": "00:00:00:11:11:13",
                                    "transport": "service-locator:mac"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    # "mac": "33:33:33:33:33:33",
                                    "port-id": "1"  # "3"
                                }

                        },
                        {
                            "name": "toSFF2",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 23,
                                    "mac": "33:00:00:00:33:11"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "2"  # "1"
                                }
                        },
                        {
                            "name": "toSFF1",#"name": "egress",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 31,
                                    "mac": "00:00:00:00:00:EE"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "3"  # "1"
                                }
                        }

                    ],
                    "service-function-dictionary": [
                        {
                            "name": "SF3",
                            "sff-sf-data-plane-locator": {
                                "sf-dpl-name": "SF3-plane",
                                "sff-dpl-name": "sff3-dpl-1"
                            }
                        }
                    ]
                }
        }

    def get_json_SSF1(self):
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
                            "name": "sff1-ip",
                            "data-plane-locator": {
                                "transport": "service-locator:vxlan-gpe",
                                "port": 6633,
                                "ip": self.controller
                            }
                        },
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
                        # {
                        #     "name": "sff1-dpl",
                        #     "data-plane-locator":
                        #         {
                        #             "transport": "service-locator:mac",
                        #             "vlan-id": 101,
                        #             "mac": "00:00:00:00:EE:EE"
                        #         },
                        #     "service-function-forwarder-ofs:ofs-port":
                        #         {
                        #             "port-id": "3"
                        #         }
                        # },
                        {
                            "name": "toSFF2",  # "name": "egress",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 21,
                                    "mac": "00:00:00:00:00:EE"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "3"  # "1"
                                }
                        },
                        {
                            "name": "toSFF3",  # "name": "egress",
                            "data-plane-locator":
                                {
                                    "transport": "service-locator:mac",
                                    "vlan-id": 31,
                                    "mac": "00:00:00:00:00:EE"
                                },
                            "service-function-forwarder-ofs:ofs-port":
                                {
                                    "port-id": "4"  # "1"
                                }
                        }

                    ],
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
                        "type": "service-function-type:dpi", #"service-function-type:tcp-proxy", #, # #"
                        "order": "1"
                    }

                ]
            }
    }

    def get_json_sfc2(self):
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
                            "type": "service-function-type:dpi",  # "service-function-type:tcp-proxy", #, # #"
                            "order": "1"
                        },
                        {
                            "name": "ips",
                            "type": "service-function-type:ips",
                            "order": "2"
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
            "classifier": "SFF1",
            "symmetric-classifier": "SFF1",
            "symmetric": "true",
            "transport-type": "service-locator:mac",
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
                "symmetric": "true",
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
                    #"access-list": "acl1",
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

if __name__ == "__main__":

    #post(controller, DEFAULT_PORT, SERVICE_FUNCTION, get_json_SF1(), True)
    #get(controller, DEFAULT_PORT, SERVICE_FUNCTION)


    #delete(controller, DEFAULT_PORT, SERVICE_CLASSIFICATION_FUNTION, True)
    deploy = SfcOfL2()
    deploy.readParameters()
    if deploy.deleteAll is True:
        print "delete all ", deploy.deleteAll
        deploy.deleteAllFlows(deploy.controller, deploy.DEFAULT_PORT, "openflow:2")
        deploy.deleteAllFlows(deploy.controller, deploy.DEFAULT_PORT, "openflow:3")

    else:
        deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH_DEL, deploy.get_json_rend_del(), True)
        deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH_DEL, deploy.get_json_rend_reverse_del(), True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_PATH, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_CHAIN, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, True)
        deploy.delete(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_CLASSIFICATION_FUNTION, True)

    call("ovs-ofctl -OOpenFlow13 del-flows sw1 udp,nw_dst=10.0.0.2", shell=True)
    call("ovs-ofctl -OOpenFlow13 del-flows sw1 ip", shell=True)
    deploy.getAndDel(deploy.controller, deploy.DEFAULT_PORT, "openflow:1", 0)
    deploy.delRemainingVlanId(deploy.controller, deploy.DEFAULT_PORT, "openflow:2", 2)
    deploy.delRemainingVlanId(deploy.controller, deploy.DEFAULT_PORT, "openflow:3", 2)

    call("ovs-ofctl -OOpenFlow13 del-flows sw2 table=10", shell=True)
    call("ovs-ofctl -OOpenFlow13 del-flows sw3 table=10", shell=True)



    print "============== All SFC configuration erased =============="
    time.sleep(2)

    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, deploy.get_json_SF1(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, deploy.get_json_SF2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION, deploy.get_json_SF3(), True)

    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, deploy.get_json_SSF1(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, deploy.get_json_SSF2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_FORWARDER, deploy.get_json_SSF3(), True)

    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_CHAIN, deploy.get_json_sfc2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_FUNCTION_PATH, deploy.get_json_sfp(), True)
    #post(controller, DEFAULT_PORT, ACCESS_CONTROL_LIST, get_json_acl(), True)
    #post(controller, DEFAULT_PORT, ACCESS_CONTROL_LIST, get_json_acl2(), True)
    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_CLASSIFICATION_FUNTION, deploy.get_json_scf(), True)


    deploy.post(deploy.controller, deploy.DEFAULT_PORT, deploy.SERVICE_RENDERED_PATH, deploy.get_json_rend(), True)
    #post(controller, DEFAULT_PORT, SERVICE_CLASSIFICATION_FUNTION, get_json_scf2(), True)

    # call("ovs-ofctl -OOpenFlow13 add-flow sw2 priority=1002,dl_src=00:00:00:00:00:ee,actions=output:3", shell=True)

    #configure cassifier
    vlanId = deploy.getVlanId(deploy.controller, deploy.DEFAULT_PORT, "openflow:2", 2)
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=1000,udp,nw_dst=10.0.0.2,actions=mod_vlan_vid:" + str(vlanId) + ",output:3", shell=True)
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=1001,ip,dl_vlan=" + str(vlanId +2)+ ",actions=output:5", shell=True)
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=1002,dl_src=00:00:00:00:00:fe,actions=normal", shell=True)
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=99,actions=normal", shell=True)

    #bidirecional
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=1000,udp,nw_dst=10.0.0.1,actions=mod_vlan_vid:" + str(vlanId + 100) + ",output:4", shell=True)
    call("ovs-ofctl -OOpenFlow13 add-flow sw1 priority=1001,ip,dl_vlan=" + str(vlanId + 102) + ",actions=output:5", shell=True)

    # call("ovs-ofctl -OOpenFlow13 add-flow sw2 priority=1001,ip,dl_vlan=" + str(vlanId + 2) + ",actions=output:3", shell=True)
    # call("ovs-ofctl -OOpenFlow13 add-flow sw2 priority=1001,ip,dl_vlan=" + str(vlanId + 102) + ",actions=output:3", shell=True)






