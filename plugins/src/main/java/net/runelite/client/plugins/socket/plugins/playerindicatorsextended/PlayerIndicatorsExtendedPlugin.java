package net.runelite.client.plugins.socket.plugins.playerindicatorsextended;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketPlayerJoin;
import net.runelite.client.plugins.socket.packet.SocketPlayerLeave;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Socket - Player Indicator", description = "Shows you players who are in your socket", tags = {"indicator, socket, player, highlight"})
public class PlayerIndicatorsExtendedPlugin extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(PlayerIndicatorsExtendedPlugin.class);

    @Inject
    private PlayerIndicatorsExtendedConfig config;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private PlayerIndicatorsExtendedOverlay overlay;

    @Inject
    private PlayerIndicatorsExtendedMinimapOverlay overlayMinimap;

    @Inject
    private ChatIconManager chatIconManager;

    private ArrayList<Actor> players;

    private ArrayList<String> names;

    @Provides
    PlayerIndicatorsExtendedConfig getConfig(ConfigManager configManager) {
        return (PlayerIndicatorsExtendedConfig)configManager.getConfig(PlayerIndicatorsExtendedConfig.class);
    }

    public ArrayList<Actor> getPlayers() {
        return this.players;
    }

    int activeTick = 0;

    boolean cleared = false;

    protected void startUp() {
        this.overlayManager.add(this.overlay);
        this.overlayManager.add(this.overlayMinimap);
        this.players = new ArrayList<>();
        this.names = new ArrayList<>();
    }

    protected void shutDown() {
        this.overlayManager.remove(this.overlay);
        this.overlayManager.remove(this.overlayMinimap);
    }

    @Subscribe
    public void onSocketPlayerJoin(SocketPlayerJoin event) {
        this.names.add(event.getPlayerName());
        if (event.getPlayerName().equals(Objects.requireNonNull(this.client.getLocalPlayer()).getName()))
            this.names.clear();
    }

    @Subscribe
    public void onSocketReceivePacket(SocketReceivePacket packet) {
        JSONObject data = packet.getPayload();
        if (!data.has("player-stats"))
            return;
        String localName = this.client.getLocalPlayer().getName();
        String targetName = data.getString("name");
        if (!targetName.equals(localName))
            if (!this.names.contains(targetName))
                this.names.add(targetName);
    }

    @Subscribe
    public void onSocketPlayerLeave(SocketPlayerLeave event) {
        this.names.remove(event.getPlayerName());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Plugin pl = this.pluginManager.getPlugins().stream().filter(x -> x.getName().equals("Socket")).findFirst().get();
        if (!this.pluginManager.isPluginEnabled(pl))
            this.names.clear();
        this.players.clear();
        for (Player p : this.client.getPlayers()) {
            if (this.names.contains(p.getName()))
                this.players.add(p);
        }
    }
}
