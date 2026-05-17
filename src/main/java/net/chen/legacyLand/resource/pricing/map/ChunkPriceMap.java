package net.chen.legacyLand.resource.pricing.map;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.resource.pricing.ChunkResourceData;
import net.chen.legacyLand.resource.pricing.ChunkResourceManager;
import net.chen.legacyLand.resource.pricing.LandPriceCalculator;
import net.chen.legacyLand.resource.pricing.ResourcePricingConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlueMap 地价标记管理（兼容 BlueMap-Towny 2.4.0 接入模式）。
 * <p>
 * 接入约定（参考 {@code codes.antti.bluemaptowny.BlueMapTowny}）：
 * <ul>
 *   <li>{@link BlueMapAPI#onEnable(java.util.function.Consumer)} 触发后调用
 *       {@link #onBlueMapEnable(BlueMapAPI)}，为每个 BlueMap 世界一次性注册 MarkerSet。</li>
 *   <li>每个世界一个 MarkerSet（按 {@link World#getUID()} 缓存），同一 MarkerSet
 *       会被注入到该 {@link BlueMapWorld} 的所有 {@link BlueMapMap} 中。</li>
 *   <li>定时刷新采用「全清后重写」策略，确保国家/城镇解散后旧标记自动消失。</li>
 *   <li>MarkerSet id 与 BlueMap-Towny 的 {@code "towny"} 互不冲突，可并存。</li>
 * </ul>
 */
public class ChunkPriceMap {

    /** 国家地价 POI MarkerSet id；与 BlueMap-Towny 的 "towny" 不重名。 */
    private static final String MARKER_SET_ID = "legacy-land-nation-prices";
    private static final String MARKER_SET_LABEL = "Nation Land Price";

    /** 地块价格 ShapeMarker MarkerSet id。 */
    private static final String CHUNK_MARKER_SET_ID = "legacy-land-chunk-prices";
    private static final String CHUNK_MARKER_SET_LABEL = "Land Prices";

    /** Shape 渲染 Y 坐标（与 BlueMap-Towny 默认 62 接近）。 */
    private static final float SHAPE_Y = 64.0f;
    /** 颜色映射用的参考价格上限。 */
    private static final double COLOR_PRICE_CEILING = 1000.0;

    /** 按世界 UUID 缓存的 MarkerSet——参照 BlueMap-Towny 的 townMarkerSets 设计。 */
    private final Map<UUID, MarkerSet> nationMarkerSets = new ConcurrentHashMap<>();
    private final Map<UUID, MarkerSet> chunkMarkerSets = new ConcurrentHashMap<>();

    // =======================================================================
    // BlueMap 生命周期
    // =======================================================================

    /**
     * 由 {@link BlueMapAPI#onEnable} 回调触发：为每个已映射的 Bukkit 世界注册 MarkerSet。
     * 重复调用是幂等的——会先清空再重建。
     */
    public void onBlueMapEnable(BlueMapAPI api) {
        if (api == null) return;
        nationMarkerSets.clear();
        chunkMarkerSets.clear();
        for (World world : Bukkit.getWorlds()) {
            Optional<BlueMapWorld> bmWorld = api.getWorld(world);
            if (bmWorld.isEmpty()) continue;
            registerWorld(world, bmWorld.get());
        }
    }

    /**
     * 由 {@link BlueMapAPI#onDisable} 回调触发：从所有地图移除 MarkerSet，清空缓存。
     */
    public void onBlueMapDisable(BlueMapAPI api) {
        if (api == null) {
            nationMarkerSets.clear();
            chunkMarkerSets.clear();
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            Optional<BlueMapWorld> bmWorld = api.getWorld(world);
            if (bmWorld.isEmpty()) continue;
            for (BlueMapMap map : bmWorld.get().getMaps()) {
                map.getMarkerSets().remove(MARKER_SET_ID);
                map.getMarkerSets().remove(CHUNK_MARKER_SET_ID);
            }
        }
        nationMarkerSets.clear();
        chunkMarkerSets.clear();
    }

    /** 为单个世界注册两组 MarkerSet（国家 POI + 地块 Shape）。 */
    private void registerWorld(World world, BlueMapWorld bmWorld) {
        MarkerSet nationSet = MarkerSet.builder()
                .label(MARKER_SET_LABEL)
                .toggleable(true)
                .build();
        MarkerSet chunkSet = MarkerSet.builder()
                .label(CHUNK_MARKER_SET_LABEL)
                .toggleable(true)
                .build();
        nationMarkerSets.put(world.getUID(), nationSet);
        chunkMarkerSets.put(world.getUID(), chunkSet);
        for (BlueMapMap map : bmWorld.getMaps()) {
            map.getMarkerSets().put(MARKER_SET_ID, nationSet);
            map.getMarkerSets().put(CHUNK_MARKER_SET_ID, chunkSet);
        }
    }

    /** 懒注册：定时刷新时若发现某世界尚未缓存（例如运行时新增），按需补一次。 */
    private MarkerSet ensureChunkSet(World world, BlueMapAPI api) {
        MarkerSet set = chunkMarkerSets.get(world.getUID());
        if (set != null) return set;
        Optional<BlueMapWorld> bmWorld = api.getWorld(world);
        if (bmWorld.isEmpty()) return null;
        registerWorld(world, bmWorld.get());
        return chunkMarkerSets.get(world.getUID());
    }

    private MarkerSet ensureNationSet(World world, BlueMapAPI api) {
        MarkerSet set = nationMarkerSets.get(world.getUID());
        if (set != null) return set;
        Optional<BlueMapWorld> bmWorld = api.getWorld(world);
        if (bmWorld.isEmpty()) return null;
        registerWorld(world, bmWorld.get());
        return nationMarkerSets.get(world.getUID());
    }

    // =======================================================================
    // 国家总价值计算 & 国家级 POI 标记
    // =======================================================================

    /**
     * 计算国家所有领土区块的地价总和。
     * @param nation Towny 国家；为 null 返回 0
     */
    public double getNationalTotalPrice(Nation nation) {
        if (nation == null) return 0.0;
        ChunkResourceManager mgr = ChunkResourceManager.getInstance();
        if (mgr == null) return 0.0;
        ResourcePricingConfig config = mgr.getConfig();

        double total = 0.0;
        for (Town town : nation.getTowns()) {
            if (town == null) continue;
            for (TownBlock tb : town.getTownBlocks()) {
                if (tb == null || tb.getWorld() == null) continue;
                double price = LandPriceCalculator.valuate(tb.getWorld().getName(), tb.getX(), tb.getZ(), config);
                if (price > 0.0) total += price;
            }
        }
        return total;
    }

    /**
     * 为单个国家更新 POI 标记。标记位置：首都 spawn；首都缺失/未设置 spawn 则跳过。
     */
    public void updateNationPriceMarker(Nation nation) {
        BlueMapAPI api = currentApi();
        if (api == null || nation == null) return;
        writeNationMarker(nation, api);
    }

    private void writeNationMarker(Nation nation, BlueMapAPI api) {
        Town capital = nation.getCapital();
        if (capital == null || !capital.hasSpawn()) return;
        Location spawn;
        try {
            spawn = capital.getSpawn();
        } catch (Throwable t) {
            return;
        }
        if (spawn == null || spawn.getWorld() == null) return;

        MarkerSet set = ensureNationSet(spawn.getWorld(), api);
        if (set == null) return;

        double total = getNationalTotalPrice(nation);
        String markerId = "nation-" + nation.getUUID();
        String label = nation.getName() + " — " + formatPrice(total);

        POIMarker marker = POIMarker.builder()
                .label(label)
                .detail(buildDetailHtml(nation, total))
                .position(spawn.getX(), spawn.getY(), spawn.getZ())
                .maxDistance(2000)
                .build();
        set.getMarkers().put(markerId, marker);
    }

    /**
     * 全清后重写所有国家 POI（参照 BlueMap-Towny 的 updateMarkers 策略）。
     * 国家解散后旧标记会随清空消失。
     */
    public void updateAllNationMarkers() {
        BlueMapAPI api = currentApi();
        if (api == null) return;
        for (MarkerSet set : nationMarkerSets.values()) {
            set.getMarkers().clear();
        }
        try {
            for (Nation nation : TownyAPI.getInstance().getNations()) {
                writeNationMarker(nation, api);
            }
        } catch (Throwable t) {
            LegacyLand.logger.warning("[ChunkPriceMap] 刷新国家地价标记失败: " + t.getMessage());
        }
    }

    /** 从所有世界的 MarkerSet 中移除指定国家的 POI（用于解散事件）。 */
    public void removeNationPriceMarker(Nation nation) {
        if (nation == null) return;
        String markerId = "nation-" + nation.getUUID();
        for (MarkerSet set : nationMarkerSets.values()) {
            set.getMarkers().remove(markerId);
        }
    }

    // =======================================================================
    // 地块级 Shape 标记
    // =======================================================================

    /**
     * 为单个 TownBlock 写入 16×16 的 ShapeMarker，颜色随价格在绿→红之间渐变。
     */
    public void updateChunkPriceMarker(TownBlock tb) {
        BlueMapAPI api = currentApi();
        if (api == null || tb == null || tb.getWorld() == null) return;
        writeChunkMarker(tb, api);
    }

    private void writeChunkMarker(TownBlock tb, BlueMapAPI api) {
        String worldName = tb.getWorld().getName();
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) return;

        ChunkResourceManager mgr = ChunkResourceManager.getInstance();
        if (mgr == null) return;
        ResourcePricingConfig config = mgr.getConfig();

        int cx = tb.getX();
        int cz = tb.getZ();
        double price = LandPriceCalculator.valuate(worldName, cx, cz, config);
        if (price < 0) return; // 未普查

        MarkerSet set = ensureChunkSet(bukkitWorld, api);
        if (set == null) return;

        ChunkResourceData data = mgr.get(worldName, cx, cz).orElse(null);
        double minX = cx * 16.0;
        double minZ = cz * 16.0;
        Shape shape = Shape.createRect(minX, minZ, minX + 16.0, minZ + 16.0);

        Town town = tb.getTownOrNull();
        String label = "[" + cx + "," + cz + "] " + formatPrice(price)
                + (town == null ? "" : " · " + town.getName());

        ShapeMarker marker = ShapeMarker.builder()
                .label(label)
                .detail(buildChunkDetailHtml(cx, cz, town, price, data))
                .shape(shape, SHAPE_Y)
                .lineColor(priceToLineColor(price))
                .fillColor(priceToFillColor(price))
                .lineWidth(2)
                .depthTestEnabled(false)
                .build();

        set.getMarkers().put(chunkMarkerId(worldName, cx, cz), marker);
    }

    /** 单点移除：从对应世界缓存里删除 markerId。 */
    public void removeChunkPriceMarker(String world, int cx, int cz) {
        String markerId = chunkMarkerId(world, cx, cz);
        World bukkit = Bukkit.getWorld(world);
        if (bukkit != null) {
            MarkerSet set = chunkMarkerSets.get(bukkit.getUID());
            if (set != null) set.getMarkers().remove(markerId);
            return;
        }
        for (MarkerSet set : chunkMarkerSets.values()) {
            set.getMarkers().remove(markerId);
        }
    }

    public void updateTownChunkPriceMarkers(Town town) {
        BlueMapAPI api = currentApi();
        if (api == null || town == null) return;
        for (TownBlock tb : town.getTownBlocks()) {
            writeChunkMarker(tb, api);
        }
    }

    /**
     * 全清后重写所有地块 Shape（与 BlueMap-Towny 的批量刷新一致）。
     */
    public void updateAllChunkPriceMarkers() {
        BlueMapAPI api = currentApi();
        if (api == null) return;
        for (MarkerSet set : chunkMarkerSets.values()) {
            set.getMarkers().clear();
        }
        try {
            for (Nation nation : TownyAPI.getInstance().getNations()) {
                for (Town town : nation.getTowns()) {
                    if (town == null) continue;
                    for (TownBlock tb : town.getTownBlocks()) {
                        writeChunkMarker(tb, api);
                    }
                }
            }
        } catch (Throwable t) {
            LegacyLand.logger.warning("[ChunkPriceMap] 刷新地块地价标记失败: " + t.getMessage());
        }
    }

    // =======================================================================
    // 辅助方法
    // =======================================================================

    private static BlueMapAPI currentApi() {
        LegacyLand plugin = LegacyLand.getInstance();
        if (plugin == null || !plugin.isBlueMap()) return null;
        return plugin.blueMapAPI;
    }

    private static String chunkMarkerId(String world, int cx, int cz) {
        return "chunk-" + world + "-" + cx + "-" + cz;
    }

    private static String formatPrice(double price) {
        return "$" + String.format(Locale.ROOT, "%,.2f", price);
    }

    private static Color priceToFillColor(double price) {
        float ratio = (float) Math.min(1.0, Math.max(0.0, price / COLOR_PRICE_CEILING));
        int r = (int) (ratio * 255);
        int g = (int) ((1.0f - ratio) * 200);
        return new Color(r, g, 32, 0.35f);
    }

    private static Color priceToLineColor(double price) {
        float ratio = (float) Math.min(1.0, Math.max(0.0, price / COLOR_PRICE_CEILING));
        int r = (int) (ratio * 200);
        int g = (int) ((1.0f - ratio) * 160);
        return new Color(r, g, 24, 0.9f);
    }

    private static String buildDetailHtml(Nation nation, double total) {
        int townCount = nation.getTowns() == null ? 0 : nation.getTowns().size();
        int townBlockCount = 0;
        for (Town t : nation.getTowns()) {
            if (t != null) townBlockCount += t.getTownBlocks().size();
        }
        return "<div style='font-family:sans-serif'>"
                + "<b>" + escape(nation.getName()) + "</b><br>"
                + "Total land price: <b>" + formatPrice(total) + "</b><br>"
                + "Towns: " + townCount + "<br>"
                + "Chunks: " + townBlockCount
                + "</div>";
    }

    private static String buildChunkDetailHtml(int cx, int cz, Town town, double price, ChunkResourceData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:sans-serif'>")
                .append("<b>Chunk [").append(cx).append(',').append(cz).append("]</b><br>");
        if (town != null) {
            sb.append("Town: ").append(escape(town.getName()));
            try {
                if (town.hasNation() && town.getNationOrNull() != null) {
                    sb.append(" (").append(escape(town.getNationOrNull().getName())).append(')');
                }
            } catch (Throwable ignored) {}
            sb.append("<br>");
        }
        sb.append("Price: <b>").append(formatPrice(price)).append("</b><br>");
        if (data != null) {
            double initial = data.getInitialValue();
            double current = Math.max(0.0, data.getCurrentValue());
            double ratio = initial <= 0 ? 0.0 : (current / initial);
            sb.append(String.format(Locale.ROOT, "Reserve: %.1f / %.1f (%.0f%%)<br>",
                    current, initial, ratio * 100));
            if (data.getBiome() != null) {
                sb.append("Biome: ").append(escape(data.getBiome()))
                        .append(String.format(Locale.ROOT, " (×%.2f)", data.getBiomeFactor()));
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
