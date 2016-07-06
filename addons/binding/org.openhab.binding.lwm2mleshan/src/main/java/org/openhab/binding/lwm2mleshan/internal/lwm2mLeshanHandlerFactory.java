/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.lwm2mleshan.lwm2mLeshanBindingConstants;
import org.openhab.binding.lwm2mleshan.handler.lwm2mLeshanHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

/**
 * The {@link lwm2mLeshanHandlerFactory} is responsible for creating things and thing
 * handlers. It starts the leshan lwm2m server with the user given configuration.
 *
 * @author David Graeff - Initial contribution
 */
public class lwm2mLeshanHandlerFactory extends BaseThingHandlerFactory {
    leshanOpenhab leshan = new leshanOpenhab();

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(lwm2mLeshanBindingConstants.THING_TYPE_SAMPLE);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        // ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        Client client = leshan.getClient(Lwm2mUID.getEndpoint(thing));
        return new lwm2mLeshanHandler(thing, leshan, client, Lwm2mUID.getObjectID(thing),
                Lwm2mUID.getObjectIDInstance(thing));
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        try {
            leshan.createAndStartServer(componentContext.getProperties());
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        leshan.stopServer();
        super.deactivate(componentContext);
    }
}
