package fr.mrmicky.fastboard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Simple Bukkit ScoreBoard API with 1.15 support !
 * Everything is at packet level so you don't need to use it in the main server thread.
 * <p>
 * You can find the project on <a href="https://github.com/MrMicky-FR/FastBoard">GitHub</a>
 * Fork:  <a href="https://github.com/Ytnoos/FastBoard">GitHub</a>
 *
 * @author MrMicky
 * @author Ytnoos
 */
public class FastBoard {

    private final Player player;
    private final String id;

    private String title = ChatColor.RESET.toString();
    private List<String> lines = new ArrayList<>();

    private boolean deleted = false;

    /**
     * Creates a new FastBoard.
     *
     * @param player the player the scoreboard is for
     */
    public FastBoard(Player player) {
        this.player = Objects.requireNonNull(player, "player");

        id = "fb-" + Double.toString(Math.random()).substring(2, 10);

        sendObjectivePacket(ObjectiveMode.CREATE);
        sendDisplayObjectivePacket();
    }

    /**
     * Get the scoreboard title.
     *
     * @return the scoreboard title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Update the scoreboard title.
     *
     * @param title the new scoreboard title
     * @throws IllegalArgumentException if the title is longer than 32 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public void updateTitle(String title) {
        if (this.title.equals(Objects.requireNonNull(title, "title"))) {
            return;
        }

        this.title = title;

        sendObjectivePacket(ObjectiveMode.UPDATE);
    }

    /**
     * Get the scoreboard lines.
     *
     * @return the scoreboard lines
     */
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    /**
     * Get the specified scoreboard line.
     *
     * @param line the line number
     * @return the line
     * @throws IndexOutOfBoundsException if the line is higher than {@code size}
     */
    public String getLine(int line) {
        checkLineNumber(line, true);

        return lines.get(line);
    }

    /**
     * Update a single scoreboard line.
     *
     * @param line the line number
     * @param text the new line text
     * @throws IndexOutOfBoundsException if the line is higher than {@code size} + 1
     */
    public void updateLine(int line, String text) {
        checkLineNumber(line, false);

        if (line < size()) {
            lines.set(line, text);

            sendTeamPacket(getScoreByLine(line), TeamMode.UPDATE);
            return;
        }

        List<String> newLines = new ArrayList<>(lines);

        if (line > size()) {
            for (int i = size(); i < line; i++) {
                newLines.add("");
            }
        }

        newLines.add(text);

        updateLines(newLines);
    }

    /**
     * Remove a scoreboard line.
     *
     * @param line the line number
     */
    public void removeLine(int line) {
        checkLineNumber(line, false);

        if (line >= size()) {
            return; // The line don't exists
        }

        List<String> lines = new ArrayList<>(this.lines);
        lines.remove(line);
        updateLines(lines);
    }

