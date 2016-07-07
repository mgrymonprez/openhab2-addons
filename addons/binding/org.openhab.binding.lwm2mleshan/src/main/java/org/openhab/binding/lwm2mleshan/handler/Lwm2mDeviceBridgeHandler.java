/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.handler;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingFactory;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lwm2mleshan.internal.LeshanOpenhab;
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
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        // TODO Get data
        // TODO add things
        thingRegistry.add(ThingFactory.createThing(thingType, thingUID, configuration));
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        leshan.stopObserve(client);
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
}
