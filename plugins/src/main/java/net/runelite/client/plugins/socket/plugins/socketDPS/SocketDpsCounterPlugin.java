package net.runelite.client.plugins.socket.plugins.socketDPS;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.socket.SocketPlugin;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "<html><font color=#00ff00>[S] Socket - DPS", description = "Counts damage by a party", enabledByDefault = false)
@PluginDependency(SocketPlugin.class)
public class SocketDpsCounterPlugin extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(SocketDpsCounterPlugin.class);

    private static final Set<Integer> BOSSES = new HashSet<>();

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SocketDpsOverlay socketDpsOverlay;

    @Inject
    private SocketDpsConfig socketDpsConfig;

    @Inject
    private EventBus eventBus;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private SocketPlugin socketPlugin;

    @Inject
    private ClientThread clientThread;

    private Map<String, Integer> members = new ConcurrentHashMap<>();

    private List<String> highlights = new ArrayList<>();

    private List<String> danger = new ArrayList<>();

    public List<String> getDanger() {
        return this.danger;
    }

    @Provides
    SocketDpsConfig provideConfig(ConfigManager configManager) {
        return (SocketDpsConfig)configManager.getConfig(SocketDpsConfig.class);
    }

    protected void startUp() {
        this.members.clear();
        this.overlayManager.add((Overlay)this.socketDpsOverlay);
        this.clientThread.invoke(this::rebuildAllPlayers);
        BOSSES.addAll(Arrays.asList(5886, 5887, 5888, 5889, 5890, 5891, 5908, 6503, 6609, 5862,
                5863, 5866, 2054, 6505, 319, 2215, 6494, 5779, 6499, 128,
                963, 965, 4303, 4304, 6500, 6501, 239, 2642, 650, 3129,
                6495, 8713, 6504, 6610, 6611, 6612, 3106, 3108, 8360, 8361,
                8362, 8363, 8364, 8365, 8359, 8354, 8355, 8356, 8357, 8387,
                8388, 8340, 8341, 8370, 8372, 8374, 7540, 7541, 7542, 7543,
                7544, 7545, 7530, 7531, 7532, 7533, 7525, 7526, 7527, 7528,
                7529, 7551, 7552, 7553,7554, 7555, 7559, 7560, 7561, 7562,
                7563, 7566, 7567, 7569, 7570, 7571, 7572, 7573, 7574, 7584,
                7585, 7604, 7605, 7606, 9425, 9426, 9427, 9428, 9429, 9430,
                9431, 9432, 9433, 3162, 2205, 2265, 2266, 2267));
    }

    protected void shutDown() {
        this.overlayManager.remove((Overlay)this.socketDpsOverlay);
        this.members.clear();
    }

    @Subscribe
    void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals("socketdpscounter"))
            this.clientThread.invoke(this::rebuildAllPlayers);
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
            this.members.clear();
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Actor target = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.isMine() && target != this.client.getLocalPlayer() &&
                target instanceof NPC) {
            NPC npc = (NPC)target;
            int interactingId = npc.getId();
            if ((!this.socketDpsConfig.onlyBossDps() || BOSSES.contains(interactingId)) &&
                    hitsplat.getAmount() > 0) {
                int hit = hitsplat.getAmount();
                String pName = this.client.getLocalPlayer().getName();
                this.members.put(pName, (Integer) this.members.getOrDefault(pName, 0) + hit);
                this.members.put("total", (Integer) this.members.getOrDefault("total", 0) + hit);
                JSONObject data = new JSONObject();
                data.put("player", pName);
                data.put("target", interactingId);
                data.put("hit", hit);
                data.put("world", this.client.getWorld());
                JSONObject payload = new JSONObject();
                payload.put("dps-counter", data);
                this.eventBus.post(new SocketBroadcastPacket(payload));
                this.members = sortByValue(this.members);
            }
        }
    }

    @Subscribe
    public void onOverlayMenuClicked(OverlayMenuClicked event) {
        if (event.getEntry() == SocketDpsOverlay.RESET_ENTRY)
            this.members.clear();
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();
        if (npc.isDead() && BOSSES.contains(Integer.valueOf(npc.getId()))) {
            log.debug("Boss has died!");
            if (this.socketDpsConfig.autoclear())
                this.members.clear();
            if (this.socketDpsConfig.clearAnyBossKill()) {
                JSONObject data = new JSONObject();
                data.put("boss", npc.getId());
                data.put("world", this.client.getWorld());
                JSONObject payload = new JSONObject();
                payload.put("dps-clear", data);
                this.eventBus.post(new SocketBroadcastPacket(payload));
            }
        }
    }

    @Subscribe
    public void onSocketReceivePacket(SocketReceivePacket event) {
        try {
            if (this.client.getGameState() != GameState.LOGGED_IN)
                return;
            JSONObject payload = event.getPayload();
            if (!payload.has("dps-counter")) {
                if (payload.has("dps-clear"))
                    if (this.socketDpsConfig.onlySameWorld()) {
                        JSONObject jSONObject = payload.getJSONObject("dps-clear");
                        if (jSONObject.getInt("world") == this.client.getWorld())
                            this.members.clear();
                    } else {
                        this.members.clear();
                    }
                return;
            }
            String pName = this.client.getLocalPlayer().getName();
            JSONObject data = payload.getJSONObject("dps-counter");
            if (this.socketDpsConfig.onlySameWorld() && this.client.getWorld() != data.getInt("world"))
                return;
            if (data.getString("player").equals(pName))
                return;
            this.clientThread.invoke(() -> {
                String attacker = data.getString("player");
                int targetId = data.getInt("target");
                updateDpsMember(attacker, targetId, data.getInt("hit"));
            });
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    private void updateDpsMember(String attacker, int targetId, int hit) {
        if (BOSSES.contains(Integer.valueOf(targetId))) {
            this.members.put(attacker, Integer.valueOf(((Integer)this.members.getOrDefault(attacker, Integer.valueOf(0))).intValue() + hit));
            this.members.put("total", Integer.valueOf(((Integer)this.members.getOrDefault("total", Integer.valueOf(0))).intValue() + hit));
            this.members = sortByValue(this.members);
            updateDanger();
        }
    }

    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return (Map<K, V>)map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, java.util.LinkedHashMap::new));
    }

    public List<String> getHighlights() {
        String configplayers = this.socketDpsConfig.getPlayerToHighlight().toLowerCase();
        return configplayers.isEmpty() ? Collections.<String>emptyList() : SocketText.fromCSV(configplayers);
    }

    void rebuildAllPlayers() {
        this.highlights = getHighlights();
    }

    void updateDanger() {
        this.danger.clear();
        for (String mem1 : this.members.keySet()) {
            if (this.highlights.contains(mem1))
                for (String mem2 : this.members.keySet()) {
                    if (!mem2.equalsIgnoreCase(mem1))
                        if (((Integer)this.members.get(mem2)).intValue() - ((Integer)this.members.get(mem1)).intValue() <= 50)
                            this.danger.add(mem2);
                }
        }
    }

    public Map<String, Integer> getMembers() {
        return this.members;
    }
}