    /**
     * Update all the scoreboard lines.
     *
     * @param lines the new lines
     * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public void updateLines(String... lines) {
        updateLines(Arrays.asList(lines));
    }

    /**
     * Update the lines of the scoreboard
     *
     * @param lines the new scoreboard lines
     * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
     * @throws IllegalStateException    if {@link #delete()} was call before
     */
    public void updateLines(Collection<String> lines) {
        Objects.requireNonNull(lines, "lines");

        List<String> oldLines = new ArrayList<>(this.lines);
        this.lines.clear();
        this.lines.addAll(lines);

        int linesSize = this.lines.size();

        if (oldLines.size() != linesSize) {
            List<String> oldLinesCopy = new ArrayList<>(oldLines);

            if (oldLines.size() > linesSize) {
                for (int i = oldLinesCopy.size(); i > linesSize; i--) {
                    sendTeamPacket(i - 1, TeamMode.REMOVE);

                    sendScorePacket(i - 1, EnumWrappers.ScoreboardAction.REMOVE);

                    oldLines.remove(0);
                }
            } else {
                for (int i = oldLinesCopy.size(); i < linesSize; i++) {
                    sendScorePacket(i, EnumWrappers.ScoreboardAction.CHANGE);

                    sendTeamPacket(i, TeamMode.CREATE);

                    oldLines.add(oldLines.size() - i, getLineByScore(i));
                }
            }
        }

        for (int i = 0; i < linesSize; i++) {
            if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(i))) {
                sendTeamPacket(i, TeamMode.UPDATE);
            }
        }
    }

    /**
     * Get the player who has the scoreboard.
     *
     * @return current player for this FastBoard
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the scoreboard id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get if the scoreboard is deleted.
     *
     * @return true if the scoreboard is deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Get the scoreboard size (the number of lines).
     *
     * @return the size
     */
    public int size() {
        return lines.size();
    }

    /**
     * Delete this FastBoard, and will remove the scoreboard for the associated player if he is online.
     * After this, all uses of {@link #updateLines} and {@link #updateTitle} will throws an {@link IllegalStateException}
     *
     * @throws IllegalStateException if this was already call before
     */
    public void delete() {
        for (int i = 0; i < lines.size(); i++) {
            sendTeamPacket(i, TeamMode.REMOVE);
        }

        sendObjectivePacket(ObjectiveMode.REMOVE);

        deleted = true;
    }

    private void checkLineNumber(int line, boolean checkMax) {
        if (line < 0) {
            throw new IllegalArgumentException("Line number must be positive");
        }

        if (checkMax && line >= lines.size()) {
            throw new IllegalArgumentException("Line number must be under " + lines.size());
        }
    }

    private int getScoreByLine(int line) {
        return lines.size() - line - 1;
    }

    private String getLineByScore(int score) {
        return getLineByScore(lines, score);
    }

    private String getLineByScore(List<String> lines, int score) {
        return lines.get(lines.size() - score - 1);
    }

    private void sendObjectivePacket(ObjectiveMode mode) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);

        packet.getStrings().write(0, id);
        packet.getIntegers().write(0, mode.ordinal());

        if (mode != ObjectiveMode.REMOVE) {
            packet.getChatComponents().write(0, WrappedChatComponent.fromText(title));
            packet.getEnumModifier(HealthDisplay.class, 2).write(0, HealthDisplay.INTEGER);
        }

        sendPacket(packet);
    }

    private void sendDisplayObjectivePacket() {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);

        packet.getIntegers().write(0, 1);
        packet.getStrings().write(0, id);

        sendPacket(packet);
    }

    private void sendScorePacket(int score, EnumWrappers.ScoreboardAction action) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);

        packet.getStrings().write(0, getColorCode(score));
        packet.getScoreboardActions().write(0, action);
        if (action == EnumWrappers.ScoreboardAction.CHANGE) {
            packet.getStrings().write(1, id);
            packet.getIntegers().write(0, score);
        }

        sendPacket(packet);
    }

    private void sendTeamPacket(int score, TeamMode mode) {
        if (mode == TeamMode.ADD_PLAYERS || mode == TeamMode.REMOVE_PLAYERS) {
            throw new UnsupportedOperationException();
        }

        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, id + ":" + score);
        packet.getIntegers().write(0, mode.ordinal());

        if (mode == TeamMode.CREATE || mode == TeamMode.UPDATE) {
            String line = getLineByScore(score);
            String prefix;
            String suffix = null;

            if (line == null || line.isEmpty()) {
                prefix = getColorCode(score) + ChatColor.RESET;
            } else if (line.length() <= 16) {
                prefix = line;
            } else {
                // Prevent splitting color codes
                int index = line.charAt(15) == ChatColor.COLOR_CHAR ? 15 : 16;
                prefix = line.substring(0, index);
                String suffixTmp = line.substring(index);
                ChatColor chatColor = null;

                if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
                    chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
                }

                String color = ChatColor.getLastColors(prefix);
                boolean addColor = chatColor == null || chatColor.isFormat();

                suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET : color) : "") + suffixTmp;
            }

            packet.getChatComponents().write(1, WrappedChatComponent.fromText(prefix));
            packet.getChatComponents().write(2, WrappedChatComponent.fromText(suffix == null ? "" : suffix));
            packet.getStrings().write(1, "always");
            packet.getStrings().write(2, "always");

            if (mode == TeamMode.CREATE) {
                packet.getSpecificModifier(Collection.class).write(0, Collections.singletonList(getColorCode(score)));
            }
        }

        sendPacket(packet);
    }

    private String getColorCode(int score) {
        return ChatColor.values()[score].toString();
    }

    private void sendPacket(PacketContainer packetContainer) {
        if (deleted) {
            throw new IllegalStateException("This FastBoard is deleted");
        }
        if (player.isOnline()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    enum ObjectiveMode {

        CREATE, REMOVE, UPDATE

    }

    enum TeamMode {

        CREATE, REMOVE, UPDATE, ADD_PLAYERS, REMOVE_PLAYERS

    }

    enum HealthDisplay {
        INTEGER, HEARTS
    }
}
