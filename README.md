# VelocityFailover

When one of your backend servers crashes or restarts, players on it are moved to a limbo server, and once the server is back they are moved back. Nobody gets the disconnect screen.

## How it works

1. A server goes down. Velocity kicks its players with a reason like "Server closed".
2. The plugin sees that kick, marks the server as down and sends the player to limbo instead.
3. While they wait, a small spinner shows in their action bar.
4. The plugin pings the downed server until it answers a few times in a row, then waits a moment so its plugins can load.
5. Players are moved back one at a time, so a freshly started server is not hit all at once.
6. Anyone trying to join the server while it is down gets a message instead.

Only downed servers are pinged. When everything is running, the plugin does nothing.

Normal kicks (bans, anticheat and so on) are left alone. Only reasons that match `shutdown-keywords` count as a crash.

## Requirements

- Velocity 4.x
- Java 25
- A limbo server registered in `velocity.toml`, for example [PicoLimbo](https://github.com/Quozul/PicoLimbo) or an empty Paper server

## Setup

1. Drop the jar into the proxy's `plugins/` folder and start the proxy once.
2. Open `plugins/velocityfailover/config.yml` and fill in your limbo server and the servers to watch.
3. Run `/failoverreload` (permission `velocityfailover.reload`) or restart the proxy.

## Config

```yaml
limbo-server: "limbo"

# Servers to watch. Groups are only for keeping the list tidy.
groups:
  lobby:
    servers: ["lobby1", "lobby2"]
  spawn:
    servers: ["spawn1", "spawn2"]

recovery:
  ping-interval-ms: 2000      # how often a downed server is pinged
  pings-to-ready: 3           # answers in a row before it counts as back
  grace-period-ms: 5000       # extra wait so its plugins finish loading
  transfer-interval-ms: 50    # pause between moving one player and the next
  ping-timeout-ms: 2000

# Kick reasons that mean the server went down, matched with "contains".
shutdown-keywords:
  - "Server closed"
  - "Server shutting down"

# MiniMessage. {spinner} in the action bar is replaced with the current frame.
messages:
  sent-to-limbo: "<red>The server is temporarily unavailable. You will be moved back automatically when it returns."
  reconnecting: "<green>The server is back online! Reconnecting..."
  connection-blocked: "<red>This server is currently unavailable. Please try again in a moment."
  waiting-action-bar: "<yellow>Connecting to the server <gray>{spinner}"

# Optional title/subtitle pairs shown together with the matching chat message.
titles:
  fade-in-ms: 300
  stay-ms: 2500
  fade-out-ms: 500
  sent-to-limbo:
    title: "<red><bold>Server unavailable</bold>"
    subtitle: "<gray>You will be moved back automatically"
  reconnecting:
    title: "<green><bold>Server is back online!</bold>"
    subtitle: "<gray>Reconnecting..."
  connection-blocked:
    title: "<red><bold>Server unavailable</bold>"
    subtitle: "<gray>Please try again in a moment"

action-bar:
  interval-ms: 400
  spinner-frames: ["[|]", "[/]", "[-]", "[\\]"]
```

Server names must match `velocity.toml` exactly. Do not list the limbo server itself.

Titles use MiniMessage too. Remove an event from `titles` (or leave both of its fields empty) to keep that event chat-only. Existing configs without a `titles` section continue to work unchanged.

## Good to know

- A player who leaves while waiting is forgotten. When they come back, your usual hub or fallback plugin takes over.
- A player who goes somewhere else on their own while waiting is forgotten too.
- If the server dies again mid-transfer, players already moved land back on limbo and the whole cycle starts over.
- This is not a hub plugin. It only handles crashes and restarts.

## License

MIT
