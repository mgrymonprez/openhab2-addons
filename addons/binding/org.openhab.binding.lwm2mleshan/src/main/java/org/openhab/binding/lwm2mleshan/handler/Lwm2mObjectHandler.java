/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.handler;

import java.util.Map;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lwm2mleshan.internal.Lwm2mUID;
import org.openhab.binding.lwm2mleshan.internal.LeshanOpenhab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Lwm2mObjectHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * Channels will have some properties:
 * - "unit": for the lwm2m color type this indicates the color space ("RGB", "HSB")
 *
 * @author David Graeff - Initial contribution
 */
public class Lwm2mObjectHandler extends BaseThingHandler {

    private static final int UNITS_RESOURCE = 5701;
    private Logger logger = LoggerFactory.getLogger(Lwm2mObjectHandler.class);
    private final LeshanOpenhab leshan;
    public final Client client;
    public final int objectID;
    public final int objectIDinstance;
    public final ObjectModel objectModel;
    public String unit;
    private Observation observe;
    private LwM2mObjectInstance objectNode;

    public Lwm2mObjectHandler(Thing thing, LeshanOpenhab leshan, Client client, int objectID, int objectIDinstance) {
        super(thing);
        this.leshan = leshan;
        this.client = client;
        this.objectID = objectID;
        this.objectIDinstance = objectIDinstance;
        objectModel = leshan.getObjectModel(client, objectID);
    }

    // Avoid dispose+initialize because of a configuration change on the thing
    @Override
    public void thingUpdated(Thing thing) {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int resID = Integer.valueOf(channelUID.getId());
        try {
            leshan.requestChange(this, objectModel.resources.get(resID), command);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        try {
            observe = leshan.startObserve(this);
            objectNode = leshan.requestValues(this);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
            return;
        }
        updateAll(objectNode);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        if (observe != null) {
            leshan.stopObserve(observe);
        }
        super.dispose();
    }

    public void updateLwM2mNode(LwM2mNode value) {
        if (value.equals(objectNode)) {
            updateAll((LwM2mObjectInstance) value);
        } else if (value instanceof LwM2mResource) {
            LwM2mResource resource = (LwM2mResource) value;
            if (value.equals(objectNode.getResource(resource.getId()))) {
                updateSingleChannel(resource);
            }
        }
    }

    private void updateSingleChannel(LwM2mResource value) {
        State newState;
        String channelID = Lwm2mUID.getChannelID(value.getId());
        Channel channel = thing.getChannel(channelID);

        if (channel == null) {
            logger.warn("No channel with id %s for thing %s found", channelID, thing.getUID().getAsString());
            return;
        }
        switch (channel.getAcceptedItemType()) {
            case "DateTime":
                if (value.getType() != ResourceModel.Type.STRING) {
                    logger.warn("String expected!");
                    return;
                }
                try {
                    newState = new DateTimeType((String) value.getValue());
                } catch (IllegalArgumentException e) {
                    logger.warn("DateTime format unknown: " + (String) value.getValue());
                    return;
                }
                break;
            case "Switch":
                switch (value.getType()) {
                    case BOOLEAN:
                        newState = ((Boolean) value.getValue() == true) ? OnOffType.ON : OnOffType.OFF;
                        break;
                    case INTEGER:
                        newState = ((Integer) value.getValue() > 0) ? OnOffType.ON : OnOffType.OFF;
                        break;
                    default:
                        logger.warn("Number or Boolean expected!");
                        return;
                }
                break;
            case "Number":
                switch (value.getType()) {
                    case FLOAT:
                        newState = new DecimalType((Float) value.getValue());
                        break;
                    case INTEGER:
                        newState = new DecimalType((Integer) value.getValue());
                        break;
                    default:
                        logger.warn("Number expected!");
                        return;
                }
                break;
            case "Rollershutter":
                logger.warn("Rollershutter not supported yet!");
                return;
            case "String":
                if (value.getType() != ResourceModel.Type.STRING) {
                    logger.warn("String expected!");
                    return;
                }
                newState = new StringType((String) value.getValue());
                break;
            case "Color":
                switch (unit) {
                    case "RGB":
                        String[] rgb = ((String) value.getValue()).split(",");
                        if (rgb.length != 3) {
                            throw new IllegalArgumentException(
                                    String.format("RGB format expected: r,g,b. Value: %s", (String) value.getValue()));
                        }
                        newState = HSBType.fromRGB(Integer.valueOf(rgb[0]), Integer.valueOf(rgb[1]),
                                Integer.valueOf(rgb[2]));
                        break;
                    case "HSB":
                        newState = new HSBType((String) value.getValue());
                        break;
                    default:
                        logger.warn("Unsupported color space unit: %s", unit);
                        return;
                }
                break;
            default:
                logger.warn("ItemType not supported: %s", channel.getAcceptedItemType());
                return;
        }
        updateState(channelID, newState);
    }

    private void updateAll(LwM2mObjectInstance value) {
        Map<Integer, LwM2mResource> resources = value.getResources();
        LwM2mResource lwM2mResource = resources.get(UNITS_RESOURCE);
        if (lwM2mResource != null) {
            unit = (String) lwM2mResource.getValue();
        }

        for (LwM2mResource res : resources.values()) {
            if (res.getId() != UNITS_RESOURCE) {
                updateSingleChannel(res);
            }
        }

    }
}
