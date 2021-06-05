package net.runelite.client.plugins.socket.plugins.socketdefence;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.ToIntFunction;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.socket.org.json.JSONArray;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketMembersUpdate;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.plugins.socket.packet.SocketShutdown;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
        name = "Socket - Defence",
        description = "Shows defence level for different bosses after specs",
        tags = {"socket", "pvm", "cox", "gwd", "corp", "tob"}
        )
public class SocketDefencePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private SkillIconManager skillIconManager;

    @Inject
    private InfoBoxManager infoBoxManager;

    @Inject
    private SocketDefenceConfig config;

    @Inject
    private SocketDefenceOverlay overlay;

    public ArrayList<String> socketPlayerNames = new ArrayList<>();
    public String boss = "";
    public double bossDef = 0;
    public String specWep = "";
    public DefenceInfoBox box = null;
    public BufferedImage img = null;
    public boolean isInCm = false;
    public ArrayList<String> bossList = new ArrayList<>(Arrays.asList("Corporeal Beast", "General Graardor", "K'ril Tsutsaroth", "Kalphite Queen", "The Maiden of Sugadinti",
            "Xarpus", "Great Olm (Left claw)", "Tekton", "Tekton (enraged)"));

    public SocketDefencePlugin() {
    }

    protected void startUp() throws Exception {
        reset();
        this.overlayManager.add(overlay);
    }

    protected void shutDown() throws Exception {
        reset();
        this.overlayManager.remove(overlay);
    }

    protected void reset() {
        infoBoxManager.removeInfoBox(box);
        socketPlayerNames.clear();
        boss = "";
        bossDef = -1;
        specWep = "";
        box = null;
        img = null;
        isInCm = false;
    }

    @Provides
    SocketDefenceConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(SocketDefenceConfig.class);
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (event.getActor() != null && this.client.getLocalPlayer() != null && event.getActor().getName() != null) {
            String actorName = event.getActor().getName();
            int animation = event.getActor().getAnimation();

            if (actorName.equals(this.client.getLocalPlayer().getName())) {
                if (animation == 1378 || animation == 7642 || event.getActor().getAnimation() == 7643 || animation == 2890) {
                    if(bossList.contains(event.getActor().getInteracting().getName())) {
                        boss = event.getActor().getInteracting().getName();

                        if (event.getActor().getAnimation() == 1378) {
                            specWep = "dwh";
                        } else if (event.getActor().getAnimation() == 7642 || event.getActor().getAnimation() == 7643) {
                            specWep = "bgs";
                        } else if (event.getActor().getAnimation() == 2890) {
                            specWep = "arclight";
                        }
                    }
                } else {
                    specWep = "";
                }
            }
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        if (!boss.equals("") && !specWep.equals("")) {
            if (event.getActor().getName().equals(boss) || (event.getActor().getName().contains("Tekton") && boss.contains("Tekton"))) {
                if (event.getHitsplat().getAmount() >= 0 && event.getHitsplat().isMine()) {
                    JSONObject data = new JSONObject();
                    data.put("boss", boss);
                    data.put("weapon", specWep);
                    data.put("hit", event.getHitsplat().getAmount());
                    JSONObject payload = new JSONObject();
                    payload.put("socketdefence", data);
                    this.eventBus.post(new SocketBroadcastPacket(payload));
                }
            }
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if(event.getNpc().getName() != null) {
            if ((event.getNpc().getName().equals(boss) || (event.getActor().getName().contains("Tekton") && boss.contains("Tekton"))) && event.getNpc().isDead()) {
                JSONObject data = new JSONObject();
                data.put("boss", boss);
                JSONObject payload = new JSONObject();
                payload.put("socketdefencebossdead", data);
                this.eventBus.post(new SocketBroadcastPacket(payload));
                boss = "";
                bossDef = -1;
                specWep = "";
                infoBoxManager.removeInfoBox(box);
            }
        }
    }

    @Subscribe
    public void onSocketReceivePacket(SocketReceivePacket event) {
        try {
            JSONObject payload = event.getPayload();
            if (payload.has("socketdefence")) {
                JSONObject data = payload.getJSONObject("socketdefence");
                String bossName = data.getString("boss");
                String weapon = data.getString("weapon");
                int hit = data.getInt("hit");

                if(((bossName.contains("Tekton") || bossName.contains("Great Olm")) && this.client.getVar(Varbits.IN_RAID) != 1) ||
                    ((bossName.contains("The Maiden of Sugadinti") || bossName.contains("Xarpus")) && this.client.getVar(Varbits.THEATRE_OF_BLOOD) != 2)){
                    return;
                }

                if (boss.equals("") || bossDef == -1 || !boss.equals(bossName)) {
                    if (bossName.equals("Corporeal Beast")) {
                        bossDef = 310;
                    } else if (bossName.equals("General Graardor")) {
                        bossDef = 250;
                    } else if (bossName.equals("K'ril Tsutsaroth")) {
                        bossDef = 270;
                    } else if (bossName.equals("Kalphite Queen")) {
                        bossDef = 300;
                    } else if (bossName.equals("The Maiden of Sugadinti")) {
                        bossDef = 200;
                    } else if (bossName.equals("Xarpus")) {
                        bossDef = 250;
                    } else if (bossName.equals("Great Olm (Left claw)")) {
                        bossDef = 175 * (1 + (.01 * (this.client.getVarbitValue(5424) - 1)));

                        if (isInCm) {
                            bossDef = bossDef * 1.5;
                        }
                    } else if (bossName.contains("Tekton")) {
                        bossDef = 205 * (1 + (.01 * (this.client.getVarbitValue(5424) - 1)));

                        if (isInCm) {
                            bossDef = bossDef * 1.2;
                        }
                    }
                    boss = bossName;
                }

                if(bossDef != -1 && !boss.equals("") || (!boss.equals(bossName) && !boss.contains("Tekton") && !bossName.contains("Tekton"))) {
                    if (weapon.equals("dwh") && hit == 0) {
                        if (client.getVar(Varbits.IN_RAID) == 1 && (boss.contains("Tekton"))) {
                            bossDef = bossDef - (bossDef * .05);
                        }
                    } else if (weapon.equals("dwh") && hit > 0) {
                        bossDef = bossDef - (bossDef * .30);
                    } else if (weapon.equals("bgs")) {
                        if (boss.equals("Corporeal Beast")) {
                            bossDef = bossDef - (hit * 2);
                        } else {
                            bossDef = bossDef - hit;
                        }
                    } else if (weapon.equals("arclight") && hit > 0) {
                        bossDef = bossDef - (bossDef * .05);
                    } else if (weapon.equals("vuln")){
                        bossDef = bossDef - (bossDef * .1);
                    }

                    if(bossDef < -1){
                        bossDef = 0;
                    }
                    infoBoxManager.removeInfoBox(box);
                    img = skillIconManager.getSkillImage(Skill.DEFENCE);
                    box = new DefenceInfoBox(img, this, Math.round(bossDef), config);
                    box.setTooltip(ColorUtil.wrapWithColorTag(boss, Color.WHITE));
                    infoBoxManager.addInfoBox(box);
                }
            } else if (payload.has("socketdefencebossdead")) {
                JSONObject data = payload.getJSONObject("socketdefencebossdead");
                String bossName = data.getString("boss");

                if (bossName.equals(boss) || (bossName.contains("Tekton") && boss.contains("Tekton"))) {
                    boss = "";
                    bossDef = -1;
                    specWep = "";
                    infoBoxManager.removeInfoBox(box);
                }
            } else if /*(payload.has("socketdefencecm"))*/(this.config.cm()) {
                isInCm = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe void onSocketMembersUpdate(SocketMembersUpdate event)
    {
        socketPlayerNames.clear();
        socketPlayerNames.addAll(event.getMembers());
    }

    @Subscribe
    private void onSocketShutdown(SocketShutdown event)
    {
        this.socketPlayerNames.clear();
    }

    @Subscribe
    private void onVarbitChanged(VarbitChanged event) {
        if (this.client.getVarbitValue(6385) != 0) {
            JSONObject data = new JSONObject();
            data.put("cm", boss.toLowerCase());
            JSONObject payload = new JSONObject();
            payload.put("socketdefencecm", data);
            this.eventBus.post(new SocketBroadcastPacket(payload));
        }
        if (client.getVar(Varbits.IN_RAID) != 1) {
            if (boss.toLowerCase().contains("tekton") || boss.toLowerCase().contains("great olm (left claw)")) {
                reset();
            }
        }

        if (boss.toLowerCase().contains("the maiden of sugadinti") && getInstanceRegionId() != TobRegions.MAIDEN.getRegionId()) {
            reset();
        }else if (boss.toLowerCase().contains("xarpus") && getInstanceRegionId() != TobRegions.XARPUS.getRegionId()) {
            reset();
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        //85 = splash
        if (event.getActor().getName() != null) {
            if (event.getActor().getGraphic() == 169) {
                specWep = "vuln";
                System.out.println("Hit vuln on " + event.getActor().getName());
                if (bossList.contains(event.getActor().getName())) {
                    boss = event.getActor().getName();
                    JSONObject data = new JSONObject();
                    data.put("boss", boss);
                    data.put("weapon", specWep);
                    data.put("hit", 0);
                    JSONObject payload = new JSONObject();
                    payload.put("socketdefence", data);
                    this.eventBus.post(new SocketBroadcastPacket(payload));
                }
            }
        }
    }


    public int getInstanceRegionId() {
        return WorldPoint.fromLocalInstance(this.client, this.client.getLocalPlayer().getLocalLocation()).getRegionID();
    }

    public enum TobRegions {
        MAIDEN(12613),
        BLOAT(13125),
        NYLOCAS(13122),
        SOTETSEG(13123),
        SOTETSEG_MAZE(13379),
        XARPUS(12612),
        VERZIK(12611);

        private final int regionId;

        TobRegions(int regionId) {
            this.regionId = regionId;
        }

        public int getRegionId() {
            return this.regionId;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e) {
        if (this.config.cm()) {
            isInCm = true;
        } else {
            isInCm = false;
        }
    }
}

