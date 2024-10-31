package at.nopro.testproxy;

import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CustomReconnect implements ReconnectHandler {
    private static ServerInfo getInfo(String name) {
        return ProxyServer.getInstance().getConfig().getServers().get(name);
    }

    private static final Map<String, ServerInfoEx> serverInfoExMap = new HashMap<>();
    private static List<String> serverPriority = new ArrayList<>();

    private ReconnectHandler handler;

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

    public CustomReconnect(ReconnectHandler originalHandler) {
        this.handler = originalHandler;
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer proxiedPlayer) {
        // Use saved data, if possible
        if(this.handler != null && !proxiedPlayer.getPendingConnection().getListener().isForceDefault()) {
            ServerInfo info = this.handler.getServer(proxiedPlayer);
            if(info != null) {
                ServerInfoEx serverInfoEx = serverInfoExMap.get(info.getName());
                if(serverInfoEx == null) {
                    ProxyServer.getInstance().getLogger().severe("Server " + info.getName() + "has no info");
                } else {
                    if(serverInfoEx.getServerPing().getVersion().getProtocol() == proxiedPlayer.getPendingConnection().getVersion()) {
                        return info;
                    }
                }
            }
        }

        if(AbstractReconnectHandler.getForcedHost(proxiedPlayer.getPendingConnection()) != null) {
            return AbstractReconnectHandler.getForcedHost(proxiedPlayer.getPendingConnection());
        }

        List<String> p = proxiedPlayer.getPendingConnection().getListener().getServerPriority();

        List<String> sp = serverPriority.stream().filter(p::contains).collect(Collectors.toList());


        for(String server : sp) {
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
        this.handler.setServer(proxiedPlayer);
    }

    @Override
    public void save() {
        this.handler.save();
    }

    @Override
    public void close() {
        this.handler.close();
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
