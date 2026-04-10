## Plugin Usage

### Crafting the Elevator Block
```
W W W
W E W    →  Elevator Block (x1)
W W W

W = White Wool
E = Ender Pearl
```

### Using Elevators
- Place **Elevator Blocks** directly above each other (on the same X, Z coordinates)
- Stand **on top** of an elevator block
- Press **Space (Jump)** → go to the floor ABOVE
- Press **Shift (Sneak)** → go to the floor BELOW

### Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/elevator reload` | Reload config | `elevator.admin` |
| `/elevator give [player] [amount]` | Give elevator blocks | `elevator.admin` |

### Permissions
| Permission | Default | Description |
|-----------|---------|-------------|
| `elevator.admin` | OP | Access to /elevator commands |
| `elevator.use` | Everyone | Use elevator blocks |
| `elevator.craft` | Everyone | Craft elevator blocks |

## Config File (config.yml)
After first run, edit `plugins/ElevatorPlugin/config.yml`:

- **elevator-item.material** — Change the block type used as an elevator
- **elevator-item.display-name** — Change the item name
- **crafting.enabled** — Enable/disable crafting recipe
- **crafting.ingredients** — Change what materials are used in the recipe
- **elevator.max-floor-distance** — How far up/down to scan for floors
- **particles.enabled** — Toggle particles on elevator use
- **messages.*** — Customize all actionbar/title messages
- **blocked-worlds** — List worlds where elevators are disabled

## Example blocked-worlds config
```yaml
blocked-worlds:
  - world_nether
  - creative_world
```
