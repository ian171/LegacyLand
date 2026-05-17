package net.chen.legacyLand.util;

import lombok.Getter;
import lombok.Setter;
import net.chen.legacyLand.LegacyLand;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 语言管理器 - 负责加载和管理多语言文件
 */
public class LanguageManager {
    @Getter
    private static LanguageManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final Map<Locale, YamlConfiguration> languages = new HashMap<>();
    /**
     * -- SETTER --
     *  设置默认语言
     * -- GETTER --
     *  获取默认语言

     */
    @Getter
    @Setter
    private Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;

    private LanguageManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static LanguageManager getInstance(LegacyLand plugin) {
        if (instance == null) {
            instance = new LanguageManager(plugin);
        }
        return instance;
    }

    /**
     * 初始化语言系统
     */
    public void init() {
        // 创建 lang 目录
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            logger.severe("无法创建 lang 目录，请检查文件夹权限");
        }
        // 保存默认语言文件
        saveDefaultLanguageFiles();

        // 加载所有语言文件
        loadLanguages();

        logger.info("语言系统已加载，支持 " + languages.size() + " 种语言");
    }

    /**
     * 保存默认语言文件到插件目录；若已存在则补齐 jar 内新增的键。
     */
    private void saveDefaultLanguageFiles() {
        String[] defaultLangs = {"zh-cn.yml", "en-us.yml"};

        for (String langFile : defaultLangs) {
            File file = new File(plugin.getDataFolder(), "lang/" + langFile);
            if (!file.exists()) {
                plugin.saveResource("lang/" + langFile, false);
                logger.info("已创建默认语言文件: " + langFile);
                continue;
            }
            mergeMissingKeys(file, langFile);
        }
    }

    /**
     * 合并 jar 内默认语言文件中缺失的键到磁盘文件。
     * 避免每次新增 i18n 键时玩家需要手动删除旧 lang 文件。
     */
    private void mergeMissingKeys(File diskFile, String resourcePath) {
        try (InputStream in = plugin.getResource("lang/" + resourcePath)) {
            if (in == null) return;
            YamlConfiguration jarCfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            YamlConfiguration diskCfg = YamlConfiguration.loadConfiguration(diskFile);

            int added = 0;
            for (String key : jarCfg.getKeys(true)) {
                if (jarCfg.isConfigurationSection(key)) continue;
                if (!diskCfg.contains(key)) {
                    diskCfg.set(key, jarCfg.get(key));
                    added++;
                }
            }
            if (added > 0) {
                diskCfg.save(diskFile);
                logger.info("已为 " + resourcePath + " 补齐 " + added + " 个缺失键");
            }
        } catch (IOException e) {
            logger.warning("合并语言文件失败: " + resourcePath + " - " + e.getMessage());
        }
    }

    /**
     * 加载所有语言文件
     */
    private void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() || !langDir.isDirectory()) {
            logger.warning("语言文件目录不存在: " + langDir.getPath());
            return;
        }

        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.warning("未找到任何语言文件");
            return;
        }

        for (File file : files) {
            String fileName = file.getName().replace(".yml", "");
            Locale locale = parseLocale(fileName);

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(locale, config);
                logger.info("已加载语言文件: " + fileName + " (" + locale.getDisplayName() + ")");
            } catch (Exception e) {
                logger.severe("加载语言文件失败: " + fileName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 重新加载所有语言文件
     */
    public void reload() {
        languages.clear();
        loadLanguages();
        logger.info("语言文件已重新加载");
    }

    /**
     * 获取翻译文本
     */
    public String translate(String key, Locale locale) {
        YamlConfiguration config = languages.get(locale);

        // 如果指定语言不存在，使用默认语言
        if (config == null) {
            config = languages.get(defaultLocale);
        }

        // 如果默认语言也不存在，返回键名
        if (config == null) {
            return key;
        }

        String translation = config.getString(key);
        return translation != null ? translation : key;
    }

    /**
     * 获取翻译文本（带参数）
     */
    public String translate(String key, Locale locale, Object... args) {
        String translation = translate(key, locale);

        // 替换占位符 {0}, {1}, {2}...
        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }
        translation = translation.replace("&","§");

        return translation;
    }

    /**
     * 获取翻译文本（使用默认语言）
     */
    public String translate(String key) {
        return translate(key, defaultLocale);
    }

    /**
     * 获取翻译文本（使用默认语言，带参数）
     */
    public String translate(String key, Object... args) {
        return translate(key, defaultLocale, args);
    }

    /**
     * 解析语言代码为 Locale
     */
    private Locale parseLocale(String localeString) {
        String[] parts = localeString.toLowerCase().split("[-_]");

        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1].toUpperCase());
        } else {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }

    /**
     * 检查是否支持指定语言
     */
    public boolean isSupported(Locale locale) {
        return languages.containsKey(locale);
    }

    /**
     * 获取所有支持的语言
     */
    public Map<Locale, YamlConfiguration> getLanguages() {
        return new HashMap<>(languages);
    }
}
