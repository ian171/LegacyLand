package net.chen.legacyLand.resource.commands;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.chen.legacyLand.resource.pricing.ChunkResourceData;
import net.chen.legacyLand.resource.pricing.LandPriceCalculator;
import net.chen.legacyLand.resource.pricing.LandPriceInquiry;
import net.chen.legacyLand.resource.pricing.LandPriceManager;
import net.chen.legacyLand.resource.pricing.ChunkResourceManager;
import net.chen.legacyLand.resource.pricing.ResourcePricingConfig;
import net.chen.legacyLand.util.LanguageManager;
import org.bukkit.Bukkit;
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
 */
public class LandPriceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("info", "ask", "reply", "show", "hide", "list");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().translate("msg.player_only"));
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
            player.sendMessage(LanguageManager.getInstance().translate("landprice.private",
                    town.getName()));
            return true;
        }

        ResourcePricingConfig cfg = crm.getConfig();
        double price = LandPriceCalculator.valuate(world, cx, cz, cfg);
        if (price < 0) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.not_surveyed"));
            return true;
        }

        ChunkResourceData data = crm.get(world, cx, cz).orElse(null);
        if (data == null) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.not_surveyed"));
            return true;
        }
        double ratio = data.getInitialValue() <= 0 ? 0.0
                : Math.max(0.0, data.getCurrentValue()) / data.getInitialValue();

        player.sendMessage(LanguageManager.getInstance().translate("landprice.header",
                cx + "," + cz,
                town == null ? "—" : town.getName()));
        player.sendMessage(LanguageManager.getInstance().translate("landprice.price_line",
                String.format("%.2f", price)));
        player.sendMessage(LanguageManager.getInstance().translate("landprice.reserve_line",
                String.format("%.1f", data.getCurrentValue()),
                String.format("%.1f", data.getInitialValue()),
                String.format("%.0f%%", ratio * 100)));
        player.sendMessage(LanguageManager.getInstance().translate("landprice.biome_line",
                data.getBiome() == null ? "?" : data.getBiome(),
                String.format("%.2f", data.getBiomeFactor())));
        if (isPublic) player.sendMessage(LanguageManager.getInstance().translate("landprice.public_tag"));
        return true;
    }

    private boolean handleAsk(Player player, String[] args) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        if (lpm == null || crm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        Town town = lpm.townOf(player.getLocation());
        if (town == null) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.unclaimed"));
            return true;
        }
        if (lpm.isMember(player, town)) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.cant_ask_own"));
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

        player.sendMessage(LanguageManager.getInstance().translate("landprice.ask_sent",
                town.getName(), String.valueOf(inq.id())));

        // 通知 town 在线成员
        String chunkLabel = cx + "," + cz;
        for (Resident r : town.getResidents()) {
            Player member = Bukkit.getPlayer(r.getUUID());
            if (member != null && member.isOnline() && !member.getUniqueId().equals(player.getUniqueId())) {
                member.sendMessage(LanguageManager.getInstance().translate("landprice.ask_received",
                        player.getName(), chunkLabel, String.valueOf(inq.id()),
                        message.isEmpty() ? "-" : message));
            }
        }
        return true;
    }

    private boolean handleReply(Player player, String[] args) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        ChunkResourceManager crm = ChunkResourceManager.getInstance();
        if (lpm == null || crm == null) return true;

        if (args.length < 3) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.reply_usage"));
            return true;
        }

        long id;
        double price;
        try {
            id = Long.parseLong(args[1]);
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.reply_invalid"));
            return true;
        }
        if (price < 0) {
            player.sendMessage(LanguageManager.getInstance().translate("error.price_positive"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Town town = lpm.townOf(player.getLocation());
        if (town == null || !lpm.isMember(player, town)) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.not_member"));
            return true;
        }

        LandPriceInquiry q = lpm.findInquiry(chunkKey, id);
        if (q == null) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.reply_not_found",
                    String.valueOf(id)));
            return true;
        }
        if (q.isReplied()) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.reply_already",
                    q.repliedByName(), String.format("%.2f", q.quotedPrice())));
            return true;
        }

        boolean ok = lpm.replyInquiry(chunkKey, id, player, price);
        if (ok) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.reply_sent",
                    q.askerName(), String.format("%.2f", price)));
        }
        return true;
    }

    private boolean handleShow(Player player, boolean show) {
        LandPriceManager lpm = LandPriceManager.getInstance();
        if (lpm == null) return true;

        Chunk chunk = player.getLocation().getChunk();
        Town town = lpm.townOf(player.getLocation());
        if (town == null) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.unclaimed"));
            return true;
        }
        if (!lpm.isMember(player, town)) {
            player.sendMessage(LanguageManager.getInstance().translate("landprice.not_member"));
            return true;
        }

        String chunkKey = ChunkResourceData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        boolean changed = lpm.setShown(player.getUniqueId(), chunkKey, show);
        if (!changed) {
            player.sendMessage(LanguageManager.getInstance().translate(
                    show ? "landprice.show_already" : "landprice.hide_already"));
            return true;
        }
        player.sendMessage(LanguageManager.getInstance().translate(
                show ? "landprice.show_ok" : "landprice.hide_ok",
                chunk.getX() + "," + chunk.getZ()));
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
            player.sendMessage(LanguageManager.getInstance().translate("landprice.list_empty"));
            return true;
        }
        player.sendMessage(LanguageManager.getInstance().translate("landprice.list_header",
                chunk.getX() + "," + chunk.getZ(), String.valueOf(list.size())));
        for (LandPriceInquiry q : list) {
            if (q.isReplied()) {
                player.sendMessage(LanguageManager.getInstance().translate("landprice.list_replied",
                        String.valueOf(q.id()), q.askerName(),
                        q.repliedByName(), String.format("%.2f", q.quotedPrice())));
            } else {
                player.sendMessage(LanguageManager.getInstance().translate("landprice.list_open",
                        String.valueOf(q.id()), q.askerName(),
                        q.message().isEmpty() ? "-" : q.message()));
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        LanguageManager lm = LanguageManager.getInstance();
        player.sendMessage(lm.translate("landprice.help_header"));
        player.sendMessage(lm.translate("landprice.help_info"));
        player.sendMessage(lm.translate("landprice.help_ask"));
        player.sendMessage(lm.translate("landprice.help_reply"));
        player.sendMessage(lm.translate("landprice.help_show"));
        player.sendMessage(lm.translate("landprice.help_hide"));
        player.sendMessage(lm.translate("landprice.help_list"));
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
