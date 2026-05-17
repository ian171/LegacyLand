package net.chen.legacyLand.resource.commands;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.resource.pricing.ChunkResourceData;
import net.chen.legacyLand.resource.pricing.LandPriceCalculator;
import net.chen.legacyLand.resource.pricing.LandPriceInquiry;
import net.chen.legacyLand.resource.pricing.LandPriceManager;
import net.chen.legacyLand.resource.pricing.ChunkResourceManager;
import net.chen.legacyLand.resource.pricing.ResourcePricingConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /landprice 命令（P3）：地价查询、询问、回复、公开/隐藏。
 * <p>
 * 子命令：
 * <ul>
 *   <li>{@code /landprice}（无参）— 显示脚下区块地价（成员可见，或已被 show 公开时全员可见）。</li>
 *   <li>{@code /landprice ask [留言]} — 向当前地块所属城镇询问地价（非成员，地块必须已声明）。</li>
 *   <li>{@code /landprice reply <inquiryId> <price>} — 城镇成员回复询问。</li>
 *   <li>{@code /landprice show} / {@code hide} — 切换脚下区块的公开展示状态（仅成员可操作）。</li>
 *   <li>{@code /landprice list} — 列出脚下区块当前所有未回复的询问。</li>
 * </ul>
 * TODO: 语言文件加载修复后恢复 i18n。
 */
