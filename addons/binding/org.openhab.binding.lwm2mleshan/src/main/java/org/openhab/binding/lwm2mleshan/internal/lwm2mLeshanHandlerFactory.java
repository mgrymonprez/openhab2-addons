/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.internal;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.lwm2mleshan.handler.Lwm2mDeviceBridgeHandler;
import org.openhab.binding.lwm2mleshan.handler.Lwm2mObjectHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

/**
 * The {@link lwm2mLeshanHandlerFactory} is responsible for creating things and thing
 * handlers. It starts the leshan lwm2m server with the user given configuration.
 *
 * @author David Graeff - Initial contribution
 */
public class lwm2mLeshanHandlerFactory extends BaseThingHandlerFactory {
    LeshanOpenhab leshan = new LeshanOpenhab();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return thingTypeUID.getId().contains("thing-id") && thingTypeUID.getId().equals("lwm2mBridgeThing");
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        Client client = leshan.getClient(Lwm2mUID.getEndpoint(thing));
        if (client == null) {
            throw new IllegalArgumentException();
        }

        if (thing.getThingTypeUID().getId().equals("lwm2mBridgeThing")) {
            return new Lwm2mDeviceBridgeHandler((Bridge) thing, leshan, client);
        } else {
            return new Lwm2mObjectHandler(thing, leshan, client, Lwm2mUID.getObjectID(thing),
                    Lwm2mUID.getObjectIDInstance(thing));
        }
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        try {
            leshan.createAndStartServer(componentContext.getProperties());
            leshan.startDiscovery(bundleContext);
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        leshan.stopDiscovery();
        leshan.stopServer();
        super.deactivate(componentContext);
    }
}
