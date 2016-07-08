package org.openhab.binding.lwm2mleshan.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.lwm2mleshan.lwm2mLeshanBindingConstants;

// Thing ID pattern: bindingID/thingTypeId/bridge0..n/thingID/channelID
// For lwm2m with a pattern of /endpoint/objectid/objectinstanceid/resourceid/resourceinstanceid
// * the endpoint is the last bridgeID,
// * the objectid is the thingTypeId (without the bindingId),
// * the objectinstanceid is the thingID,
// * the resourceid is the channelID if no channelGroupId otherwise the channelGroupId,
// * the resourceinstanceid is default 0. If there is a channelGroupID, the the channelID is the instanceID.
public class Lwm2mUID extends ChannelUID {
    private final String endpoint;
    private int objectID;
    private int objectInstance;
    private int resourceID;
    private int resourceIDinstance = 0;

    public static String getEndpoint(Thing thing) {
        return thing instanceof Bridge ? thing.getUID().getId() : thing.getBridgeUID().getId();
    }

    public static int getObjectID(Thing thing) {
        return Integer.valueOf(thing.getThingTypeUID().getId());
    }

    public static int getObjectIDInstance(Thing thing) {
        return Integer.valueOf(thing.getUID().getId());
    }

    public static int getResourceID(ChannelUID channelUid) {
        if (channelUid.isInGroup()) {
            return Integer.valueOf(channelUid.getGroupId());
        } else {
            return Integer.valueOf(channelUid.getId());
        }
    }

    public static int getResourceIDinstance(ChannelUID channelUid) {
        if (channelUid.isInGroup()) {
            return Integer.valueOf(channelUid.getIdWithoutGroup());
        } else {
            return 0;
        }
    }

    public Lwm2mUID(ChannelUID channelUid) {
        super(channelUid.getAsString());

        String[] segments = getSegments();
        objectID = Integer.valueOf(segments[1]);
        String thingID = segments[segments.length - 2];
        objectInstance = Integer.valueOf(thingID);

        if (isInGroup()) {
            resourceID = Integer.valueOf(getGroupId());
            resourceIDinstance = Integer.valueOf(getIdWithoutGroup());
        } else {
            resourceID = Integer.valueOf(getId());
            resourceIDinstance = 0;
        }

        List<String> bridgeIds = new ArrayList<>();
        // skip bindingID/thingTypeId and also don't care about thingID/channelID
        for (int i = 2; i < segments.length - 2; i++) {
            bridgeIds.add(segments[i]);
        }
        endpoint = bridgeIds.get(bridgeIds.size() - 1);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getObjectID() {
        return objectID;
    }

    public int getObjectInstance() {
        return objectInstance;
    }

    public int getResourceID() {
        return resourceID;
    }

    public int getResourceIDinstance() {
        return resourceIDinstance;
    }

    public static String getChannelID(LwM2mResource value, int resourceInstance) {
        if (value.isMultiInstances() && resourceInstance >= 0) {
            return String.valueOf(resourceInstance) + "#" + String.valueOf(value.getId());
        }
        return String.valueOf(value.getId());
    }

    public static ThingTypeUID getThingTypeUID(int objectID) {
        return new ThingTypeUID(lwm2mLeshanBindingConstants.BINDING_ID, String.valueOf(objectID));
    }

}
