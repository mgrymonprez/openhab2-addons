/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.handler;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingFactory;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.TypeResolver;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lwm2mleshan.internal.LeshanOpenhab;
import org.openhab.binding.lwm2mleshan.internal.Lwm2mUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Lwm2mDeviceBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * Channels will have some properties:
 * - "unit": for the lwm2m color type this indicates the color space ("RGB", "HSB")
 *
 * @author David Graeff - Initial contribution
 */
public class Lwm2mDeviceBridgeHandler extends BaseBridgeHandler {
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(Lwm2mDeviceBridgeHandler.class);
    private final LeshanOpenhab leshan;
    public final Client client;

    public Lwm2mDeviceBridgeHandler(Bridge bridge, LeshanOpenhab leshan, Client client) {
        super(bridge);
        this.leshan = leshan;
        this.client = client;
    }

    // Avoid dispose+initialize because of a configuration change on the bridge
    @Override
    public void thingUpdated(Thing thing) {
        this.thing = thing;
    }

    // Avoid dispose+initialize because of a configuration change on the bridge
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        validateConfigurationParameters(configurationParameters);

        Configuration configuration = editConfiguration();
        for (Entry<String, Object> configurationParmeter : configurationParameters.entrySet()) {
            configuration.put(configurationParmeter.getKey(), configurationParmeter.getValue());
        }

        updateConfiguration(configuration);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        // TODO Get data
        // TODO add things

        int thingID = 3200;
        ThingTypeUID thingTypeUID = Lwm2mUID.getThingTypeUID(thingID);
        ThingUID thingUID = new ThingUID(thingTypeUID, "0");
        Thing newThing = thingRegistry.get(thingUID);
        if (newThing == null) {
            ThingType thingType = TypeResolver.resolve(thingTypeUID);
            newThing = ThingFactory.createThing(thingType, thingUID, new Configuration());
            thingRegistry.add(newThing);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
}
