package net.chen.legacyLand.item;

import org.bukkit.Material;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 资源包 overrides 导出工具
 *
 * 输出格式（可直接嵌入 minecraft/models/item/<material>.json 的 overrides 数组）：
 * {
 *   "glass_bottle": [
 *     { "predicate": { "custom_model_data": 1001 }, "model": "legacyland:item/mercury_thermometer" }
 *   ]
 * }
 */
public final class ResourcePackExporter {

    private ResourcePackExporter() {}

    /**
     * 将注册表导出为资源包 overrides JSON，写入指定文件
     *
     * @param outputPath 输出文件路径，如 plugins/LegacyLand/overrides.json
     */
    public static void exportToFile(Path outputPath) throws IOException {
        String json = buildJson();
        Files.createDirectories(outputPath.getParent());
        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(json);
        }
    }

    /**
     * 构建 JSON 字符串
     */
    public static String buildJson() {
        // 按 Material 分组，每组内按 CMD 升序排列
        Map<Material, List<CustomItem>> grouped = new TreeMap<>(Comparator.comparing(Material::name));
        for (CustomItem item : ItemsRegistry.getAll().values()) {
            grouped.computeIfAbsent(item.getBaseMaterial(), k -> new ArrayList<>()).add(item);
        }
        grouped.values().forEach(list -> list.sort(Comparator.comparingInt(CustomItem::getCmd)));

        StringBuilder sb = new StringBuilder("{\n");
        Iterator<Map.Entry<Material, List<CustomItem>>> matIter = grouped.entrySet().iterator();

        while (matIter.hasNext()) {
            Map.Entry<Material, List<CustomItem>> entry = matIter.next();
            String materialKey = entry.getKey().name().toLowerCase();

            sb.append("  \"").append(materialKey).append("\": [\n");

            List<CustomItem> items = entry.getValue();
            for (int i = 0; i < items.size(); i++) {
                CustomItem item = items.get(i);
                sb.append("    {\n");
                sb.append("      \"predicate\": { \"custom_model_data\": ").append(item.getCmd()).append(" },\n");
                sb.append("      \"model\": \"").append(item.getTexturePath()).append("\",\n");
                sb.append("      \"_id\": \"").append(item.getId()).append("\"\n");
                sb.append("    }");
                if (i < items.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append("  ]");
            if (matIter.hasNext()) sb.append(",");
            sb.append("\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
