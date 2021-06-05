package net.runelite.client.plugins.socket.plugins.playerindicatorsextended;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class PlayerIndicatorsExtendedOverlay extends Overlay {
    private final Client client;

    private final PlayerIndicatorsExtendedPlugin plugin;

    private final PlayerIndicatorsExtendedConfig config;

    private final ChatIconManager chatIconManager;

    @Inject
    private PlayerIndicatorsExtendedOverlay(Client client, PlayerIndicatorsExtendedPlugin plugin, PlayerIndicatorsExtendedConfig config, ChatIconManager chatIconManager) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.chatIconManager = chatIconManager;
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
    }

    public Dimension render(Graphics2D graphics) {
        for (Actor actor : this.plugin.getPlayers()) {
            int zOffset = actor.getLogicalHeight() + 40;
            String name = Text.sanitize(actor.getName());
            Point textLocation = actor.getCanvasTextLocation(graphics, name, zOffset);
            if (actor instanceof Player) {
                Player player = (Player)actor;
                if (player.isFriendsChatMember()) {
                    FriendsChatRank rank = this.getFriendsChatRank(player);
                    if (rank != FriendsChatRank.UNRANKED) {
                        BufferedImage rankImage = this.chatIconManager.getRankImage(rank);
                        if (rankImage != null) {
                            int imageWidth = rankImage.getWidth();
                            int imageTextMargin = imageWidth / 2;
                            textLocation = new Point(textLocation.getX() + imageTextMargin, textLocation.getY());
                        }
                    }
                }
            }
            OverlayUtil.renderTextLocation(graphics, textLocation, name, this.config.nameColor());
        }
        return null;
    }

    FriendsChatRank getFriendsChatRank(Player player)
    {
        final FriendsChatManager friendsChatManager = client.getFriendsChatManager();
        if (friendsChatManager == null)
        {
            return FriendsChatRank.UNRANKED;
        }

        FriendsChatMember friendsChatMember = friendsChatManager.findByName(player.getName());
        return friendsChatMember != null ? friendsChatMember.getRank() : FriendsChatRank.UNRANKED;
    }
}
