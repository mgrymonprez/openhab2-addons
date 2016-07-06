/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.handler;

import java.io.IOException;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lwm2mleshan.internal.leshanOpenhab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link lwm2mLeshanHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * Channels will have some properties:
 * - "unit": for the lwm2m color type this indicates the color space ("RGB", "HSB")
 *
 * @author David Graeff - Initial contribution
 */
public class lwm2mLeshanHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(lwm2mLeshanHandler.class);
    private final leshanOpenhab leshan;
    private final Client client;
    private final int objectID;
    private final int objectIDinstance;
    private ObjectModel objectModel;
    private Observation observe;

    public lwm2mLeshanHandler(Thing thing, leshanOpenhab leshan, Client client, int objectID, int objectIDinstance) {
        super(thing);
        this.leshan = leshan;
        this.client = client;
        this.objectID = objectID;
        this.objectIDinstance = objectIDinstance;
        objectModel = leshan.getObjectModel(client, objectID);
        // .resources.get(0);
    }

    // Avoid dispose+initialize because of a configuration change on the thing
    @Override
    public void thingUpdated(Thing thing) {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            leshan.requestChange(client, objectID, objectIDinstance, thing.getChannel(channelUID.getId()), command);
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
        observe = leshan.startObserve(client, objectID, objectIDinstance);
    }

    @Override
    public void dispose() {
        if (observe != null) {
            leshan.stopObserve(observe);
        }
        super.dispose();
    }
}
