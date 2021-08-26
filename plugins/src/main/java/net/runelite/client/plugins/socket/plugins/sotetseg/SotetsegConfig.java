/*
 * Copyright (c) 2020, Charles Xu <github.com/kthisiscvpv>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.socket.plugins.sotetseg;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("Socket Sotetseg Config")
public interface SotetsegConfig extends Config {

      @ConfigItem(
              position = 0,
              keyName = "getTileColor",
              name = "Tile Color",
              description = "The color of the tiles."
      )
      default Color getTileColor() {
            return Color.GREEN;
      }

      @Range(min = 0, max = 255)
      @ConfigItem(
              position = 1,
              keyName = "getTileTransparency",
              name = "Tile Transparency",
              description = "The color transparency of the tiles. Ranges from 0 to 255, inclusive."
      )
      default int getTileTransparency() {
            return 50;
      }

      @ConfigItem(
              position = 2,
              keyName = "getTileOutline",
              name = "Tile Outline Color",
              description = "The color of the outline of the tiles."
      )
      default Color getTileOutline() {
            return Color.GREEN;
      }

      @Range(min = 0, max = 5)
      @ConfigItem(
              position = 3,
              keyName = "getTileOutlineSize",
              name = "Tile Outline Size",
              description = "The size of the outline of the tiles."
      )
      default int getTileOutlineSize() {
            return 1;
      }

      @ConfigItem(
              position = 4,
              keyName = "streamerMode",
              name = "Streamer Mode",
              description = "Send Maze Info to team but don't display maze overlay on your screen."
      )
      default boolean streamerMode() {
            return false;
      }

      @ConfigItem(
              position = 5,
              keyName = "testOverlay",
              name = "Show Test Tiles",
              description = "Shows test tiles to allow you to change your tile outline settings"
      )
      default boolean showTestOverlay()
      {
            return false;
      }

      @ConfigItem(
              position = 6,
              keyName = "warnBall",
              name = "Warns if invisible ball is sent",
              description = "Warns you if the ball was sent while you were chosen since it's invisible otherwise"
      )
      default boolean warnBall() { return true;}

      @ConfigItem(
              position = 7,
              keyName = "trueMaze",
              name = "Maze True Tile",
              description = "Shows your true tile location only when the maze is active"
      )
      default boolean trueMaze() { return true;}

      @ConfigItem(
              position = 8,
              keyName = "trueMazeColor",
              name = "Maze True Tile Color",
              description = "Color for the maze true tile"
      )
      default Color trueMazeColor() { return Color.RED;}

      @Range(min = 1, max = 5)
      @ConfigItem(
              position = 9,
              keyName = "trueMazeThicc",
              name = "Maze True Tile Width",
              description = "Width for the maze true location tile"
      )
      default int trueMazeThicc() { return 2;}

      @ConfigItem(
              position = 10,
              keyName = "antiAlias",
              name = "Maze True Tile Anti-Aliasing Off",
              description = "Turns off anti-aliasing for the tiles. Makes them more jagged."
      )
      default boolean antiAlias()
      {
            return false;
      }
}
