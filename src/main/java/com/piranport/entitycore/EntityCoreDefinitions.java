package com.piranport.entitycore;

import com.piranport.registry.ModEntityTypes;

import java.util.List;
import java.util.Optional;

public final class EntityCoreDefinitions {
    public static final int DEEP_OCEAN_SUPPLY = 1;
    public static final int DEEP_OCEAN_ARCHIVIST = 2;
    public static final int DEEP_OCEAN_ENGINEER = 3;
    public static final int DEEP_OCEAN_NAVIGATOR = 4;
    public static final int DEEP_OCEAN_QUARTERMASTER = 5;
    public static final int DEEP_OCEAN_DESTROYER = 6;
    public static final int DEEP_OCEAN_LIGHT_CRUISER = 7;
    public static final int DEEP_OCEAN_HEAVY_CRUISER = 8;
    public static final int DEEP_OCEAN_BATTLE_CRUISER = 9;
    public static final int DEEP_OCEAN_BATTLESHIP = 10;
    public static final int DEEP_OCEAN_LIGHT_CARRIER = 11;
    public static final int DEEP_OCEAN_CARRIER = 12;
    public static final int DEEP_OCEAN_SUBMARINE = 13;
    public static final int DEEP_OCEAN_FLAGSHIP = 14;
    public static final int SHIP_GIRL = 15;

    private static final List<EntityCoreDefinition> ALL = List.of(
            new EntityCoreDefinition(DEEP_OCEAN_SUPPLY, "deep_ocean_supply", ModEntityTypes.DEEP_OCEAN_SUPPLY),
            new EntityCoreDefinition(DEEP_OCEAN_ARCHIVIST, "deep_ocean_archivist", ModEntityTypes.DEEP_OCEAN_ARCHIVIST),
            new EntityCoreDefinition(DEEP_OCEAN_ENGINEER, "deep_ocean_engineer", ModEntityTypes.DEEP_OCEAN_ENGINEER),
            new EntityCoreDefinition(DEEP_OCEAN_NAVIGATOR, "deep_ocean_navigator", ModEntityTypes.DEEP_OCEAN_NAVIGATOR),
            new EntityCoreDefinition(DEEP_OCEAN_QUARTERMASTER, "deep_ocean_quartermaster", ModEntityTypes.DEEP_OCEAN_QUARTERMASTER),
            new EntityCoreDefinition(DEEP_OCEAN_DESTROYER, "deep_ocean_destroyer", ModEntityTypes.DEEP_OCEAN_DESTROYER),
            new EntityCoreDefinition(DEEP_OCEAN_LIGHT_CRUISER, "deep_ocean_light_cruiser", ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER),
            new EntityCoreDefinition(DEEP_OCEAN_HEAVY_CRUISER, "deep_ocean_heavy_cruiser", ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER),
            new EntityCoreDefinition(DEEP_OCEAN_BATTLE_CRUISER, "deep_ocean_battle_cruiser", ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER),
            new EntityCoreDefinition(DEEP_OCEAN_BATTLESHIP, "deep_ocean_battleship", ModEntityTypes.DEEP_OCEAN_BATTLESHIP),
            new EntityCoreDefinition(DEEP_OCEAN_LIGHT_CARRIER, "deep_ocean_light_carrier", ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER),
            new EntityCoreDefinition(DEEP_OCEAN_CARRIER, "deep_ocean_carrier", ModEntityTypes.DEEP_OCEAN_CARRIER),
            new EntityCoreDefinition(DEEP_OCEAN_SUBMARINE, "deep_ocean_submarine", ModEntityTypes.DEEP_OCEAN_SUBMARINE),
            new EntityCoreDefinition(DEEP_OCEAN_FLAGSHIP, "deep_ocean_flagship", ModEntityTypes.DEEP_OCEAN_FLAGSHIP),
            new EntityCoreDefinition(SHIP_GIRL, "ship_girl", ModEntityTypes.SHIP_GIRL)
    );

    private EntityCoreDefinitions() {}

    public static List<EntityCoreDefinition> all() {
        return ALL;
    }

    public static Optional<EntityCoreDefinition> get(int coreId) {
        for (EntityCoreDefinition definition : ALL) {
            if (definition.id() == coreId) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    public static boolean isValid(int coreId) {
        return get(coreId).isPresent();
    }
}
