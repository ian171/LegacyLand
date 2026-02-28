package net.chen.legacyLand;

import com.palmergames.bukkit.towny.TownyAPI;
import lombok.Getter;
import net.chen.legacyLand.achievements.AchievementManager;
import net.chen.legacyLand.achievements.listener.PlayerAchievementsListener;
import net.chen.legacyLand.command.AdministrationCommands;
import net.chen.legacyLand.config.ConfigManager;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.listeners.TownyEventListener;
import net.chen.legacyLand.listeners.WarEventListener;
import net.chen.legacyLand.listeners.WarProtectionListener;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.commands.DiplomacyCommand;
import net.chen.legacyLand.nation.commands.LegacyCommand;
import net.chen.legacyLand.nation.commands.TaxCommand;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.plot.PlotClaimListener;
import net.chen.legacyLand.nation.plot.PlotClaimManager;
import net.chen.legacyLand.nation.plot.PlotClaimTimerTask;
import net.chen.legacyLand.nation.politics.PoliticalEffectListener;
import net.chen.legacyLand.nation.politics.PoliticalSystemManager;
import net.chen.legacyLand.nation.politics.effects.ParticleEffect;
import net.chen.legacyLand.nation.politics.effects.SpeedBoostEffect;
import net.chen.legacyLand.nation.transaction.NationTradeManager;
import net.chen.legacyLand.placeholder.LegacyLandPlaceholder;
import net.chen.legacyLand.player.PlayerManager;
import net.chen.legacyLand.player.commands.PlayerCommand;
import net.chen.legacyLand.player.listeners.PlayerEventListener;
import net.chen.legacyLand.player.status.*;
import net.chen.legacyLand.season.SeasonManager;
import net.chen.legacyLand.war.WarManager;
import net.chen.legacyLand.war.commands.SiegeCommand;
import net.chen.legacyLand.war.commands.WarCommand;
import net.chen.legacyLand.war.flagwar.FlagWarCommand;
import net.chen.legacyLand.war.flagwar.FlagWarListener;
import net.chen.legacyLand.war.flagwar.FlagWarManager;
import net.chen.legacyLand.war.flagwar.FlagWarTimerTask;
import net.chen.legacyLand.war.siege.OutpostMonitorTask;
import net.chen.legacyLand.war.siege.SiegeWarManager;
import net.chen.legacyLand.market.MarketManager;
import net.chen.legacyLand.market.commands.MarketCommand;
import net.chen.legacyLand.market.commands.PriceCommand;
import net.chen.legacyLand.market.listener.MarketListener;
import net.chen.legacyLand.nation.law.LawCommand;
import net.chen.legacyLand.nation.law.LawManager;
import net.chen.legacyLand.nation.law.LawTimerTask;
import net.chen.legacyLand.nation.tech.TechCommand;
import net.chen.legacyLand.nation.tech.TechManager;
import net.chen.legacyLand.nation.tech.TechPointTask;
import net.chen.legacyLand.organization.OrganizationManager;
import net.chen.legacyLand.util.FoliaScheduler;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.SQLException;
import java.util.logging.Logger;

@Getter
public final class LegacyLand extends JavaPlugin {

    public static Logger logger;
    public static String version = "1.0-Beta2.1";
    public boolean isDev;
    @Getter
    private static LegacyLand instance;
    private ConfigManager configManager;
    private NationManager nationManager;
    private DiplomacyManager diplomacyManager;
    private WarManager warManager;
    private SiegeWarManager siegeWarManager;
    private FlagWarManager flagWarManager;
    private PlotClaimManager plotClaimManager;
    private PlayerManager playerManager;
    private DatabaseManager databaseManager;
    private AchievementManager achievementManager;
    private net.chen.legacyLand.season.SeasonManager seasonManager;
    private PoliticalSystemManager politicalSystemManager;
    private NationTradeManager nationTradeManager;
    private LawManager lawManager;
    private TechManager techManager;

