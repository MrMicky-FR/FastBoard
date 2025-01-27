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
package fr.mrmicky.fastboard;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * {@inheritDoc}
 */
public class FastBoard extends FastBoardBase<String> {

    private static final MethodHandle MESSAGE_FROM_STRING;
    private static final Object EMPTY_MESSAGE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
            MESSAGE_FROM_STRING = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
            EMPTY_MESSAGE = Array.get(MESSAGE_FROM_STRING.invoke(""), 0);
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
    public void updateTitle(String title) {
        Objects.requireNonNull(title, "title");

        if (!VersionType.V1_13.isHigherOrEqual() && title.length() > 32) {
            throw new IllegalArgumentException("Title is longer than 32 chars");
        }

        super.updateTitle(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLines(String... lines) {
        Objects.requireNonNull(lines, "lines");

        if (!VersionType.V1_13.isHigherOrEqual()) {
            int lineCount = 0;
            for (String s : lines) {
                if (s != null && s.length() > 30) {
                    throw new IllegalArgumentException("Line " + lineCount + " is longer than 30 chars");
                }
                lineCount++;
            }
        }

        super.updateLines(lines);
    }

    @Override
    protected void sendLineChange(int score) throws Throwable {
        int maxLength = hasLinesMaxLength() ? 16 : 1024;
        String line = getLineByScore(score);
        String prefix;
        String suffix = "";

        if (line == null || line.isEmpty()) {
            prefix = COLOR_CODES[score] + ChatColor.RESET;
        } else if (line.length() <= maxLength) {
            prefix = line;
        } else {
            // Prevent splitting color codes
            int index = line.charAt(maxLength - 1) == ChatColor.COLOR_CHAR
                    ? (maxLength - 1) : maxLength;
            prefix = line.substring(0, index);
            String suffixTmp = line.substring(index);
            ChatColor chatColor = null;

            if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
                chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
            }

            String color = ChatColor.getLastColors(prefix);
            boolean addColor = chatColor == null || chatColor.isFormat();

            suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET.toString() : color) : "") + suffixTmp;
        }

        if (prefix.length() > maxLength || suffix.length() > maxLength) {
            // Something went wrong, just cut to prevent client crash/kick
            prefix = prefix.substring(0, Math.min(maxLength, prefix.length()));
            suffix = suffix.substring(0, Math.min(maxLength, suffix.length()));
        }

        sendTeamPacket(score, TeamMode.UPDATE, prefix, suffix);
    }

    @Override
    protected Object toMinecraftComponent(String line) throws Throwable {
        if (line == null || line.isEmpty()) {
            return EMPTY_MESSAGE;
        }

        return Array.get(MESSAGE_FROM_STRING.invoke(line), 0);
    }

    @Override
    protected String serializeLine(String value) {
        return value;
    }

    @Override
    protected String emptyLine() {
        return "";
    }

    /**
     * Return if the player has a prefix/suffix characters limit.
     * By default, it returns true only in 1.12 or lower.
     * This method can be overridden to fix compatibility with some versions support plugin.
     *
     * @return max length
     */
    protected boolean hasLinesMaxLength() {
        return !VersionType.V1_13.isHigherOrEqual();
    }
}
