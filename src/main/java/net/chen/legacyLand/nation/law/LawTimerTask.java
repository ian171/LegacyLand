package net.chen.legacyLand.nation.law;

/**
 * 法令定时任务 - 每分钟检查过期法令和投票截止
 */
public class LawTimerTask implements Runnable {

    @Override
    public void run() {
        LawManager manager = LawManager.getInstance();
        if (manager == null) return;
        manager.checkExpiredLaws();
        manager.checkVoteDeadlines();
    }
}
