package net.runelite.client.plugins.socket.plugins.deathindicators;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ClientInterface;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.socket.org.json.JSONArray;
import net.runelite.client.plugins.socket.org.json.JSONObject;
import net.runelite.client.plugins.socket.packet.SocketBroadcastPacket;
import net.runelite.client.plugins.socket.packet.SocketReceivePacket;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static net.runelite.api.ScriptID.XPDROPS_SETDROPSIZE;
import static net.runelite.api.widgets.WidgetInfo.TO_CHILD;
import static net.runelite.api.widgets.WidgetInfo.TO_GROUP;

@Slf4j

@PluginDescriptor(
        name = "Socket - Death Indicators",
        description = "Shows you NPCs that have been killed",
        tags = {"Socket, death, kill"},
        enabledByDefault = false
)
public class DeathIndicatorsPlugin extends Plugin
{
    @Inject
    private DeathIndicatorsConfig config;

    @Inject
    ConfigManager configManager;

    @Inject
    PluginManager pluginManager;

    @Inject
    private DeathIndicatorsOverlay overlay;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EventBus eventBus;

    private ArrayList<NyloQ> nylos;

    private ArrayList<Method> reflectedMethods;
    private ArrayList<Plugin> reflectedPlugins;

    @Getter
    private ArrayList<NPC> deadNylos;

    @Getter
    private NyloQ maidenNPC;

    private int partySize;

