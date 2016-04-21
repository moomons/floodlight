/**
 *
 * Created by mo on 4/21/16.
 *
 * I mainly followed the documentation here: https://floodlight.atlassian.net/wiki/display/floodlightcontroller/How+to+Write+a+Module
 *
 */

package net.floodlightcontroller.monsmodule;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.*;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class monsModule implements IOFMessageListener, IFloodlightModule {

    protected IFloodlightProviderService floodlightProvider;
    protected Set<Long> macAddresses;
    protected static Logger logger;

    @Override
    public String getName() {
        // We also have to put in an ID for our OFMessage listener. This is done in the getName() call.
        return monsModule.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        // Now we need to wire it up to the module loading system. We tell the module loader we depend on it by modifying the getModuleDependencies() function.
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        // Init is called early in the controller startup process — it primarily is run to load dependencies and initialize datastructures.
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        macAddresses = new ConcurrentSkipListSet<Long>();
        logger = LoggerFactory.getLogger(monsModule.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        // implement the basic listener. We'll register for PACKET_IN messages in our startUp method. Here we are assured other modules we depend on are already initialized.
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }


    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        // Now we have to define the behavior we want for PACKET_IN messages.
        // Note that we return Command.CONTINUE to allow this message to continue to be handled by other PACKET_IN handlers as well.

        // If you would like more information on how to inspect higher-layer packet headers like IP, TCP, etc., please refer to this tutorial.
        // TODO: Definitely! This is exactly what I want!
        // Link; https://floodlight.atlassian.net/wiki/display/floodlightcontroller/How+to+Process+a+Packet-In+Message

//        Ethernet eth =
//                IFloodlightProviderService.bcStore.get(cntx,
//                        IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
//        Long sourceMACHash = eth.getSourceMACAddress().getLong();
//        if (!macAddresses.contains(sourceMACHash)) {
//            macAddresses.add(sourceMACHash);
//            logger.info("MAC Address: {} seen on switch: {}",
//                    eth.getSourceMACAddress().toString(),
//                    sw.getId().toString());
//        }

        switch (msg.getType()) {
            case PACKET_IN:
                /* Retrieve the deserialized packet in message */
                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

                /* Various getters and setters are exposed in Ethernet */
                MacAddress srcMac = eth.getSourceMACAddress();
                VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());

                /*
                 * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
                 * Note the shallow equality check. EthType caches and reuses instances for valid types.
                 */
                if (eth.getEtherType() == EthType.IPv4) {
                    /* We got an IPv4 packet; get the payload from Ethernet */
                    IPv4 ipv4 = (IPv4) eth.getPayload();

                    /* Various getters and setters are exposed in IPv4 */
                    byte[] ipOptions = ipv4.getOptions();
                    IPv4Address dstIp = ipv4.getDestinationAddress();

                    /*
                     * Check the IP protocol version of the IPv4 packet's payload.
                     * Note the deep equality check. Unlike EthType, IpProtocol does
                     * not cache valid/common types; thus, all instances are unique.
                     */
                    if (ipv4.getProtocol().equals(IpProtocol.TCP)) {
                        /* We got a TCP packet; get the payload from IPv4 */
                        TCP tcp = (TCP) ipv4.getPayload();

                        /* Various getters and setters are exposed in TCP */
                        TransportPort srcPort = tcp.getSourcePort();
                        TransportPort dstPort = tcp.getDestinationPort();
                        short flags = tcp.getFlags();

                        /* Your logic here! */
//                        logger.info("TCP Packet: sourcePort {}, destPort {}", srcPort.getPort(), dstPort.getPort()); // Too verbose if this line exist

                        if (dstPort.getPort() == 13562) {
//                            logger.info("TCP Packet with srcPort {}, destPort {} detected.",
//                                    srcPort.getPort(), dstPort.getPort());

                            Data data = (Data) tcp.getPayload();
                            String payload = new String(data.getData());
//                            logger.info("TCP Payload: {}", payload);

                            // Check "mapOutput" as keyword, if not, DO NOT send POST to Python Controller.
                            if (payload.startsWith("GET /mapOutput?")) {
                                int end = payload.indexOf(" HTTP/1.1");
                                logger.info("Sending JSON: srcPort: {}, payload: {}",
                                        srcPort.getPort(),
                                        payload.substring(15, end));

                                // Desired output:
                                // Source port, job=job_1461107404635_0002, map=attempt_1461107404635_0002_m_000021_0

                                // TODO: Import SDNReport module and send json to PythonCon
                            }

                        }

                    } else if (ipv4.getProtocol().equals(IpProtocol.UDP)) {
                        /* We got a UDP packet; get the payload from IPv4 */
                        UDP udp = (UDP) ipv4.getPayload();

                        /* Various getters and setters are exposed in UDP */
                        TransportPort srcPort = udp.getSourcePort();
                        TransportPort dstPort = udp.getDestinationPort();

                        /* Your logic here! */
                    }

                } else if (eth.getEtherType() == EthType.ARP) {
                    /* We got an ARP packet; get the payload from Ethernet */
                    ARP arp = (ARP) eth.getPayload();

                    /* Various getters and setters are exposed in ARP */
                    boolean gratuitous = arp.isGratuitous();

//                    logger.info("ARP Packet. MAC Address: {} seen on switch: {}",
//                            eth.getSourceMACAddress().toString(),
//                            sw.getId().toString());

                } else {
//                    logger.info("Unhandled ethertype. MAC Address: {} seen on switch: {}",
//                            eth.getSourceMACAddress().toString(),
//                            sw.getId().toString());
                    /* Unhandled ethertype */
                }
                break;
            default:
                break;
        }

        return Command.CONTINUE;
    }

}
