# Changelog (Server Waypoint Reworked)

## 3.0.4-reworked
Unofficial fork of Server Waypoint 3.0.4 by 2676959 for Forge 1.20.1.

### Fixed
- **Waypoints stopped syncing after a player died or respawned** (for example after `kill @a`).
  On Minecraft 1.20.1, respawning replaces the server-side player object, and the player's language
  was not copied to the new one. Every waypoint chat message sent to that player then crashed with a
  `NullPointerException`. The broadcast loop stopped at that player, so no one received the update,
  and a function such as `set_red_spawn` stopped after `wp remove`, so `wp add` never ran.
  - The player's language is now kept across respawns (`ServerPlayer.restoreFrom`).
  - A missing language falls back to the server's default instead of crashing.
  - A failure for one player no longer stops the waypoint update from reaching the others; the
    update packet is also sent before the chat message.

### Changed
- **Server waypoints show in Xaero's Minimap and World Map for every player, right away.** They are
  added as Xaero *server waypoints* (Xaero's third-party waypoint API, the same one it uses for
  Waystones) instead of being copied into a separate waypoint set named after the list.
  - Every list (for example `Spawns`) appears in the main (default) waypoint tab, so players don't
    have to switch sets or turn on "render all sets".
  - Players who are online see a new waypoint as soon as it is added, and players who join later
    get all of them when they connect. The World Map refreshes immediately, even if it is open.
  - They are rebuilt from the server on every sync and never written into the player's own sets,
    so waypoints a player adds from a shared message are no longer wiped by the next sync.
  - Players can still hide a server waypoint in Xaero's waypoint menu; the choice is remembered.
  - Old copied sets are cleaned up automatically: a set with the same name as a server list is
    removed only if it contains nothing but that list's waypoints. If it was the selected set, the
    player is switched back to the default set.
  - New client option `xaerosServerWaypoints` in `config/server_waypoint/client-config.json`
    (default `true`). Set it to `false` for the old behaviour, which is also used automatically
    with Xaero's Minimap versions that don't have the server waypoint API.
  - Built against Xaero's Minimap `forge-1.20.1-26.5.0`.
- **Waypoints are stored per world on dedicated servers.** They are saved in
  `<world>/server_waypoint/waypoints/` instead of the shared
  `defaultconfigs/server_waypoint/waypoints/` folder, so each world (map) has its own waypoints.
  Singleplayer already worked this way.
  - One-time migration: the first world started with this version gets a copy of the old shared
    waypoints, and the old folder is renamed to `waypoints_moved_to_world` so other worlds start
    empty.
  - Each world gets its own id (`<world>/server_waypoint/server_id.txt`), sent to clients, so a
    client never shows the previous world's waypoints from its cache.
  - New server option `Features.waypointsInWorldFolder` in
    `defaultconfigs/server_waypoint/config.json` (default `true`). Set it to `false` for one shared
    set of waypoints for every world. Takes effect after a server restart.

No network protocol change: original 3.0.4 clients can still join, but every player needs this
version for the Xaero changes.
