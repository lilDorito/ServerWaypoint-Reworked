package _959.server_waypoint.common.util;

public final class XaerosApiCheck {
    private static final String THIRD_PARTY_WAYPOINTS_CLASS = "xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager";
    private static Boolean thirdPartyWaypoints;

    private XaerosApiCheck() {
    }

    public static boolean hasThirdPartyWaypoints() {
        if (thirdPartyWaypoints == null) {
            try {
                Class.forName(THIRD_PARTY_WAYPOINTS_CLASS, false, XaerosApiCheck.class.getClassLoader());
                thirdPartyWaypoints = true;
            } catch (Throwable e) {
                thirdPartyWaypoints = false;
            }
        }
        return thirdPartyWaypoints;
    }
}
