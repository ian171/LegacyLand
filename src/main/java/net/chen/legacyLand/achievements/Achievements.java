package net.chen.legacyLand.achievements;

import lombok.Getter;

@Getter
public enum Achievements {
    // 矿工类成就
    UNCOVERED_ORES("Uncovered Ores", "Mine at least 10 different ores"),
    STONE_AGE("Stone Age", "Mine 100 cobblestones"),
    COAL_MINER("Coal Miner", "Mine 50 coal ores"),
    IRON_COLLECTOR("Iron Collector", "Mine 30 iron ores"),
    GOLD_RUSH("Gold Rush", "Mine 20 gold ores"),
    DIAMOND_HUNTER("Diamond Hunter", "Mine 10 diamond ores"),
    ANCIENT_DEBRIS_FINDER("Ancient Debris Finder", "Find 5 ancient debris"),

    // 伐木工类成就
    RUBBISH_BAGGER("Rubbish Bagger", "Collect at least 10 pieces of branches"),
    LUMBERJACK("Lumberjack", "Chop 100 logs"),
    WOOD_COLLECTOR("Wood Collector", "Collect at least 5 different wood types"),
    MASTER_LUMBERJACK("Master Lumberjack", "Collect all 10 wood types"),

    // 农业类成就
    FANNY_FARMER("Fanny Farmer", "Harvest at least 10 different crops"),
    FIRST_HARVEST("First Harvest", "Harvest your first crop"),
    GREEN_THUMB("Green Thumb", "Plant 50 crops"),
    MASTER_FARMER("Master Farmer", "Harvest all 8 crop types"),

    // 战斗类成就
    MONSTER_SLAYER("Monster Slayer", "Kill 100 monsters"),
    BONE_COLLECTOR("Bone Collector", "Collect 50 bones"),
    GUNPOWDER_EXPERT("Gunpowder Expert", "Collect 30 gunpowder"),
    ENDER_HUNTER("Ender Hunter", "Collect 10 ender pearls"),
    BLAZE_SLAYER("Blaze Slayer", "Collect 10 blaze rods"),

    // 钓鱼类成就
    FIRST_CATCH("First Catch", "Catch your first fish"),
    FISHERMAN("Fisherman", "Catch 50 fish"),
    MASTER_ANGLER("Master Angler", "Catch a rare fish"),

    // 其他成就
    ENCHANTER("Enchanter", "Enchant 10 items"),
    POTION_BREWER("Potion Brewer", "Brew 10 potions"),
    ANIMAL_TAMER("Animal Tamer", "Tame 5 animals"),
    BLACKSMITH("Blacksmith", "Repair 10 items"),

    // 探索类成就
    EXPLORER("Explorer", "Visit 5 different biomes"),
    NETHER_TRAVELER("Nether Traveler", "Enter the Nether"),
    END_VOYAGER("End Voyager", "Enter the End"),

    // 建筑类成就
    ARCHITECT("Architect", "Place 1000 blocks"),
    MASTER_BUILDER("Master Builder", "Build a structure with 5000 blocks");

    private final String name;
    private final String description;

    Achievements(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