    @Provides
    DeathIndicatorsConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DeathIndicatorsConfig.class);
    }

    @Override
    protected void startUp()
    {
        deadNylos = new ArrayList<NPC>();
        this.overlayManager.add(overlay);
        nylos = new ArrayList<NyloQ>();
        reflectedMethods = new ArrayList<>();
        reflectedPlugins = new ArrayList<>();
        for(Plugin p : pluginManager.getPlugins())
        {
            Method m = null;
            try
            {
                m = p.getClass().getDeclaredMethod("SocketDeathIntegration", int.class);
            }
            catch(NoSuchMethodException e)
            {
                continue;
            }
            reflectedMethods.add(m);
            reflectedPlugins.add(p);
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        int smSmallHP = -1;
        int smBigHP = -1;
        int bigHP = -1;
        int smallHP = -1;
        int maidenHP = -1;
        if(partySize == 1)
        {
            bigHP = 16;
            smallHP = 8;
            maidenHP = 2625;
            smSmallHP = 2;
            smBigHP = 3;
        }
        else if(partySize == 2)
        {
            bigHP = 16;
            smallHP = 8;
            maidenHP = 2625;
            smSmallHP = 4;
            smBigHP = 6;
        }
        else if(partySize == 3)
        {
            bigHP = 16;
            smallHP = 8;
            maidenHP = 2625;
            smSmallHP = 6;
            smBigHP = 9;
        }
        else if(partySize == 4)
        {
            bigHP = 19;
            smallHP = 9;
            maidenHP = 3062;
            smSmallHP = 8;
            smBigHP = 12;
        }
        else if(partySize == 5)
        {
            bigHP = 22;
            smallHP = 11;
            maidenHP = 3500;
            smSmallHP = 10;
            smBigHP = 15;
        }
        int id = event.getNpc().getId();
        switch (id)
        {
            case 8342:
            case 10791://melee
            case 8343:
            case 10792://range
            case 8344:
            case 10793://mage
                nylos.add(new NyloQ(event.getNpc(), 0, smallHP));
                break;
            case 10775:
            case 10774:
            case 10776:
                nylos.add(new NyloQ(event.getNpc(), 0, smSmallHP));
                break;
            case 10777:
            case 10778:
            case 10779:
                nylos.add(new NyloQ(event.getNpc(), 0, smBigHP));
                break;
            case 8345:
            case 10794://melee
            case 8346:
            case 10795://range
            case 8347:
            case 10796://mage
            case 8351:
            case 10783:
            case 10800://melee aggro
            case 8352:
            case 10784:
            case 10801://range aggro
            case 8353:
            case 10785:
            case 10802://mage aggro
                nylos.add(new NyloQ(event.getNpc(), 0, bigHP));
                break;
            case 8360:
                NyloQ maidenTemp = new NyloQ(event.getNpc(), 0, maidenHP);
                nylos.add(maidenTemp);
                maidenNPC = maidenTemp;
        }

    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if(nylos.size() != 0)
        {
            nylos.removeIf(q -> q.npc.equals(event.getNpc()));
        }
        if(deadNylos.size() != 0)
        {
            deadNylos.removeIf(q->q.equals(event.getNpc()));
        }
    }


    @Subscribe
    public void onScriptPreFired(ScriptPreFired scriptPreFired)
    {
        if(!inNylo) return;
        if (scriptPreFired.getScriptId() == XPDROPS_SETDROPSIZE)
        {
            final int[] intStack = client.getIntStack();
            final int intStackSize = client.getIntStackSize();
            final int widgetId = intStack[intStackSize - 4];
            try
            {
                processXpDrop(widgetId);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private boolean inRegion(int... regions)
    {
        if (client.getMapRegions() != null)
        {
            for (int i : client.getMapRegions())
            {
                for (int j : regions)
                {
                    if (i == j)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void postHit(int index, int dmg)
    {
        JSONArray data = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("index", index);
        message.put("damage", dmg);
        data.put(message);
        JSONObject send = new JSONObject();
        send.put("sDeath", data);
        eventBus.post(new SocketBroadcastPacket(send));
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
        if(!inNylo) return;
        {
            for(NyloQ q : nylos)
            {
                if(hitsplatApplied.getActor().equals(q.npc))
                {
                    if(hitsplatApplied.getHitsplat().getHitsplatType().equals(Hitsplat.HitsplatType.HEAL))
                    {
                        q.hp+= hitsplatApplied.getHitsplat().getAmount();
                    }
                    else
                    {
                        q.hp -= hitsplatApplied.getHitsplat().getAmount();
                    }
                    q.queuedDamage -= hitsplatApplied.getHitsplat().getAmount();
                    if (q.hp <= 0)
                    {
                        deadNylos.removeIf(o -> o.equals(q.npc));
                    }
                    else if(q.npc.getId() == 8360 || q.npc.getId() == 8361 || q.npc.getId() == 8362 || q.npc.getId() == 8363)
                    {
                        double percent = ((double)q.hp)/((double)q.maxHP);
                        if(percent < .7)
                        {
                            q.phase = 1;
                        }
                        if(percent < .5)
                        {
                            q.phase = 2;
                        }
                        if(percent < .3)
                        {
                            q.phase = 3;
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onSocketReceivePacket(SocketReceivePacket event) {
        if(!inNylo) return;
        try
        {
            JSONObject payload = event.getPayload();
            if (!payload.has("sDeath"))
                return;

            JSONArray data = payload.getJSONArray("sDeath");
            JSONObject jsonmsg = data.getJSONObject(0);
            int index = jsonmsg.getInt("index");
            int damage = jsonmsg.getInt("damage");
            for(NyloQ q : nylos)
            {
                if(q.npc.getIndex() == index)
                {
                    q.queuedDamage+=damage;
                    if(q.hp-q.queuedDamage <= 0)
                    {
                        if(deadNylos.stream().noneMatch(o->o.getIndex()==q.npc.getIndex()))
                        {
                            deadNylos.add(q.npc);
                            if(config.hideNylo())
                            {

                                ClientInterface.setHidden(q.npc, true);
                                q.hidden = true;
                                if(reflectedPlugins.size() == reflectedMethods.size())
                                {
                                    for(int i = 0; i < reflectedPlugins.size(); i++)
                                    {
                                        try
                                        {
                                            Method tm = reflectedMethods.get(i);
                                            tm.setAccessible(true);
                                            tm.invoke(reflectedPlugins.get(i), q.npc.getIndex());
                                        }
                                        catch (NullPointerException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ExceptionInInitializerError e)
                                        {
                                            log.debug("Failed on plugin: " + reflectedPlugins.get(i).getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(q.npc.getId() == 8360 || q.npc.getId() == 8361 || q.npc.getId() == 8362 || q.npc.getId() == 8363)
                    {
                        double percent = ((double)q.hp-q.queuedDamage)/((double)q.maxHP);
                        if(percent < .7)
                        {
                            q.phase = 1;
                        }
                        if(percent < .5)
                        {
                            q.phase = 2;
                        }
                        if(percent < .3)
                        {
                            q.phase = 3;
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void addToDamageQueue(int damage)
    {
        if(damage == -1)
        {
            return;
        }
        Actor interacted = Objects.requireNonNull(client.getLocalPlayer()).getInteracting();
        NPC interactedNPC;
        if(interacted instanceof NPC)
        {
            interactedNPC = (NPC) interacted;
            postHit(interactedNPC.getIndex(), damage);
        }
    }

    private void processXpDrop(int widgetId) throws InterruptedException
    {
        final Widget xpdrop = client.getWidget(TO_GROUP(widgetId), TO_CHILD(widgetId));
        if(xpdrop == null) return;
        final Widget[] children = xpdrop.getChildren();
        final Widget textWidget = children[0];
        String text = textWidget.getText();
        boolean isDamage = false;
        Optional<Plugin> o = pluginManager.getPlugins().stream().filter(p->p.getName().equals("damagedrops")).findAny();
        if(o.isPresent())
        {
            if(pluginManager.isPluginEnabled(o.get()))
            {
                isDamage = (configManager.getConfiguration("damagedrops", "replaceEXPDrop").equals("true"));
            }
        }
        if(text.contains("<"))
        {
            if(text.contains("<img=11>"))
            {
                text = text.substring(9);
            }
            if(text.contains("<"))
            {
                text = text.substring(0, text.indexOf("<"));
            }
        }
        int damage = -1;
        int weaponUsed = Objects.requireNonNull(client.getLocalPlayer()).getPlayerComposition().getEquipmentId(KitType.WEAPON);
        if(client.getLocalPlayer().getAnimation() == 1979) //Barrage
        {
            return;
        }
        if(Arrays.stream(children).skip(1).filter(Objects::nonNull).mapToInt(Widget::getSpriteId).anyMatch(id->id == SpriteID.SKILL_MAGIC))
        {
            if(weaponUsed == 22323 || weaponUsed == 11905 || weaponUsed == 11907 || weaponUsed == 12899 || weaponUsed == 22292 || weaponUsed == 25731)
            {
                if(client.getVarbitValue(4696) == 0)
                {
                    if (client.getVar(VarPlayer.ATTACK_STYLE) != 3)
                    {
                        if(isDamage)
                        {
                            damage = Integer.parseInt(text);
                        }
                        else
                        {
                            damage = (int) (Integer.parseInt(text) / 2.0);
                        }
                    }
                }
                else
                {
                    if(client.getVar(VarPlayer.ATTACK_STYLE) == 3)
                    {
                        if(isDamage)
                        {
                            damage = (int) (Integer.parseInt(text));
                        }
                        else
                        {
                            damage = (int) Math.round(Integer.parseInt(text) / 3.6667);
                        }
                    }
                    else
                    {
                        if(isDamage)
                        {
                            damage = (int) (Integer.parseInt(text));
                        }
                        else
                        {
                            damage = (int) Math.round(Integer.parseInt(text) / 3.3334);
                        }
                    }
                }
            }
        }
        else if(Arrays.stream(children).skip(1).filter(Objects::nonNull).mapToInt(Widget::getSpriteId).anyMatch(id->id== SpriteID.SKILL_ATTACK || id == SpriteID.SKILL_STRENGTH || id == SpriteID.SKILL_DEFENCE))
        {
            if(weaponUsed == 22325 || weaponUsed == 25739 || weaponUsed == 25736) //Don't apply if weapon is scythe
            {
                return;
            }
            if(client.getVarbitValue(4696) == 0) //Separate XP Drops
            {
                if(weaponUsed == 22323 || weaponUsed == 11905 || weaponUsed == 11907 || weaponUsed == 12899 || weaponUsed == 22292 || weaponUsed == 25731) //Powered Staves
                {
                    if(client.getLocalPlayer().getAnimation() == 1979) //Barrage
                    {
                        return;
                    }
                    if(client.getVarbitValue(4696) == 0) //If separate xp drops, and weapon is mage,
                    {
                        if (client.getVar(VarPlayer.ATTACK_STYLE) == 3)
                        {
                            if(isDamage)
                            {
                                damage = (int) (Integer.parseInt(text));
                            }
                            else
                            {
                                damage = Integer.parseInt(text);
                            }
                        }
                    }
                }
                else if (weaponUsed == 12006)
                {
                    if (client.getVar(VarPlayer.ATTACK_STYLE) == 1)
                    {
                        if (Arrays.stream(children).skip(1).filter(Objects::nonNull).mapToInt(Widget::getSpriteId).anyMatch(id -> id == SpriteID.SKILL_ATTACK))
                        {
                            if(isDamage)
                            {
                                damage = (int) (Integer.parseInt(text));
                            }
                            else
                            {
                                damage = (int) Math.round(3.0 * Integer.parseInt(text) / 4.0);
                            }
                        }
                    }
                    else
                    {
                        if(isDamage)
                        {
                            damage = (int) (Integer.parseInt(text));
                        }
                        else
                        {
                            damage = Integer.parseInt(text) / 4;
                        }
                    }
                } else
                {
                    if(isDamage)
                    {
                        damage = (int) (Integer.parseInt(text));
                    }
                    else
                    {
                        damage = Integer.parseInt(text) / 4;
                    }
                }
            }
            else
            {
                if(isDamage)
                {
                    damage = (int) (Integer.parseInt(text));
                }
                else
                {
                    damage = (int) Math.round(Integer.parseInt(text) / 5.3333);
                }
            }
        }
        else if(Arrays.stream(children).skip(1).filter(Objects::nonNull).mapToInt(Widget::getSpriteId).anyMatch(id->id == SpriteID.SKILL_RANGED))
        {
            if(weaponUsed == 11959)
            {
                return;
            }
            if(client.getVarbitValue(4696) == 0)
            {
                if(isDamage)
                {
                    damage = (int) (Integer.parseInt(text));
                }
                else
                {
                    damage = (int) (Integer.parseInt(text) / 4.0);
                }
            }
            else
            {
                if(isDamage)
                {
                    damage = (int) (Integer.parseInt(text));
                }
                else
                {
                    damage = (int) Math.round(Integer.parseInt(text) / 5.333);
                }
            }
        }
        addToDamageQueue(damage);
    }

    private boolean inNylo = false;

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if(!inNylo)
        {
            if(inRegion(13122))
            {
                inNylo = true;
                partySize = 0;
                for(int i = 330; i < 335; i++)
                {
                    if(client.getVarcStrValue(i) != null && !client.getVarcStrValue(i).equals(""))
                    {
                        partySize++;
                    }
                }
            }
        }
        else
        {
            if(!inRegion(13122))
            {
                inNylo = false;
            }
        }

        for(NyloQ q : nylos)
        {
            if(q.hidden)
            {
                q.hiddenTicks++;
                if(q.npc.getHealthRatio() != 0 && q.hiddenTicks > 5)
                {
                    q.hiddenTicks = 0;
                    q.hidden = false;
                    ClientInterface.setHidden(q.npc, false);
                    deadNylos.removeIf(x->x.equals(q.npc));
                }
            }
        }

    }
}
