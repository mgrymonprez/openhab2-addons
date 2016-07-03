/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lwm2mleshan.internal;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.Collections;
import java.util.Set;

import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.util.Hex;
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
 * handlers.
 *
 * @author David Graeff - Initial contribution
 */
public class lwm2mLeshanHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(lwm2mLeshanBindingConstants.THING_TYPE_SAMPLE);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(lwm2mLeshanBindingConstants.THING_TYPE_SAMPLE)) {
            return new lwm2mLeshanHandler(thing);
        }

        return null;
    }

    public static void createAndStartServer(String localAddress, int localPort, String secureLocalAddress,
            int secureLocalPort) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        // Get point values
        byte[] publicX = Hex
                .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
        byte[] publicY = Hex
                .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
        byte[] privateS = Hex
                .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

        // Get Elliptic Curve Parameter spec for secp256r1
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

        // Create key specs
        KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                parameterSpec);
        KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

        // Get keys
        publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
        privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

        LwM2mModelProvider modelProvider = new StandardModelProvider();
        builder.setObjectModelProvider(modelProvider);

        // in memory security registry (with file persistence) (alternativly use redis)
        builder.setSecurityRegistry(new SecurityRegistryImpl(privateKey, publicKey));

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();
        lwServer.start();
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        try {
            createAndStartServer(null, LeshanServerBuilder.PORT, null, LeshanServerBuilder.PORT_DTLS);
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }
}
