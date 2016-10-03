#include <stdio.h> //For standard things
#include <stdlib.h>    //malloc
#include <string.h>    //memset
#include <netinet/ip_icmp.h>   //Provides declarations for icmp header
#include <netinet/udp.h>   //Provides declarations for udp header
#include <netinet/tcp.h>   //Provides declarations for tcp header
#include <netinet/ip.h>    //Provides declarations for ip header
#include <sys/socket.h>
#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <net/if.h>
#include <netinet/ether.h>

#define ETHER_TYPE	0x0800
#define ICMP  1
#define TCP 6
#define UDP 17
#define IGMP 2

int ProcessPacket(unsigned char* , int);
unsigned char * getMacAddress (char*);

int tcp=0,udp=0,icmp=0,others=0,igmp=0,total=0;

int main(int argc, char **argv)
{

    if (argc < 2) {
        perror("invalid input\n");
        return 1;
    }

    char *iface;
    iface = argv[1]; //"eth0";

    unsigned char * mac = getMacAddress(iface);
    printf("macc : %.2X:%.2X:%.2X:%.2X:%.2X:%.2X\n" , mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    if (mac == NULL) {
        perror("could not get mac address");
    }

    int saddr_size , data_size, daddr_size, bytes_sent;
    struct sockaddr_ll saddr, daddr;
    unsigned char *buffer=malloc(65535);
    struct ether_header *eh = (struct ether_header *) buffer;
//    struct iphdr *iph = (struct iphdr*)buffer;

    int sock_raw = socket( AF_PACKET , SOCK_RAW , htons(ETH_P_ALL)) ; //For receiving
    int sock = socket( PF_PACKET , SOCK_RAW , IPPROTO_RAW) ;            //For sending

    memset(&saddr, 0, sizeof(struct sockaddr_ll));
    saddr.sll_family = AF_PACKET;
    saddr.sll_protocol = htons(ETH_P_ALL);
    saddr.sll_ifindex = if_nametoindex(iface);
    if (bind(sock_raw, (struct sockaddr*) &saddr, sizeof(saddr)) < 0) {
        perror("bind failed\n");
        close(sock_raw);
    }
    if (setsockopt(sock_raw, SOL_SOCKET, SO_BINDTODEVICE, iface, sizeof iface) < 0) {
        perror("Socket interface Error\n");
        fflush(stdout);
        return 1;
    } 

    memset(&daddr, 0, sizeof(struct sockaddr_ll));
    daddr.sll_family = AF_PACKET;
    daddr.sll_protocol = htons(ETH_P_ALL);
    daddr.sll_ifindex = if_nametoindex(iface);
    if (bind(sock, (struct sockaddr*) &daddr, sizeof(daddr)) < 0) {
      perror("bind failed\n");
      close(sock);
    }
    struct ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), iface);
    if (setsockopt(sock, SOL_SOCKET, SO_BINDTODEVICE, (void *)&ifr, sizeof(ifr)) < 0) {
        char *errormsg = malloc(50);
        sprintf(errormsg, "bind to %s", iface);
        perror(errormsg);
        }

    while(1)
    {
        saddr_size = sizeof (struct sockaddr);
        daddr_size = sizeof (struct sockaddr);
        //Receive a packet

        data_size = recvfrom(sock_raw , buffer , 65536 , 0 ,(struct sockaddr *) &saddr , (socklen_t*)&saddr_size);
        printf("MacPkt src: %.2X:%.2X:%.2X:%.2X:%.2X:%.2X\n" , eh->ether_shost[0], eh->ether_shost[1], eh->ether_shost[2], eh->ether_shost[3], eh->ether_shost[4], eh->ether_shost[5]);
        printf("MacIface : %.2X:%.2X:%.2X:%.2X:%.2X:%.2X\n" , mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        fflush(stdout);
        if (eh->ether_dhost[0] == mac[0] &&
            eh->ether_dhost[1] == mac[1] &&
            eh->ether_dhost[2] == mac[2] &&
            eh->ether_dhost[3] == mac[3] &&
            eh->ether_dhost[4] == mac[4] &&
            eh->ether_dhost[5] == mac[5]) {
                printf("Correct destination MAC address\n");
                int ipDst = ProcessPacket(buffer , data_size);
                if(data_size < 0 ) {
                    printf("Recvfrom error , failed to get packets\n");
                    return 1;
                }
                else {
                    printf("Received %d bytes\n",data_size);

                    printf("ip value dst %d\n", ipDst);

                    if (ipDst == 33554442) {
                        eh->ether_dhost[0] = 0;
                        eh->ether_dhost[1] = 0;
                        eh->ether_dhost[2] = 0;
                        eh->ether_dhost[3] = 0;
                        eh->ether_dhost[4] = 0;
                        eh->ether_dhost[5] = 2;
                    } else if(ipDst == 16777226) {
                        eh->ether_dhost[0] = 0;
                        eh->ether_dhost[1] = 0;
                        eh->ether_dhost[2] = 0;
                        eh->ether_dhost[3] = 0;
                        eh->ether_dhost[4] = 0;
                        eh->ether_dhost[5] = 1;
                    } else {
                        continue;
                    }


                    eh->ether_shost[0] = mac[0];
                    eh->ether_shost[1] = mac[1];
                    eh->ether_shost[2] = mac[2];
                    eh->ether_shost[3] = mac[3];
                    eh->ether_shost[4] = mac[4];
                    eh->ether_shost[5] = mac[5];


                    //Huge code to process the packet (optional)

                    //Send the same packet out
                    bytes_sent=write(sock,buffer,data_size);
                    printf("Sent %d bytes\n",bytes_sent);
//                    struct sockaddr_in self;
//                        self.sin_family = AF_INET;
//                        self.sin_addr.s_addr = iph->saddr
//                        self.sin_port = htons(1234);
//                        memset(self.sin_zero, '\0', sizeof(self.sin_zero));
//                    struct sockaddr_in tx;
                    //Init network IP information
                      //for routing and transmission
                      //so kernel can prepare layer 1 data
//                      memset(&tx, 0, sizeof(tx));
//                      tx.sin_family      = AF_INET;
//                      tx.sin_port        = htons(1337);
//                      tx.sin_addr.s_addr = inet_addr("127.0.0.1");

//                    if (sendto(sock, buffer, sizeof buffer, 0, (struct sockaddr *)&tx,sizeof(tx)) < 0) {
//                        perror("sendto");
//                        exit(1);
//                    }
                }
                fflush(stdout);
        } else {
            printf("wrong dst\n");
            fflush(stdout);
        }

    }
    close(sock_raw);
    return 0;
}

