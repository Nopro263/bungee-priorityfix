package at.nopro.testproxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

public final class Testproxy extends Plugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        CustomReconnect.populateCache();
        ProxyServer.getInstance().setReconnectHandler(new CustomReconnect());

        ProxyServer.getInstance().getScheduler().schedule(this, CustomReconnect::populateCache, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
