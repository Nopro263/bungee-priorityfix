package at.nopro.testproxy;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomReconnect implements ReconnectHandler {
    private static ServerInfo getInfo(String name) {
        return ProxyServer.getInstance().getConfig().getServers().get(name);
    }

    private static Map<String, ServerInfoEx> serverInfoExMap = new HashMap<>();
    private static List<String> serverPriority = new ArrayList<>();

    public static void populateCache() {
        List<String> sp = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        for(ListenerInfo l : ProxyServer.getInstance().getConfig().getListeners()) {
            for (String server : l.getServerPriority()) {
                ServerInfo serverInfo = getInfo(server);
                if(serverInfo != null) {
                    count.addAndGet(1);
                    serverInfo.ping((serverPing, throwable) -> {
                        if(serverPing != null) {
                            serverInfoExMap.put(server, new ServerInfoEx(serverInfo, serverPing));
                            if(!serverPriority.contains(server)) {
                                ProxyServer.getInstance().getLogger().info("Server '" + server + "' came online with Protocol version: " + serverPing.getVersion().getName());
                            }
                        } else {
                            sp.remove(server);
                            if(serverPriority.contains(server)) {
                                ProxyServer.getInstance().getLogger().warning("Server '" + server + "' went offline");
                            }
                        }
                        int newVar = count.addAndGet(-1);
                        if(newVar == 0) {
                            serverPriority = sp;
                        }
                    });
                    sp.add(server);
                }
            }
        }
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer proxiedPlayer) {
        for(String server : serverPriority) {
            ServerInfoEx serverInfoEx = serverInfoExMap.get(server);
            if(serverInfoEx == null) {
                ProxyServer.getInstance().getLogger().severe("Server " + server + "has no info");
                continue;
            }

            //ProxyServer.getInstance().getLogger().warning("Protocol: " + proxiedPlayer.getPendingConnection().getVersion());

            if(serverInfoEx.getServerPing().getVersion().getProtocol() == proxiedPlayer.getPendingConnection().getVersion()) {
                return serverInfoEx.getServerInfo();
            }
        }
        return null;
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {

    }

    @Override
    public void save() {

    }

    @Override
    public void close() {

    }

    private static class ServerInfoEx {
        private final ServerInfo serverInfo;
        private final ServerPing serverPing;

        public ServerInfoEx(ServerInfo serverInfo, ServerPing serverPing) {
            this.serverInfo = serverInfo;
            this.serverPing = serverPing;
        }

        public ServerInfo getServerInfo() {
            return serverInfo;
        }

        public ServerPing getServerPing() {
            return serverPing;
        }
    }
}
