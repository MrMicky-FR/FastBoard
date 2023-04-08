/*
 * This file is part of FastBoard, licensed under the MIT License.
 *
 * Copyright (c) 2019-2021 MrMicky
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
import java.lang.reflect.Array;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * {@inheritDoc}
 */
public class FastBoard extends FastBoardBase<Component> {

    private static MethodHandle AS_VANILLA;
    private static Object EMPTY_COMPONENT;
    private static Object EMPTY_MESSAGE;
    private static boolean CONVERT_TO_LEGACY;
    private static MethodHandle MESSAGE_FROM_STRING;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method method = paperAdventure.getDeclaredMethod("asVanilla", Component.class);
            AS_VANILLA = lookup.unreflect(method);
            EMPTY_COMPONENT = AS_VANILLA.invoke(Component.empty());
            CONVERT_TO_LEGACY = false;
        } catch (Throwable t) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
                MESSAGE_FROM_STRING = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
                EMPTY_MESSAGE = Array.get(MESSAGE_FROM_STRING.invoke(""), 0);
                CONVERT_TO_LEGACY = true;
            } catch (Throwable t2) {
                throw new ExceptionInInitializerError(t);
            }
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
        // If the component is null, we return an empty component
        if(component == null) {
            return CONVERT_TO_LEGACY ? EMPTY_MESSAGE : EMPTY_COMPONENT;
        }

        // If the server isn't running adventure nativly, we convert the component to legacy text
        // and then to a minecraft chat component
        if(CONVERT_TO_LEGACY) {
            return Array.get(MESSAGE_FROM_STRING.invoke(LegacyComponentSerializer.legacySection().serialize(component)), 0);
        }

        // Server supports adventure natively
        return AS_VANILLA.invoke(component == null);
    }

    @Override
    protected Component emptyLine() {
        return Component.empty();
    }
}
