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
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight packet-based scoreboard API for Bukkit plugins.
 * It can be safely used asynchronously as everything is at packet level.
 * <p>
 * The project is on <a href="https://github.com/MrMicky-FR/FastBoard">GitHub</a>.
 *
 * @author MrMicky
 * @version 2.0.0
 */
public abstract class FastBoardBase<T> {

    private static final Map<Class<?>, Field[]> PACKETS = new HashMap<>(8);
    protected static final String[] COLOR_CODES = Arrays.stream(ChatColor.values())
            .map(Object::toString)
            .toArray(String[]::new);
    private static final VersionType VERSION_TYPE;
    // Packets and components
    private static final Class<?> CHAT_COMPONENT_CLASS;
    private static final Class<?> CHAT_FORMAT_ENUM;
    private static final Object RESET_FORMATTING;
    private static final MethodHandle PLAYER_CONNECTION;
    private static final MethodHandle SEND_PACKET;
    private static final MethodHandle PLAYER_GET_HANDLE;
    // Scoreboard packets
    private static final FastReflection.PacketConstructor PACKET_SB_OBJ;
    private static final FastReflection.PacketConstructor PACKET_SB_DISPLAY_OBJ;
    private static final FastReflection.PacketConstructor PACKET_SB_SCORE;
    private static final FastReflection.PacketConstructor PACKET_SB_TEAM;
    private static final FastReflection.PacketConstructor PACKET_SB_SERIALIZABLE_TEAM;
    // Scoreboard enums
    private static final Class<?> ENUM_SB_HEALTH_DISPLAY;
    private static final Class<?> ENUM_SB_ACTION;
    private static final Object ENUM_SB_HEALTH_DISPLAY_INTEGER;
    private static final Object ENUM_SB_ACTION_CHANGE;
    private static final Object ENUM_SB_ACTION_REMOVE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            if (FastReflection.isRepackaged()) {
                VERSION_TYPE = VersionType.V1_17;
            } else if (FastReflection.nmsOptionalClass(null, "ScoreboardServer$Action").isPresent()) {
                VERSION_TYPE = VersionType.V1_13;
            } else if (FastReflection.nmsOptionalClass(null, "IScoreboardCriteria$EnumScoreboardHealthDisplay").isPresent()) {
                VERSION_TYPE = VersionType.V1_8;
            } else {
                VERSION_TYPE = VersionType.V1_7;
            }

