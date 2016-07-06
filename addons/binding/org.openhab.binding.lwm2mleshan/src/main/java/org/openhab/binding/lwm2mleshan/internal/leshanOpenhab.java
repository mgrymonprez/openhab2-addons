package org.openhab.binding.lwm2mleshan.internal;

import java.io.IOException;
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
import java.util.Dictionary;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.request.exception.ResourceAccessException;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.util.Hex;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lwm2mleshan.handler.lwm2mLeshanHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class leshanOpenhab {
    private LeshanServer lwServer;
    private Logger logger = LoggerFactory.getLogger(lwm2mLeshanHandler.class);
    private final Gson gson;

    public leshanOpenhab() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private static byte[] getOrDefault(String in, String defaultV) {
        return Hex.decodeHex((in == null || in.length() == 0) ? defaultV.toCharArray() : in.toCharArray());
    }

    private static int getOrDefault(Integer in, int defaultV) {
        return in == null ? defaultV : in;
    }

    private static boolean getOrDefault(Boolean in, boolean defaultV) {
        return in == null ? defaultV : in;
    }

    public void createAndStartServer(Dictionary<String, Object> properties) throws Exception {
        String localAddress = null;
        int localPort = getOrDefault((Integer) properties.get("lwm2m_port"), LeshanServerBuilder.PORT);
        int secureLocalPort = getOrDefault((Integer) properties.get("lwm2m_port_secure"),
                LeshanServerBuilder.PORT_DTLS);
        boolean useECC = getOrDefault((Boolean) properties.get("lwm2m_secure_use_ecc"), false);
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

        lwServer = builder.build();
        if (!useECC) {
            SecurityInfo info1 = SecurityInfo.newPreSharedKeyInfo("lwm2mClient1", "identity1", privateKeyPart);
            lwServer.getSecurityRegistry().add(info1);
        }
        lwServer.getObservationRegistry().addListener(new ObservationRegistryListener() {

            @Override
            public void newValue(Observation observation, LwM2mNode value) {
                // TODO Auto-generated method stub

            }

            @Override
            public void newObservation(Observation observation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void cancelled(Observation observation) {
                // TODO Auto-generated method stub

            }
        });
        lwServer.start();
    }

    public void stopServer() {
        if (lwServer == null) {
            return;
        }
        lwServer.stop();
        lwServer.destroy();
        lwServer = null;
    }

    private static final long TIMEOUT = 5000; // ms

    public Observation startObserve(Client client, int objectID, int objectIDinstance) {
        try {
            ObserveRequest request = new ObserveRequest(objectID, objectIDinstance);
            ObserveResponse cResponse = lwServer.send(client, request, TIMEOUT);
            if (cResponse == null) {
                logger.warn(String.format("startObserve failed for %i/%i", objectID, objectIDinstance));
            } else {
                String response = this.gson.toJson(cResponse);
                if (cResponse.getCode().isError()) {
                    logger.warn("Response indicate error '%s'", cResponse.getErrorMessage());
                } else {
                    logger.debug("Response %s", response);
                }
                return cResponse.getObservation();
            }
        } catch (ResourceAccessException | RequestFailedException e) {
            // logger.warn(String.format("Error accessing resource %s%s.", String.join("/", path)), e);
        } catch (InterruptedException e) {
            logger.warn("Thread Interrupted", e);
        }
        return null;
    }

    public void stopObserve(Observation observation) {
        lwServer.getObservationRegistry().cancelObservation(observation);
    }

    // The coap url /endpoint/objectId/instanceId maps to /bridgeID/thingTypeID/thingID
    public void requestChange(Client client, int objectID, int objectIDinstance, Channel channel, Command command)
            throws IOException {
        DownlinkRequest<?> request = null;
        int resourceID = Lwm2mUID.getResourceID(channel.getUID());

        switch (channel.getAcceptedItemType()) {
            case "Number":
                if (channel.getDefaultTags().contains("Integer")) {
                    request = new WriteRequest(objectID, objectIDinstance, resourceID,
                            ((DecimalType) command).intValue());
                } else if (channel.getDefaultTags().contains("Float")) {
                    request = new WriteRequest(objectID, objectIDinstance, resourceID,
                            ((DecimalType) command).floatValue());
                } else {
                    logger.warn("Invalid number. Must be Float or Integer");
                    return;
                }

                break;
            case "Switch":
                if (channel.getDefaultTags().contains("Executable")) {
                    request = new ExecuteRequest(objectID, objectIDinstance, resourceID);
                } else {
                    request = new WriteRequest(objectID, objectIDinstance, resourceID,
                            ((OnOffType) command) == OnOffType.ON);
                }
                break;

            case "String":
                request = new WriteRequest(objectID, objectIDinstance, resourceID, ((StringType) command).toString());
                break;

            case "Dimmer":
                if (command instanceof DecimalType) {
                    DecimalType v = (DecimalType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v.intValue());
                } else if (command instanceof OnOffType) {
                    request = new WriteRequest(objectID, objectIDinstance, resourceID,
                            ((OnOffType) command) == OnOffType.ON ? 100 : 0);
                } else if (command instanceof PercentType) {
                    PercentType v = (PercentType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v.intValue());
                }
                break;
            case "Color":
                if (command instanceof HSBType) {
                    HSBType v = (HSBType) command;
                    String unit = channel.getProperties().get("unit");
                    String color;
                    switch (unit) {
                        case "RGB":
                            color = String.valueOf(v.getRed()) + "," + String.valueOf(v.getGreen()) + ","
                                    + String.valueOf(v.getRed());
                            break;
                        case "HSV":
                            color = String.valueOf(v.getRed()) + "," + String.valueOf(v.getGreen()) + ","
                                    + String.valueOf(v.getRed());
                            break;
                        default:
                            logger.warn(
                                    "Colorspace unknown. The lwm2m device must support RGB or HSV and propagate this via the \"unit\" resource.");
                            return;
                    }
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, color);

                } else if (command instanceof OnOffType) {
                    OnOffType v = (OnOffType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == OnOffType.ON);
                } else if (command instanceof PercentType) {
                    PercentType v = (PercentType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v.intValue());
                }
                break;
            case "Rollershutter":
                if (channel.getDefaultTags().contains("Executable")) {
                    request = new ExecuteRequest(objectID, objectIDinstance, resourceID);
                } else if (command instanceof OnOffType) {
                    OnOffType v = (OnOffType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == OnOffType.ON);

                } else if (command instanceof UpDownType) {
                    UpDownType v = (UpDownType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == UpDownType.UP);
                } else if (command instanceof OpenClosedType) {
                    OpenClosedType v = (OpenClosedType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == OpenClosedType.OPEN);
                } else if (command instanceof StopMoveType) {
                    StopMoveType v = (StopMoveType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == StopMoveType.MOVE);
                }

                break;
            case "Player":
                if (command instanceof PlayPauseType) {
                    PlayPauseType v = (PlayPauseType) command;
                    request = new WriteRequest(objectID, objectIDinstance, resourceID, v == PlayPauseType.PLAY);
                } else if (command instanceof NextPreviousType) {
                    NextPreviousType v = (NextPreviousType) command;

                } else if (command instanceof RewindFastforwardType) {
                    RewindFastforwardType v = (RewindFastforwardType) command;

                }
                break;
            default:
                break;
        }

        try {
            LwM2mResponse cResponse = lwServer.send(client, request, TIMEOUT);
            if (cResponse == null) {
                logger.warn(String.format("Request %s timed out.", channel.getUID().getAsString()));
            } else {
                String response = this.gson.toJson(cResponse);
                if (cResponse.getCode().isError()) {
                    logger.warn("Response indicate error '%s'", cResponse.getErrorMessage());
                } else {
                    logger.debug("Response %s", response);
                }
            }
        } catch (ResourceAccessException | RequestFailedException e) {
            // logger.warn(String.format("Error accessing resource %s%s.", String.join("/", path)), e);
        } catch (InterruptedException e) {
            logger.warn("Thread Interrupted", e);
        }
    }

    public ObjectModel getObjectModel(Client client, int objectID) {
        return lwServer.getModelProvider().getObjectModel(client).getObjectModel(objectID);
    }

    public Client getClient(String endpoint) {
        return lwServer.getClientRegistry().get(endpoint);
    }

}
