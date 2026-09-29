package _959.server_waypoint.config;

public class Features {
    public static boolean noXaerosMod = true;
    boolean addWaypointFromChatSharing = true;
    boolean sendXaerosWorldId = true;
    boolean waypointsInWorldFolder = true;

    public Features() {
    }

    public boolean addWaypointFromChatSharing() {
        return this.addWaypointFromChatSharing;
    }

    public boolean sendXaerosWorldId() {
        return noXaerosMod && this.sendXaerosWorldId;
    }

    /**
     * dedicated server only: store waypoints in &lt;world&gt;/server_waypoint/waypoints instead of the config folder,
     * so every world (map) has its own waypoints
     */
    public boolean waypointsInWorldFolder() {
        return this.waypointsInWorldFolder;
    }

    public void sendXaerosWorldId(boolean enable) {
        this.sendXaerosWorldId = enable;
    }

    @Override
    public String toString() {
        return "{addWaypointFromChatSharing=" + addWaypointFromChatSharing  + ", sendXaerosWorldId=" + sendXaerosWorldId + ", waypointsInWorldFolder=" + waypointsInWorldFolder + "}";
    }
}
