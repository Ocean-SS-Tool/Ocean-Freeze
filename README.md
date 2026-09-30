# Ocean - Screenshare Plugin

Spigot plugin for Minecraft 1.8+ that integrates with the [Ocean Anticheat API](https://anticheat.ac) to automate the screenshare process.

Video Demo: [Demo](https://youtu.be/o9VYs2kKARI)

## Features

- Freeze system with movement, command, and interaction blocking
- Ocean API integration with automatic PIN generation and real-time scan monitoring
- AUTO / MANUAL freeze modes
- Freeze Response GUI (Admit / I'm Legit)
- Discord webhook notifications (freeze, admit, scan results)
- User lookup and risk score commands
- Reconnect handling and auto-ban on disconnect
- Clickable chat messages for staff actions
- Fully customizable messages (`messages.yml`)

## Installation

1. Place `Ocean.jar` in your `plugins/` folder
2. Start the server to generate config files
3. Set your API key in `plugins/Ocean/config.yml`
4. `/ocean reload`

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ss <player>` | Freeze/unfreeze a player | `ocean.freeze` |
| `/ocean scan <player>` | Start screenshare scan | `ocean.staff` |
| `/ocean lookup <discordId>` | View scan history | `ocean.staff` |
| `/ocean riskscore <discordId>` | Check risk analysis | `ocean.staff` |
| `/ocean config` | Configuration GUI | `ocean.admin` |
| `/ocean mode [AUTO\|MANUAL]` | Change freeze mode | `ocean.admin` |
| `/ocean reload` | Reload configuration | `ocean.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `ocean.freeze` | Freeze players | op |
| `ocean.freeze.bypass` | Cannot be frozen | false |
| `ocean.staff` | Staff commands (scan, lookup, riskscore) and notifications | op |
| `ocean.admin` | Admin commands (config, mode, reload) | op |

## Configuration

- `config.yml` - Plugin settings, API config, Discord webhook
- `messages.yml` - All customizable messages

## Building

```bash
mvn clean package
```
