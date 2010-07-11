/*  Copyright 2010 Ben Ruijl, Wouter Smeenk

This file is part of Walled In.

Walled In is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3, or (at your option)
any later version.

Walled In is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with Walled In; see the file LICENSE.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

 */
package walledin.game.gui;

import java.awt.event.KeyEvent;

import walledin.engine.Input;
import walledin.engine.Renderer;
import walledin.engine.math.Rectangle;
import walledin.engine.math.Vector2f;
import walledin.game.gui.ScreenManager.ScreenType;
import walledin.game.gui.components.ServerList;

public class ServerListScreen extends Screen {
    Screen serverListWidget;

    public ServerListScreen() {
        super(null, null);
    }

    @Override
    public void initialize() {
        serverListWidget = new ServerList(this, new Rectangle(0, 0, 500, 400));
        serverListWidget.setPosition(new Vector2f(100, 0));
        addChild(serverListWidget);
        serverListWidget.initialize(); // initialize after add!

    }

    @Override
    public void update(final double delta) {
        if (Input.getInstance().isKeyDown(KeyEvent.VK_ESCAPE)) {
            Input.getInstance().setKeyUp(KeyEvent.VK_ESCAPE);

            /*
             * If playing a game, return to it when pressing escape. Otherwise,
             * return to main menu.
             */
            if (getManager().getClient().connectedToServer()) {
                getManager().getScreen(ScreenType.GAME).show();
            } else {
                getManager().getScreen(ScreenType.MAIN_MENU).show();
            }

            hide();
        }

        super.update(delta);
    }

    @Override
    public void draw(final Renderer renderer) {
        super.draw(renderer);
    }

}