package org.openhab.binding.lwm2mleshan.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;

// Thing ID pattern: bindingID/thingTypeId/bridge0..n/thingID/channelID
// For lwm2m with a pattern of /endpoint/objectid/objectinstanceid/resourceid/resourceinstanceid
// * the endpoint is the last bridgeID,
// * the objectid is encoded in the thingTypeId (the number after the "-"),
// * the objectinstanceid is the thingID,
// * the resourceid is encoded in the channelID (the number after the "-"),
// * the resourceinstanceid is always 0. This may change in the future.
public class Lwm2mUID extends ChannelUID {
    private final String endpoint;
    private int objectID;
    private int objectInstance;
    private int resourceID;

    public static String getEndpoint(Thing thing) {
        return thing.getBridgeUID().getId();
    }

    public static int getObjectID(Thing thing) {
        String segment = thing.getThingTypeUID().getId();
        return Integer.valueOf(segment.substring(segment.lastIndexOf("-id") + 3));
    }

    public static int getObjectIDInstance(Thing thing) {
        return Integer.valueOf(thing.getUID().getId());
    }

    public static int getResourceID(ChannelUID channelUid) {
        String chanID = channelUid.getId();
        return Integer.valueOf(chanID.substring(chanID.lastIndexOf("-id") + 3));
    }

    public Lwm2mUID(ChannelUID channelUid) {
        super(channelUid.getAsString());

        String[] segments = getSegments();
        objectID = Integer.valueOf(segments[1].substring(segments[1].lastIndexOf("-id") + 3));
        String thingID = segments[segments.length - 2];
        objectInstance = Integer.valueOf(thingID);
        String chanID = segments[segments.length - 1];
        resourceID = Integer.valueOf(chanID.substring(chanID.lastIndexOf("-id") + 3));

        List<String> bridgeIds = new ArrayList<>();
        // skip bindingID/thingTypeId and also don't care about thingID/channelID
        for (int i = 2; i < segments.length - 2; i++) {
            bridgeIds.add(segments[i]);
        }
        endpoint = segments[segments.length - 1];
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

    public static String getChannelID(int id) {
        return "channel-id" + String.valueOf(id);
    }

}
