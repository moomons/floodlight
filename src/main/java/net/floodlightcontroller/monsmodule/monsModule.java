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
import net.floodlightcontroller.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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

        Ethernet eth =
                IFloodlightProviderService.bcStore.get(cntx,
                        IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        Long sourceMACHash = eth.getSourceMACAddress().getLong();
        if (!macAddresses.contains(sourceMACHash)) {
            macAddresses.add(sourceMACHash);
            logger.info("MAC Address: {} seen on switch: {}",
                    eth.getSourceMACAddress().toString(),
                    sw.getId().toString());
        }
        return Command.CONTINUE;
    }

}