public class LandPriceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("info", "ask", "reply", "show", "hide", "list");

    private static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("&c此命令只能由玩家使用！"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            return handleInfo(player);
        }

        return switch (args[0].toLowerCase()) {
            case "ask" -> handleAsk(player, args);
            case "reply" -> handleReply(player, args);
            case "show" -> handleShow(player, true);
            case "hide" -> handleShow(player, false);
            case "list" -> handleList(player);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    // -----------------------------------------------------------------------

    private boolean handleInfo(Player player) {
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        LandPriceManager lpm = LandPriceManager.getInstance();
        if (crm == null || lpm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        String chunkKey = ChunkResourceData.key(world, cx, cz);

        Location loc = player.getLocation();
        Town town = lpm.townOf(loc);
        boolean isMember = lpm.isMember(player, town);
        boolean isPublic = lpm.isPubliclyShown(chunkKey);

        // 未声明的地块：所有人可查看
        // 已声明的地块：仅成员或已公开可查看
        if (town != null && !isMember && !isPublic) {
            player.sendMessage(c("&c此地块价格仅对 " + town.getName() + " 成员可见"));
            return true;
        }

        ResourcePricingConfig cfg = crm.getConfig();
        double price = LandPriceCalculator.valuate(world, cx, cz, cfg);
        if (price < 0) {
            player.sendMessage(c("&c此地块尚未普查,地价暂无数据"));
            return true;
        }

        ChunkResourceData data = crm.get(world, cx, cz).orElse(null);
        if (data == null) {
            player.sendMessage(c("&c此地块尚未普查,地价暂无数据"));
            return true;
        }
        double ratio = data.getInitialValue() <= 0 ? 0.0
                : Math.max(0.0, data.getCurrentValue()) / data.getInitialValue();

        String townName = town == null ? "—" : town.getName();
        player.sendMessage(c(String.format("&6===== 地价 [%d,%d] 城镇:%s =====", cx, cz, townName)));
        player.sendMessage(c(String.format("&e地价: &f%.2f", price)));
        player.sendMessage(c(String.format("&7剩余储量: &f%.1f&7/&f%.1f &7(%.0f%%)",
                data.getCurrentValue(), data.getInitialValue(), ratio * 100)));
        player.sendMessage(c(String.format("&7群系: &f%s &7(系数 %.2f)",
                data.getBiome() == null ? "?" : data.getBiome(), data.getBiomeFactor())));
        if (isPublic) player.sendMessage(c("&a[公开] 此地块价格对所有人可见"));
        return true;
    }

    private boolean handleAsk(Player player, String[] args) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        if (lpm == null || crm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        Town town = lpm.townOf(player.getLocation());
        if (town == null) {
            player.sendMessage(c("&c此地块未被任何城镇声明"));
            return true;
        }
        if (lpm.isMember(player, town)) {
            player.sendMessage(c("&c你是此地块成员,无需发起询问"));
            return true;
        }

        String message = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "";

        String world = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        long ttl = crm.getConfig().getInquiryTtlSeconds() * 1000L;
        LandPriceInquiry inq = lpm.submitInquiry(player, world, cx, cz, message, ttl);

        player.sendMessage(c(String.format("&a已向 %s 发出询问 #%d", town.getName(), inq.id())));

        // 通知 town 在线成员
        String chunkLabel = cx + "," + cz;
        String displayMsg = message.isEmpty() ? "-" : message;
        for (Resident r : town.getResidents()) {
            Player member = Bukkit.getPlayer(r.getUUID());
            if (member != null && member.isOnline() && !member.getUniqueId().equals(player.getUniqueId())) {
                member.sendMessage(c(String.format("&e[地价询问] &f%s &7询问地块 [%s] 询价 (#%d): &f%s",
                        player.getName(), chunkLabel, inq.id(), displayMsg)));
            }
        }
        return true;
    }

    private boolean handleReply(Player player, String[] args) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        if (lpm == null || crm == null) return true;

        if (args.length < 3) {
            player.sendMessage(c("&c用法: /landprice reply <询问ID> <价格>"));
            return true;
        }

        long id;
        double price;
        try {
            id = Long.parseLong(args[1]);
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(c("&c询问ID 或价格格式错误"));
            return true;
        }
        if (price < 0) {
            player.sendMessage(c("&c价格必须大于0!"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Town town = lpm.townOf(player.getLocation());
        if (town == null || !lpm.isMember(player, town)) {
            player.sendMessage(c("&c你不是此地块所属城镇的成员"));
            return true;
        }

        LandPriceInquiry q = lpm.findInquiry(chunkKey, id);
        if (q == null) {
            player.sendMessage(c(String.format("&c未找到询问 #%d", id)));
            return true;
        }
        if (q.isReplied()) {
            player.sendMessage(c(String.format("&c该询问已由 %s 回复 (%.2f)",
                    q.repliedByName(), q.quotedPrice())));
            return true;
        }

        boolean ok = lpm.replyInquiry(chunkKey, id, player, price);
        if (ok) {
            player.sendMessage(c(String.format("&a已向 %s 回复地价 %.2f", q.askerName(), price)));
        }
        return true;
    }

    private boolean handleShow(Player player, boolean show) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        if (lpm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        Town town = lpm.townOf(player.getLocation());
        if (town == null) {
            player.sendMessage(c("&c此地块未被任何城镇声明"));
            return true;
        }
        if (!lpm.isMember(player, town)) {
            player.sendMessage(c("&c你不是此地块所属城镇的成员"));
            return true;
        }

        String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        boolean changed = lpm.setShown(player.getUniqueId(), chunkKey, show);
        if (!changed) {
            player.sendMessage(c(show ? "&c该地块已处于公开状态" : "&c该地块本就未公开"));
            return true;
        }
        String chunkLabel = chunk.getX() + "," + chunk.getZ();
        player.sendMessage(c(show
                ? String.format("&a已公开 [%s] 的地价", chunkLabel)
                : String.format("&7已隐藏 [%s] 的地价", chunkLabel)));
        return true;
    }

    private boolean handleList(Player player) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        if (lpm == null || crm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        long ttl = crm.getConfig().getInquiryTtlSeconds() * 1000L;
        List<LandPriceInquiry> list = lpm.listInquiries(chunkKey, ttl);

        if (list.isEmpty()) {
            player.sendMessage(c("&7当前地块没有未过期的询问"));
            return true;
        }
        String chunkLabel = chunk.getX() + "," + chunk.getZ();
        player.sendMessage(c(String.format("&6===== 地块 [%s] 询问列表 (%d) =====",
                chunkLabel, list.size())));
        for (LandPriceInquiry q : list) {
            if (q.isReplied()) {
                player.sendMessage(c(String.format("&7#%d &f%s &a→ %s: &f%.2f",
                        q.id(), q.askerName(), q.repliedByName(), q.quotedPrice())));
            } else {
                player.sendMessage(c(String.format("&7#%d &f%s &7待回复: &8%s",
                        q.id(), q.askerName(), q.message().isEmpty() ? "-" : q.message())));
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(c("&6========== 地价命令 =========="));
        player.sendMessage(c("&e/landprice &7- 查看脚下地块地价"));
        player.sendMessage(c("&e/landprice ask [留言] &7- 询问当前地块所属城镇"));
        player.sendMessage(c("&e/landprice reply <ID> <价格> &7- 回复询问（仅成员）"));
        player.sendMessage(c("&e/landprice show &7- 公开当前地块地价"));
        player.sendMessage(c("&e/landprice hide &7- 取消公开"));
        player.sendMessage(c("&e/landprice list &7- 列出当前地块的询问"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) if (sub.startsWith(prefix)) result.add(sub);
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reply")
                && sender instanceof Player p) {
            LandPriceManager lpm = LandPriceManager.getInstance();
            ChunkResourceManager crm = ChunkResourceManager.getInstance();
            if (lpm == null || crm == null) return List.of();
            Chunk chunk = p.getLocation().getChunk();
            String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            long ttl = crm.getConfig().getInquiryTtlSeconds() * 1000L;
            List<String> ids = new ArrayList<>();
            for (LandPriceInquiry q : lpm.listInquiries(chunkKey, ttl)) {
                if (!q.isReplied()) ids.add(String.valueOf(q.id()));
            }
            return ids;
        }
        return new ArrayList<>();
    }
}
