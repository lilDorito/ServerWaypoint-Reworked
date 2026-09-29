package _959.server_waypoint.common.util;

import _959.server_waypoint.common.client.WaypointClientMod;
import _959.server_waypoint.core.waypoint.SimpleWaypoint;
import _959.server_waypoint.core.waypoint.WaypointList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static _959.server_waypoint.ModInfo.MOD_ID;
import static _959.server_waypoint.common.client.WaypointClientMod.LOGGER;
import static _959.server_waypoint.common.util.DimensionFileHelper.getDimensionKey;
import static _959.server_waypoint.common.util.XaeroMinimapHelper.getMinimapWorld;
import static _959.server_waypoint.common.util.XaeroMinimapHelper.saveMinimapWorld;
import static _959.server_waypoint.common.util.XaerosWaypointHelper.simpleWaypointToXaerosWaypoint;

/**
 * Shows the server's waypoints in Xaero's Minimap and World Map as server-provided (third-party) waypoints.
 * They appear in the default waypoint set for every player, are rebuilt from the server data on every sync
 * and are never written into the player's own waypoint sets.
 */
public final class XaerosServerWaypointsHelper {
    private static final String ORIGIN_PATH = "waypoints";
    private static java.lang.reflect.Field worldMapMinimapSupport;
    private static boolean worldMapUnavailable = false;

    private XaerosServerWaypointsHelper() {
    }

    @Nullable
    private static MinimapWorldRootContainer getRootContainer(@Nullable MinimapSession session) {
        if (session == null || session.getWorldManager() == null) {
            return null;
        }
        return session.getWorldManager().getAutoRootContainer();
    }

    @Nullable
    private static MinimapWorldContainer getDimensionContainer(MinimapSession session, ResourceKey<Level> dimKey) {
        MinimapWorldRootContainer rootContainer = getRootContainer(session);
        if (rootContainer == null) {
            return null;
        }
        String dimensionNode = session.getDimensionHelper().getDimensionDirectoryName(dimKey);
        return rootContainer.addSubContainer(rootContainer.getPath().resolve(dimensionNode));
    }

    public static void syncAll(@Nullable MinimapSession session) {
        MinimapWorldRootContainer rootContainer = getRootContainer(session);
        if (rootContainer == null) {
            return;
        }
        for (MinimapWorldContainer dimensionContainer : rootContainer.getSubContainers()) {
            dimensionContainer.getThirdPartyWaypointManager().clearOrigin(ResourceLocationHelper.id(MOD_ID, ORIGIN_PATH));
        }
        WaypointClientMod.getInstance().forEachWaypointFileManager(fileManager ->
                syncDimension(session, fileManager.getDimensionName(), fileManager.getWaypointLists()));
        refreshWorldMap();
    }

    public static void syncDimension(@Nullable MinimapSession session, String dimensionName) {
        syncDimension(session, dimensionName, WaypointClientMod.getInstance().getWaypointListsByDimensionName(dimensionName));
        refreshWorldMap();
    }

    /**
     * Xaero's World Map only re-reads minimap waypoints when its screen opens. Ask it to refresh now so a waypoint
     * added while the map is open shows up right away. The World Map is optional, so it is reached by reflection.
     */
    private static void refreshWorldMap() {
        if (worldMapUnavailable) {
            return;
        }
        try {
            if (worldMapMinimapSupport == null) {
                Class<?> supportMods = Class.forName("xaero.map.mods.SupportMods");
                worldMapMinimapSupport = supportMods.getField("xaeroMinimap");
            }
            Object support = worldMapMinimapSupport.get(null);
            if (support != null) {
                support.getClass().getMethod("requestWaypointsRefresh").invoke(support);
            }
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            worldMapUnavailable = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            worldMapUnavailable = true;
            LOGGER.warn("Failed to refresh Xaero's World Map waypoints", e);
        }
    }

    public static void syncDimension(@Nullable MinimapSession session, String dimensionName, List<WaypointList> waypointLists) {
        if (getRootContainer(session) == null) {
            return;
        }
        ResourceKey<Level> dimKey = getDimensionKey(dimensionName);
        if (dimKey == null) {
            LOGGER.warn("Failed to decode dimension {}", dimensionName);
            return;
        }
        MinimapWorldContainer dimensionContainer = getDimensionContainer(session, dimKey);
        if (dimensionContainer == null) {
            return;
        }
        ThirdPartyWaypoints serverWaypoints = dimensionContainer.getThirdPartyWaypointManager().get(ResourceLocationHelper.id(MOD_ID, ORIGIN_PATH));
        serverWaypoints.clear();
        for (WaypointList waypointList : waypointLists) {
            for (SimpleWaypoint simpleWaypoint : waypointList.simpleWaypoints()) {
                if (simpleWaypoint == null) {
                    continue;
                }
                serverWaypoints.add(waypointId(waypointList.name(), simpleWaypoint.name()), simpleWaypointToXaerosWaypoint(simpleWaypoint));
            }
        }
        removeLegacySets(session, dimKey, waypointLists);
    }

    private static String waypointId(String listName, String waypointName) {
        return listName + "/" + waypointName;
    }

    /**
     * Older versions copied every server list into a Xaero waypoint set with the same name. Such a set is removed
     * only when all of its waypoints still come from the server list, so waypoints a player added by hand are kept.
     */
    private static void removeLegacySets(MinimapSession session, ResourceKey<Level> dimKey, List<WaypointList> waypointLists) {
        MinimapWorld minimapWorld;
        try {
            minimapWorld = getMinimapWorld(session, dimKey);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to get Xaero's minimap world for {}", dimKey, e);
            return;
        }
        if (minimapWorld == null) {
            return;
        }
        boolean changed = false;
        for (WaypointList waypointList : waypointLists) {
            String setName = waypointList.name();
            if (MinimapWorld.DEFAULT_SET.equals(setName)) {
                continue;
            }
            WaypointSet legacySet = minimapWorld.getWaypointSet(setName);
            if (legacySet == null || !onlyServerWaypoints(legacySet, waypointList)) {
                continue;
            }
            if (setName.equals(minimapWorld.getCurrentWaypointSetId())) {
                minimapWorld.setCurrentWaypointSetId(MinimapWorld.DEFAULT_SET);
            }
            minimapWorld.removeWaypointSet(setName);
            changed = true;
            LOGGER.info("Removed old copied waypoint set {} from Xaero's minimap ({})", setName, dimKey);
        }
        if (changed) {
            try {
                saveMinimapWorld(session, minimapWorld);
            } catch (IOException e) {
                LOGGER.warn("Failed to save Xaero's minimap waypoints", e);
            }
        }
    }

    private static boolean onlyServerWaypoints(WaypointSet waypointSet, WaypointList waypointList) {
        Set<String> serverNames = new HashSet<>();
        for (SimpleWaypoint simpleWaypoint : waypointList.simpleWaypoints()) {
            if (simpleWaypoint != null) {
                serverNames.add(simpleWaypoint.name());
            }
        }
        for (Waypoint waypoint : waypointSet.getWaypoints()) {
            if (!serverNames.contains(waypoint.getName())) {
                return false;
            }
        }
        return true;
    }
}