int ProcessPacket(unsigned char* buffer, int size)
{
    //Get the IP Header part of this packet
    ++total;
    struct iphdr *iph = (struct iphdr *) (buffer + sizeof(struct ether_header));
    //struct udphdr *udph = (struct udphdr *) (buffer + sizeof(struct iphdr) + sizeof(struct ether_header));

    switch (iph->protocol) //Check the Protocol and do accordingly...
    {
        case ICMP:  //ICMP Protocol
            ++icmp;
            //PrintIcmpPacket(Buffer,Size);
            break;

        case IGMP:  //IGMP Protocol
            ++igmp;
            break;

        case TCP:  //TCP Protocol
            ++tcp;
            //print_tcp_packet(buffer , size);
            break;

        case UDP: //UDP Protocol
            ++udp;
            //print_udp_packet(buffer , size);
            //Receive a packet
            //return udph->dest;
            break;

        default: //Some Other Protocol like ARP etc.
            ++others;
            break;
    }
    //iph->ttl--;
    printf("TCP : %d   UDP : %d   ICMP : %d   IGMP : %d   Others : %d   Total : %d\n",tcp,udp,icmp,igmp,others,total);
    fflush(stdout);
    return iph->daddr;
}

unsigned char * getMacAddress (char *iface) {
    int fd;
    struct ifreq ifr;
    unsigned char *mac = NULL;
    unsigned char * macReturn = malloc(8);
    memset(&ifr, 0, sizeof(ifr));

    fd = socket(AF_INET, SOCK_DGRAM, 0);

    ifr.ifr_addr.sa_family = AF_INET;
    strncpy(ifr.ifr_name , iface , IFNAMSIZ-1);


    if (0 == ioctl(fd, SIOCGIFHWADDR, &ifr)) {
        mac = (unsigned char *)ifr.ifr_hwaddr.sa_data;

        //display mac address
        printf("Mac : %.2X:%.2X:%.2X:%.2X:%.2X:%.2X\n" , mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        fflush(stdout);
    }
    memcpy(macReturn, mac, 8);

    close(fd);

    return macReturn;
}