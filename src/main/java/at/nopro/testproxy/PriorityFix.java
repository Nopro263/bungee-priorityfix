package at.nopro.testproxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class PriorityFix extends Plugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        CustomReconnect.populateCache();
        ProxyServer.getInstance().setReconnectHandler(new CustomReconnect(ProxyServer.getInstance().getReconnectHandler()));

        ProxyServer.getInstance().getScheduler().schedule(this, CustomReconnect::populateCache, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
