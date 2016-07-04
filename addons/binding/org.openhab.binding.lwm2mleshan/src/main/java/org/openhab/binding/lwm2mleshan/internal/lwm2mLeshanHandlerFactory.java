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
import java.util.Dictionary;
import java.util.Set;

import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.security.SecurityInfo;
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
 * handlers. It starts the leshan lwm2m server with the user given configuration.
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

    private static byte[] getOrDefault(String in, String defaultV) {
        return Hex.decodeHex((in == null || in.length() == 0) ? defaultV.toCharArray() : in.toCharArray());
    }

    private static int getOrDefault(Integer in, int defaultV) {
        return in == null ? defaultV : in;
    }

    public void createAndStartServer(ComponentContext componentContext) throws Exception {
        Dictionary<String, Object> properties = componentContext.getProperties();

        String localAddress = null;
        int localPort = getOrDefault((Integer) properties.get("lwm2m_port"), LeshanServerBuilder.PORT);
        int secureLocalPort = getOrDefault((Integer) properties.get("lwm2m_port_secure"),
                LeshanServerBuilder.PORT_DTLS);
        boolean useECC = (Boolean) properties.get("lwm2m_secure_use_ecc");
        String temp;
        temp = (String) properties.get("lwm2m_secure_public_key");
        byte[] privateKeyPart = getOrDefault(temp, "1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400");
        temp = (String) properties.get("lwm2m_secure_point_x");
        byte[] publicX = getOrDefault(temp, "fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73");
        temp = (String) properties.get("lwm2m_secure_point_y");
        byte[] publicY = getOrDefault(temp, "d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a");

        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(localAddress, secureLocalPort);

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        if (useECC) {
            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateKeyPart), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            builder.setSecurityRegistry(new SecurityRegistryImpl(privateKey, publicKey));
        }

        LwM2mModelProvider modelProvider = new StandardModelProvider();
        builder.setObjectModelProvider(modelProvider);

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();
        if (!useECC) {
            SecurityInfo info1 = SecurityInfo.newPreSharedKeyInfo("lwm2mClient1", "identity1", privateKeyPart);
            lwServer.getSecurityRegistry().add(info1);
        }
        lwServer.start();
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        try {
            createAndStartServer(componentContext);
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }
}
