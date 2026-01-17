# Combat Plugin - Parry Stun Effect

A Hytale mod plugin that adds a stun effect to attackers when their attacks are successfully parried.

## Features

- **Parry Stun Mechanic**: When an entity successfully parries an attack (blocks with angular wielding), the attacker receives a 2-second "Stunned" effect.
- **Stun Duration Extension**: Multiple parries extend the stun duration rather than replacing it (uses `OverlapBehavior.EXTEND`).
- **Entity Support**: Works with both NPCs and Players.
- **Graceful Error Handling**: Safely handles missing effects or components.

## How It Works

1. The `WieldingDamageReduction` system (built into Hytale) detects angular blocking and marks damage as `BLOCKED`.
2. `DamageFilterParry` system detects the blocked status in the `InspectDamageGroup`.
3. The "Stunned" effect is applied to the attacker for 2 seconds.
4. The stunned effect should disable movement and interactions (configured in the "Stunned" EntityEffect asset).

## File Structure

```
combatplugin/
└── src/
    └── main/
        └── java/
            └── com/
                └── modding/
                    └── combatplugin/
                        ├── CombatPlugin.java      # Main plugin class
                        └── ParrySystems.java      # Parry detection and stun logic
```

## Classes

### CombatPlugin
Main plugin class that registers the combat systems with the Hytale server.

**Dependencies:**
- `EntityModule` - Core entity system
- `DamageModule` - Damage processing system

### ParrySystems
Contains the `DamageFilterParry` system that:
- Hooks into the `InspectDamageGroup` for damage processing
- Detects parries via `Damage.BLOCKED` metadata
- Applies the "Stunned" effect to attackers
- Uses `EffectControllerComponent` for effect application

## Requirements

### EntityEffect Asset
The plugin requires a "Stunned" `EntityEffect` asset to be configured in the game assets:

**Asset ID**: `Stunned`

**Configuration Requirements:**
- `MovementEffects`: Should disable movement
- `AbilityEffects`: Should disable interactions
- Duration: Can be configured in the asset, but will be overridden to 2.0 seconds by the plugin

If the "Stunned" effect doesn't exist in assets, the system fails gracefully without errors.

## Installation

1. Compile the plugin into a JAR file.
2. Place the JAR in your Hytale server's plugins directory.
3. Ensure the "Stunned" EntityEffect asset is configured in your game assets.
4. Restart the server.

## Configuration

### Stun Duration
The default stun duration is 2.0 seconds, defined in `ParrySystems.java`:

```java
private static final float STUN_DURATION = 2.0f;
```

To change the duration, modify this value and recompile the plugin.

### Effect ID
The default effect ID is "Stunned", defined in `ParrySystems.java`:

```java
private static final String STUNNED_EFFECT_ID = "Stunned";
```

To use a different effect, modify this value and ensure the corresponding EntityEffect asset exists.

## Technical Details

### Query Requirements
Entities must have the following components to be processed:
- Part of `AllLegacyLivingEntityTypesQuery` (NPCs or Players)
- `DamageDataComponent` - For damage data access
- `EffectControllerComponent` - For effect application

### System Group
The system runs in the `InspectDamageGroup`, which executes after damage calculation but before knockback application.

### Damage Detection
A parry is detected when:
1. Damage has `Damage.BLOCKED` metadata set to `true`
2. The damage source is an `EntitySource` (entity-to-entity damage)
3. The attacker reference is valid

## License

This is a community mod for Hytale. Please ensure you comply with Hypixel Studios' modding policies.