    @Override
    public void onLoad() {
        logger = getLogger();
        if (FoliaScheduler.isFolia()){
            logger.warning("You are using Folia Client");
            logger.warning("Folia supporting is still in test. Report your bugs in Github!");
        }else {
            logger.warning("The plugin is in test. Please report your bugs in Github!");
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        printBar();
        logger.info("LegacyLand 插件已启用！");
        logger.warning("Unauthorized modification and redistribution prohibited.");
        logger.info("LegacyLand Version: "+version);
        // 检查 Towny 依赖
        if (getServer().getPluginManager().getPlugin("Towny") == null) {
            logger.severe("Towny 插件未找到！插件无法启动。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //incompatiblePluginsChecker();
        Thread virtual = Thread.ofVirtual().unstarted(() -> {
            // 注册命令
            registerCommands();
            // 注册监听器
            registerListeners();

            // 注册 PlaceholderAPI

        });

        // 加载配置
        configManager = new ConfigManager(this);
        virtual.start();
        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // 初始化管理器
        nationManager = NationManager.getInstance();
        if (isDev) {
            logger.info("国家系统已加载。");
        }
        nationManager.setDatabase(databaseManager);
        // 加载政治体制配置
        politicalSystemLoader();
        if (isDev) {
            logger.info("政治体制系统已加载。");
        }
        // 加载所有已有国家的扩展数据（政体持久化）
        for (com.palmergames.bukkit.towny.object.Nation nation : TownyAPI.getInstance().getNations()) {
            nationManager.loadNationData(nation.getName());
        }
        logger.info("已加载 " + TownyAPI.getInstance().getNations().size() + " 个国家的扩展数据。");
        diplomacyManager = DiplomacyManager.getInstance();
        if (isDev) {
            logger.info("外交系统已加载。");
            logger.info("税收系统已加载。");
        }
        warManager = WarManager.getInstance();
        siegeWarManager = SiegeWarManager.getInstance();
        if (isDev) {
            logger.info("战争系统已加载。");
        }
        flagWarManager = FlagWarManager.getInstance();
        flagWarManager.loadFromDatabase(databaseManager);
        if (isDev) {
            logger.info("FlagWar 系统已加载。");
        }
        plotClaimManager = PlotClaimManager.getInstance();
        if (isDev) {
            logger.info("PlotClaim 系统已加载。");
        }
        playerManager = PlayerManager.getInstance();
        playerManager.setDatabase(databaseManager);
        if (isDev) {
            logger.info("玩家系统已加载。");
        }
        achievementManager = new AchievementManager(playerManager, databaseManager);
        AchievementManager.setInstance(achievementManager);
        if (isDev) {
            logger.info("成就系统已加载。");
        }
        vault_init();
        if (isDev) {
            logger.info("经济系统已加载");
        }
        nationTradeManager = new NationTradeManager(getNationManager());
        // 初始化季节系统
        seasonManager = new SeasonManager(this);
        seasonManager.start();
        if (isDev) {
            logger.info("季节系统已加载。");
        }

        // 初始化组织系统和市场系统（需要 DB 连接）
        java.sql.Connection dbConn = null;
        try {
            dbConn = databaseManager.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (dbConn != null) {
            OrganizationManager.getInstance(this).init(dbConn);
            MarketManager.getInstance(this).init(dbConn);
            logger.info("组织系统和市场系统已加载。");

            // 法令系统
            lawManager = LawManager.getInstance(this);
            lawManager.init(dbConn);
            logger.info("法令系统已加载。");

            // 科技树系统
            techManager = TechManager.getInstance(this);
            techManager.loadConfig(this);
            techManager.init(dbConn);
            logger.info("科技树系统已加载。");
        } else {
            logger.warning("无法获取数据库连接，组织/市场/法令/科技系统将无法持久化（MongoDB 暂不支持）。");
        }

        try {
            LegacyLand.initItemsadderItems();
        } catch (IOException e) {
            LegacyLand.logger.severe("Failed to write items");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LegacyLandPlaceholder(this, seasonManager).register();
            if (isDev) {
                logger.info("PlaceholderAPI 已注册！");
            }
        } else {
            logger.warning("未找到 PlaceholderAPI，变量功能将不可用！");
        }

        // 启动前哨战监控任务（每分钟检查一次）
        FoliaScheduler.runTaskTimerGlobal(instance, new OutpostMonitorTask(instance), 1200L, 1200L);
        // 启动 FlagWar 计时器任务（每秒检查一次）
        FoliaScheduler.runTaskTimerGlobal(instance, new FlagWarTimerTask(), 20L, 20L);
        // 启动 PlotClaim 计时器任务（每秒检查一次）
        FoliaScheduler.runTaskTimerGlobal(instance, new PlotClaimTimerTask(), 20L, 20L);
        // 启动状态更新任务（每5秒检查一次）
        FoliaScheduler.runTaskTimerGlobal(instance, new StatusUpdateTask(), 10L, 10L);
        // 启动法令定时任务（每分钟检查一次）
        FoliaScheduler.runTaskTimerGlobal(instance, new LawTimerTask(), 1200L, 1200L);
        // 启动科技研究点生成任务
        int techInterval = getConfig().getInt("tech.research-tick-interval", 6000);
        FoliaScheduler.runTaskTimerGlobal(instance, new TechPointTask(), techInterval, techInterval);

        // 根据配置决定是否启用 ActionBar
        boolean enableActionBar = getConfig().getBoolean("player-status.enable-actionbar", true);
        if (enableActionBar) {
            int interval = getConfig().getInt("player-status.actionbar-update-interval", 10);
            FoliaScheduler.runTaskTimerGlobal(instance, new ActionBarUpdateTask(), interval, interval);
            //logger.info("ActionBar 显示已启用（更新间隔: " + interval + " ticks）");
        }
    }

    private void politicalSystemLoader() {
        politicalSystemManager = PoliticalSystemManager.getInstance();
        politicalSystemManager.load(this);
        // 注册自定义效果工厂
        politicalSystemManager.registerEffectFactory("speed-boost", (nation, config) -> {
            int amplifier = 0;
            if (config != null && config.containsKey("amplifier")) {
                Object ampValue = config.get("amplifier");
                if (ampValue instanceof Number) {
                    amplifier = ((Number) ampValue).intValue();
                }
            }
            return new SpeedBoostEffect(amplifier);
        });
        politicalSystemManager.registerEffectFactory("particle-effect", (nation, config) -> {
            if (config == null) return null;

            String particleName = (String) config.get("particle");
            String patternName = (String) config.get("pattern");

            if (particleName == null || patternName == null) return null;

            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName.toUpperCase());
                ParticleEffect.ParticlePattern pattern = ParticleEffect.ParticlePattern.valueOf(patternName.toUpperCase());
                return new ParticleEffect(particle, pattern);
            } catch (IllegalArgumentException e) {
                logger.warning("无效的粒子效果配置: particle=" + particleName + ", pattern=" + patternName);
                return null;
            }
        });
    }

    @Override
    public void onDisable() {
        // 停止季节系统
        if (seasonManager != null) {
            seasonManager.stop();
        }
        // 断开数据库连接
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    private void registerCommands(){
        AdministrationCommands administrationCommands = new AdministrationCommands();
        instance.getCommand("legacylandadmin").setExecutor(administrationCommands);

        LegacyCommand legacyCommand = new LegacyCommand();
        instance.getCommand("legacy").setExecutor(legacyCommand);
        instance.getCommand("legacy").setTabCompleter(legacyCommand);

        TaxCommand taxCommand = new TaxCommand();
        instance.getCommand("tax").setExecutor(taxCommand);
        instance.getCommand("tax").setTabCompleter(taxCommand);

        DiplomacyCommand diplomacyCommand = new DiplomacyCommand();
        instance.getCommand("diplomacy").setExecutor(diplomacyCommand);
        instance.getCommand("diplomacy").setTabCompleter(diplomacyCommand);

        WarCommand warCommand = new WarCommand();
        instance.getCommand("war").setExecutor(warCommand);
        instance.getCommand("war").setTabCompleter(warCommand);

        SiegeCommand siegeCommand = new SiegeCommand();
        instance.getCommand("siege").setExecutor(siegeCommand);
        instance.getCommand("siege").setTabCompleter(siegeCommand);

        PlayerCommand playerCommand = new PlayerCommand();
        instance.getCommand("player").setExecutor(playerCommand);
        instance.getCommand("player").setTabCompleter(playerCommand);

        StatusCommand statusCommand = new StatusCommand();
        instance.getCommand("status").setExecutor(statusCommand);
        instance.getCommand("status").setTabCompleter(statusCommand);

        MedicalItemCommand medicalItemCommand = new MedicalItemCommand();
        instance.getCommand("heal").setExecutor(medicalItemCommand);
        instance.getCommand("drink").setExecutor(medicalItemCommand);

        net.chen.legacyLand.season.SeasonCommand seasonCommand = new net.chen.legacyLand.season.SeasonCommand(this, seasonManager);
        instance.getCommand("season").setExecutor(seasonCommand);
        instance.getCommand("season").setTabCompleter(seasonCommand);

        FlagWarCommand flagWarCommand = new FlagWarCommand();
        instance.getCommand("flagwar").setExecutor(flagWarCommand);
        instance.getCommand("flagwar").setTabCompleter(flagWarCommand);

        MarketCommand marketCommand = new MarketCommand();
        instance.getCommand("market").setExecutor(marketCommand);
        instance.getCommand("market").setTabCompleter(marketCommand);

        PriceCommand priceCommand = new PriceCommand();
        instance.getCommand("price").setExecutor(priceCommand);
        instance.getCommand("price").setTabCompleter(priceCommand);

        LawCommand lawCommand = new LawCommand();
        instance.getCommand("law").setExecutor(lawCommand);
        instance.getCommand("law").setTabCompleter(lawCommand);

        TechCommand techCommand = new TechCommand();
        instance.getCommand("tech").setExecutor(techCommand);
        instance.getCommand("tech").setTabCompleter(techCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new WarProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new WarEventListener(), this);
        getServer().getPluginManager().registerEvents(new TownyEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerAchievementsListener(),this);
        getServer().getPluginManager().registerEvents(new PlayerStatusListener(), this);
        getServer().getPluginManager().registerEvents(new FlagWarListener(), this);
        getServer().getPluginManager().registerEvents(new PlotClaimListener(), this);
        getServer().getPluginManager().registerEvents(new PoliticalEffectListener(), this);
        getServer().getPluginManager().registerEvents(new MarketListener(this), this);
    }
    public static void initItemsadderItems() throws IOException {
        // 修正资源路径（去掉 /resources/）
        try (InputStream inputStream = LegacyLand.class.getResourceAsStream("/itemsadder/medical_items.yml")) {
            if (inputStream == null) {
                logger.warning("无法找到资源文件: /itemsadder/medical_items.yml");
                return;
            }

            File file = new File("plugins/ItemsAdder/contents/medical_items.yml");

            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.severe("无法创建目录: " + parentDir.getPath());
                    return;
                }
            }

            // 使用 try-with-resources 自动关闭流
            try (OutputStream op = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    op.write(buffer, 0, bytesRead);
                }
            }

            logger.info("ItemsAdder 医疗物品配置已成功复制");
        }
    }
    @Deprecated
    public static void incompatiblePluginsChecker(){
        try {
            if (Bukkit.getPluginManager().getPlugin("RealisticSeason").isEnabled()){
                Bukkit.getPluginManager().getPlugin("RealisticSeason").onDisable();
            }
        } catch (NullPointerException ignore) {

        }
    }
    @Getter
    private static Economy econ = null;
    @Getter
    private static Permission perms = null;
    @Getter
    private static Chat chat = null;

    public void vault_init(){
        if (!setupEconomy()) {
            logger.warning("未找到 Vault 或经济插件，经济功能将不可用！");
            return;
        }
        setupPermissions();
        setupChat();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
        return econ != null;
    }
    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            chat = rsp.getProvider();
        }
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            perms = rsp.getProvider();
        }
        return perms != null;
    }
    public static void printBar(){
        logger.info("""
                  _                                _                 _\s
                 | |    ___  __ _  __ _  ___ _   _| | __ _ _ __   __| |
                 | |   / _ \\/ _` |/ _` |/ __| | | | |/ _` | '_ \\ / _` |
                 | |__|  __/ (_| | (_| | (__| |_| | | (_| | | | | (_| |
                 |_____\\___|\\__, |\\__,_|\\___|\\__, |_|\\__,_|_| |_|\\__,_|
                            |___/            |___/                     \
                """);
    }
}
