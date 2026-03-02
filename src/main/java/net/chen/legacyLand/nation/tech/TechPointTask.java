package net.chen.legacyLand.nation.tech;

/**
 * 科技研究点生成定时任务
 */
public class TechPointTask implements Runnable {

    @Override
    public void run() {
        TechManager manager = TechManager.getInstance();
        if (manager == null) return;
        manager.generateResearchPoints();
    }
}