            String gameProtocolPackage = "network.protocol.game";
            Class<?> craftPlayerClass = FastReflection.obcClass("entity.CraftPlayer");
            Class<?> entityPlayerClass = FastReflection.nmsClass("server.level", "EntityPlayer");
            Class<?> playerConnectionClass = FastReflection.nmsClass("server.network", "PlayerConnection");
            Class<?> packetClass = FastReflection.nmsClass("network.protocol", "Packet");
            Class<?> packetSbObjClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardObjective");
            Class<?> packetSbDisplayObjClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardDisplayObjective");
            Class<?> packetSbScoreClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardScore");
            Class<?> packetSbTeamClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardTeam");
            Class<?> sbTeamClass = VersionType.V1_17.isHigherOrEqual()
                    ? FastReflection.innerClass(packetSbTeamClass, innerClass -> !innerClass.isEnum()) : null;
            Field playerConnectionField = Arrays.stream(entityPlayerClass.getFields())
                    .filter(field -> field.getType().isAssignableFrom(playerConnectionClass))
                    .findFirst().orElseThrow(NoSuchFieldException::new);
            Method sendPacketMethod = Arrays.stream(playerConnectionClass.getMethods())
                    .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == packetClass)
                    .findFirst().orElseThrow(NoSuchMethodException::new);

            CHAT_COMPONENT_CLASS = FastReflection.nmsClass("network.chat", "IChatBaseComponent");
            CHAT_FORMAT_ENUM = FastReflection.nmsClass(null, "EnumChatFormat");
            RESET_FORMATTING = FastReflection.enumValueOf(CHAT_FORMAT_ENUM, "RESET", 21);
            PLAYER_GET_HANDLE = lookup.findVirtual(craftPlayerClass, "getHandle", MethodType.methodType(entityPlayerClass));
            PLAYER_CONNECTION = lookup.unreflectGetter(playerConnectionField);
            SEND_PACKET = lookup.unreflect(sendPacketMethod);
            PACKET_SB_OBJ = FastReflection.findPacketConstructor(packetSbObjClass, lookup);
            PACKET_SB_DISPLAY_OBJ = FastReflection.findPacketConstructor(packetSbDisplayObjClass, lookup);
            PACKET_SB_SCORE = FastReflection.findPacketConstructor(packetSbScoreClass, lookup);
            PACKET_SB_TEAM = FastReflection.findPacketConstructor(packetSbTeamClass, lookup);
            PACKET_SB_SERIALIZABLE_TEAM = sbTeamClass == null ? null : FastReflection.findPacketConstructor(sbTeamClass, lookup);

            for (Class<?> clazz : Arrays.asList(packetSbObjClass, packetSbDisplayObjClass, packetSbScoreClass, packetSbTeamClass, sbTeamClass)) {
                if (clazz == null) {
                    continue;
                }
                Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .toArray(Field[]::new);
                for (Field field : fields) {
                    field.setAccessible(true);
                }
                PACKETS.put(clazz, fields);
            }

            if (VersionType.V1_8.isHigherOrEqual()) {
                String enumSbActionClass = VersionType.V1_13.isHigherOrEqual()
                        ? "ScoreboardServer$Action"
                        : "PacketPlayOutScoreboardScore$EnumScoreboardAction";
                ENUM_SB_HEALTH_DISPLAY = FastReflection.nmsClass("world.scores.criteria", "IScoreboardCriteria$EnumScoreboardHealthDisplay");
                ENUM_SB_ACTION = FastReflection.nmsClass("server", enumSbActionClass);
                ENUM_SB_HEALTH_DISPLAY_INTEGER = FastReflection.enumValueOf(ENUM_SB_HEALTH_DISPLAY, "INTEGER", 0);
                ENUM_SB_ACTION_CHANGE = FastReflection.enumValueOf(ENUM_SB_ACTION, "CHANGE", 0);
                ENUM_SB_ACTION_REMOVE = FastReflection.enumValueOf(ENUM_SB_ACTION, "REMOVE", 1);
            } else {
                ENUM_SB_HEALTH_DISPLAY = null;
                ENUM_SB_ACTION = null;
                ENUM_SB_HEALTH_DISPLAY_INTEGER = null;
                ENUM_SB_ACTION_CHANGE = null;
                ENUM_SB_ACTION_REMOVE = null;
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private final Player player;
    private final String id;

    private final List<T> lines = new ArrayList<>();
    private T title = emptyLine();

    private boolean deleted = false;

    /**
     * Creates a new FastBoard.
     *
     * @param player the owner of the scoreboard
     */
    protected FastBoardBase(Player player) {
        this.player = Objects.requireNonNull(player, "player");
        this.id = "fb-" + Integer.toHexString(ThreadLocalRandom.current().nextInt());

        try {
            sendObjectivePacket(ObjectiveMode.CREATE);
            sendDisplayObjectivePacket();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create scoreboard", t);
        }
    }

    /**
     * Get the scoreboard title.
     *
     * @return the scoreboard title
     */
    public T getTitle() {
        return this.title;
    }

    /**
     * Update the scoreboard title.
     *
     * @param title the new scoreboard title
     * @throws IllegalArgumentException if the title is longer than 32 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public void updateTitle(T title) {
        if (this.title.equals(Objects.requireNonNull(title, "title"))) {
            return;
        }

        this.title = title;

        try {
            sendObjectivePacket(ObjectiveMode.UPDATE);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard title", t);
        }
    }

    /**
     * Get the scoreboard lines.
     *
     * @return the scoreboard lines
     */
    public List<T> getLines() {
        return new ArrayList<>(this.lines);
    }

    /**
     * Get the specified scoreboard line.
     *
     * @param line the line number
     * @return the line
     * @throws IndexOutOfBoundsException if the line is higher than {@code size}
     */
    public T getLine(int line) {
        checkLineNumber(line, true, false);

        return this.lines.get(line);
    }

    /**
     * Update a single scoreboard line.
     *
     * @param line the line number
     * @param text the new line text
     * @throws IndexOutOfBoundsException if the line is higher than {@link #size() size() + 1}
     */
    public synchronized void updateLine(int line, T text) {
        checkLineNumber(line, false, true);

        try {
            if (line < size()) {
                this.lines.set(line, text);

                sendLineChange(getScoreByLine(line));
                return;
            }

            List<T> newLines = new ArrayList<>(this.lines);

            if (line > size()) {
                for (int i = size(); i < line; i++) {
                    newLines.add(emptyLine());
                }
            }

            newLines.add(text);

            updateLines(newLines);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard lines", t);
        }
    }

    /**
     * Remove a scoreboard line.
     *
     * @param line the line number
     */
    public synchronized void removeLine(int line) {
        checkLineNumber(line, false, false);

        if (line >= size()) {
            return;
        }

        List<T> newLines = new ArrayList<>(this.lines);
        newLines.remove(line);
        updateLines(newLines);
    }

    /**
     * Update all the scoreboard lines.
     *
     * @param lines the new lines
     * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public void updateLines(T... lines) {
        updateLines(Arrays.asList(lines));
    }

    /**
     * Update the lines of the scoreboard
     *
     * @param lines the new scoreboard lines
     * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public synchronized void updateLines(Collection<T> lines) {
        Objects.requireNonNull(lines, "lines");
        checkLineNumber(lines.size(), false, true);

        List<T> oldLines = new ArrayList<>(this.lines);
        this.lines.clear();
        this.lines.addAll(lines);

        int linesSize = this.lines.size();

        try {
            if (oldLines.size() != linesSize) {
                List<T> oldLinesCopy = new ArrayList<>(oldLines);

                if (oldLines.size() > linesSize) {
                    for (int i = oldLinesCopy.size(); i > linesSize; i--) {
                        sendTeamPacket(i - 1, TeamMode.REMOVE);
                        sendScorePacket(i - 1, ScoreboardAction.REMOVE);

                        oldLines.remove(0);
                    }
                } else {
                    for (int i = oldLinesCopy.size(); i < linesSize; i++) {
                        sendScorePacket(i, ScoreboardAction.CHANGE);
                        sendTeamPacket(i, TeamMode.CREATE, null, null);
                    }
                }
            }

            for (int i = 0; i < linesSize; i++) {
                if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(i))) {
                    sendLineChange(i);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Unable to update scoreboard lines", t);
        }
    }

    /**
     * Get the player who has the scoreboard.
     *
     * @return current player for this FastBoard
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Get the scoreboard id.
     *
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get if the scoreboard is deleted.
     *
     * @return true if the scoreboard is deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Get the scoreboard size (the number of lines).
     *
     * @return the size
     */
    public int size() {
        return this.lines.size();
    }

    /**
     * Delete this FastBoard, and will remove the scoreboard for the associated player if he is online.
     * After this, all uses of {@link #updateLines} and {@link #updateTitle} will throw an {@link IllegalStateException}
     *
     * @throws IllegalStateException if this was already call before
     */
    public void delete() {
        try {
            for (int i = 0; i < this.lines.size(); i++) {
                sendTeamPacket(i, TeamMode.REMOVE);
            }

            sendObjectivePacket(ObjectiveMode.REMOVE);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to delete scoreboard", t);
        }

        this.deleted = true;
    }

    protected abstract void sendLineChange(int score) throws Throwable;

    protected abstract Object toMinecraftComponent(T value) throws Throwable;

    protected abstract T emptyLine();

    private void checkLineNumber(int line, boolean checkInRange, boolean checkMax) {
        if (line < 0) {
            throw new IllegalArgumentException("Line number must be positive");
        }

        if (checkInRange && line >= this.lines.size()) {
            throw new IllegalArgumentException("Line number must be under " + this.lines.size());
        }

        if (checkMax && line >= COLOR_CODES.length - 1) {
            throw new IllegalArgumentException("Line number is too high: " + line);
        }
    }

    protected int getScoreByLine(int line) {
        return this.lines.size() - line - 1;
    }

    protected T getLineByScore(int score) {
        return getLineByScore(this.lines, score);
    }

    protected T getLineByScore(List<T> lines, int score) {
        return score < lines.size() ? lines.get(lines.size() - score - 1) : null;
    }

    protected void sendObjectivePacket(ObjectiveMode mode) throws Throwable {
        Object packet = PACKET_SB_OBJ.invoke();

        setField(packet, String.class, this.id);
        setField(packet, int.class, mode.ordinal());

        if (mode != ObjectiveMode.REMOVE) {
            setComponentField(packet, this.title, 1);

            if (VersionType.V1_8.isHigherOrEqual()) {
                setField(packet, ENUM_SB_HEALTH_DISPLAY, ENUM_SB_HEALTH_DISPLAY_INTEGER);
            }
        } else if (VERSION_TYPE == VersionType.V1_7) {
            setField(packet, String.class, "", 1);
        }

        sendPacket(packet);
    }

    protected void sendDisplayObjectivePacket() throws Throwable {
        Object packet = PACKET_SB_DISPLAY_OBJ.invoke();

        setField(packet, int.class, 1); // Position (1: sidebar)
        setField(packet, String.class, this.id); // Score Name

        sendPacket(packet);
    }

    protected void sendScorePacket(int score, ScoreboardAction action) throws Throwable {
        Object packet = PACKET_SB_SCORE.invoke();

        setField(packet, String.class, COLOR_CODES[score], 0); // Player Name

        if (VersionType.V1_8.isHigherOrEqual()) {
            Object enumAction = action == ScoreboardAction.REMOVE
                    ? ENUM_SB_ACTION_REMOVE : ENUM_SB_ACTION_CHANGE;
            setField(packet, ENUM_SB_ACTION, enumAction);
        } else {
            setField(packet, int.class, action.ordinal(), 1); // Action
        }

        if (action == ScoreboardAction.CHANGE) {
            setField(packet, String.class, this.id, 1); // Objective Name
            setField(packet, int.class, score); // Score
        }

        sendPacket(packet);
    }

    protected void sendTeamPacket(int score, TeamMode mode) throws Throwable {
        sendTeamPacket(score, mode, null, null);
    }

    protected void sendTeamPacket(int score, TeamMode mode, T prefix, T suffix)
            throws Throwable {
        if (mode == TeamMode.ADD_PLAYERS || mode == TeamMode.REMOVE_PLAYERS) {
            throw new UnsupportedOperationException();
        }

        Object packet = PACKET_SB_TEAM.invoke();

        setField(packet, String.class, this.id + ':' + score); // Team name
        setField(packet, int.class, mode.ordinal(), VERSION_TYPE == VersionType.V1_8 ? 1 : 0); // Update mode

        if (mode == TeamMode.REMOVE) {
            sendPacket(packet);
            return;
        }

        if (VersionType.V1_17.isHigherOrEqual()) {
            Object team = PACKET_SB_SERIALIZABLE_TEAM.invoke();
            // Since the packet is initialized with null values, we need to change more things.
            setComponentField(team, null, 0); // Display name
            setField(team, CHAT_FORMAT_ENUM, RESET_FORMATTING); // Color
            setComponentField(team, prefix, 1); // Prefix
            setComponentField(team, suffix, 2); // Suffix
            setField(team, String.class, "always", 0); // Visibility
            setField(team, String.class, "always", 1); // Collisions
            setField(packet, Optional.class, Optional.of(team));
        } else {
            setComponentField(packet, prefix, 2); // Prefix
            setComponentField(packet, suffix, 3); // Suffix
            setField(packet, String.class, "always", 4); // Visibility for 1.8+
            setField(packet, String.class, "always", 5); // Collisions for 1.9+
        }

        if (mode == TeamMode.CREATE) {
            setField(packet, Collection.class, Collections.singletonList(COLOR_CODES[score])); // Players in the team
        }

        sendPacket(packet);
    }

    private void sendPacket(Object packet) throws Throwable {
        if (this.deleted) {
            throw new IllegalStateException("This FastBoard is deleted");
        }

        if (this.player.isOnline()) {
            Object entityPlayer = PLAYER_GET_HANDLE.invoke(this.player);
            Object playerConnection = PLAYER_CONNECTION.invoke(entityPlayer);
            SEND_PACKET.invoke(playerConnection, packet);
        }
    }

    private void setField(Object object, Class<?> fieldType, Object value)
            throws ReflectiveOperationException {
        setField(object, fieldType, value, 0);
    }

    private void setField(Object packet, Class<?> fieldType, Object value, int count)
            throws ReflectiveOperationException {
        int i = 0;
        for (Field field : PACKETS.get(packet.getClass())) {
            if (field.getType() == fieldType && count == i++) {
                field.set(packet, value);
            }
        }
    }

    private void setComponentField(Object packet, T value, int count) throws Throwable {
        if (!VersionType.V1_13.isHigherOrEqual()) {
            setField(packet, String.class, value != null ? value : "", count);
            return;
        }

        int i = 0;
        for (Field field : PACKETS.get(packet.getClass())) {
            if ((field.getType() == String.class || field.getType() == CHAT_COMPONENT_CLASS) && count == i++) {
                field.set(packet, toMinecraftComponent(value));
            }
        }
    }

    public enum ObjectiveMode {
        CREATE, REMOVE, UPDATE
    }

    public enum TeamMode {
        CREATE, REMOVE, UPDATE, ADD_PLAYERS, REMOVE_PLAYERS
    }

    public enum ScoreboardAction {
        CHANGE, REMOVE
    }

    enum VersionType {
        V1_7, V1_8, V1_13, V1_17;

        public boolean isHigherOrEqual() {
            return VERSION_TYPE.ordinal() >= ordinal();
        }
    }
}
