/*
 * This file is part of FastBoard, licensed under the MIT License.
 *
 * Copyright (c) 2019-2023 MrMicky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package fr.mrmicky.fastboard.adventure;

import fr.mrmicky.fastboard.FastBoardBase;
import fr.mrmicky.fastboard.FastReflection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * {@inheritDoc}
 */
public class FastBoard extends FastBoardBase<Component> {

    private static final MethodHandle COMPONENT_METHOD;
    private static final Object EMPTY_COMPONENT;
    private static final boolean ADVENTURE_SUPPORT;

    static {
        ADVENTURE_SUPPORT = FastReflection
                .optionalClass("io.papermc.paper.adventure.PaperAdventure")
                .isPresent();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            if (ADVENTURE_SUPPORT) {
                Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
                Method method = paperAdventure.getDeclaredMethod("asVanilla", Component.class);
                COMPONENT_METHOD = lookup.unreflect(method);
                EMPTY_COMPONENT = COMPONENT_METHOD.invoke(Component.empty());
            } else {
                Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
                COMPONENT_METHOD = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
                EMPTY_COMPONENT = Array.get(COMPONENT_METHOD.invoke(""), 0);
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public FastBoard(Player player) {
        super(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void sendLineChange(int score) throws Throwable {
        Component line = getLineByScore(score);

        sendTeamPacket(score, FastBoardBase.TeamMode.UPDATE, line, null);
    }

    @Override
    protected Object toMinecraftComponent(Component component) throws Throwable {
        if (component == null) {
            return EMPTY_COMPONENT;
        }

        // If the server isn't running adventure natively, we convert the component to legacy text
        // and then to a Minecraft chat component
        if (!ADVENTURE_SUPPORT) {
            String legacy = serializeLine(component);

            return Array.get(COMPONENT_METHOD.invoke(legacy), 0);
        }

        return COMPONENT_METHOD.invoke(component);
    }

    @Override
    protected String serializeLine(Component value) {
        return LegacyComponentSerializer.legacySection().serialize(value);
    }

    @Override
    protected Component emptyLine() {
        return Component.empty();
    }
}
