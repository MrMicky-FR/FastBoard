package fr.mrmicky.fastboard;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;

public class FastBoard {

    private Player player;
    private String id;

    private String title;
    private List<String> lines = new ArrayList<>();

    public FastBoard(Player player) {
        this.player = player;

        id = "fb-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            sendObjectivePacket(ObjectiveMode.CREATE);
            sendDisplayObjectivePacket();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void updateTitle(String title) {
        if (title.equals(this.title)) {
            return;
        }

        if (title.length() > 32) {
            throw new IllegalArgumentException("Title is longer than 32 chars");
        }

        this.title = title;

        try {
            sendObjectivePacket(ObjectiveMode.UPDATE);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void updateLines(String... lines) {
        updateLines(Arrays.asList(lines));
    }

    public void updateLines(List<String> newLines) {
        int lineCount = 0;
        for (String s : newLines) {
            if (s.length() > 30 && FastReflection.VERSION_TYPE != FastReflection.VersionType.V1_13) {
                throw new IllegalArgumentException("Line " + lineCount + " is longer than 30 chars");
            }
            lineCount++;
        }

        List<String> lines = new ArrayList<>(newLines);
        Collections.reverse(lines);

        List<String> oldLines = this.lines;

        this.lines = lines;

        try {
            if (oldLines.size() != lines.size()) {
                List<String> oldLinesCopy = new ArrayList<>(oldLines);

                if (oldLinesCopy.size() > lines.size()) {
                    for (int i = oldLinesCopy.size(); i > lines.size(); i--) {
                        sendTeamPacket(i - 1, TeamMode.REMOVE);

                        sendScorePacket(i - 1, true);

                        oldLines.remove(oldLines.size() - 1);
                    }
                } else {
                    for (int i = oldLinesCopy.size(); i < lines.size(); i++) {
                        sendScorePacket(i, false);

                        sendTeamPacket(i, TeamMode.CREATE);

                        oldLines.add(i, lines.get(i));
                    }
                }
            }

            if (lines.isEmpty()) {
                return;
            }

            for (int i = 0; i < lines.size(); i++) {
                if (!Objects.equals(oldLines.get(i), lines.get(i))) {
                    sendTeamPacket(i, TeamMode.UPDATE);
                }
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public List<String> getLines() {
        List<String> lines = new ArrayList<>(this.lines);
        Collections.reverse(lines);
        return lines;
    }

    public Player getPlayer() {
        return player;
    }

    public void delete() {
        try {
            for (int i = 0; i < lines.size(); i++) {
                sendTeamPacket(i, TeamMode.REMOVE);
            }

            sendObjectivePacket(ObjectiveMode.REMOVE);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        player = null;
    }

    private void sendObjectivePacket(ObjectiveMode mode) throws ReflectiveOperationException {
        Object packet = FastReflection.PACKET_SB_OBJ.newInstance();

        setField(packet, String.class, id);
        setField(packet, int.class, mode.ordinal());

        if (mode != ObjectiveMode.REMOVE) {
            setComponentField(packet, title != null ? title : ChatColor.RESET.toString(), 1);

            if (FastReflection.VERSION_TYPE.isHigherOrEqual(FastReflection.VersionType.V1_8)) {
                setField(packet, FastReflection.ENUM_SB_HEALTH_DISPLAY, FastReflection.ENUM_SB_HEALTH_DISPLAY_INTEGER);
            }
        } else if (FastReflection.VERSION_TYPE == FastReflection.VersionType.V1_7) {
            setField(packet, String.class, "", 1);
        }

        sendPacket(packet);
    }

    private void sendDisplayObjectivePacket() throws ReflectiveOperationException {
        Object packet = FastReflection.PACKET_SB_DISPLAY_OBJ.newInstance();

        setField(packet, int.class, 1);
        setField(packet, String.class, id);

        sendPacket(packet);
    }

    private void sendScorePacket(int score, boolean remove) throws ReflectiveOperationException {
        Object packet = FastReflection.PACKET_SB_SCORE.newInstance();

        setField(packet, String.class, getColorCode(score), 0);

        if (FastReflection.VERSION_TYPE.isHigherOrEqual(FastReflection.VersionType.V1_8)) {
            if (remove) {
                setField(packet, FastReflection.ENUM_SB_ACTION, FastReflection.ENUM_SB_ACTION_REMOVE);
            } else {
                setField(packet, FastReflection.ENUM_SB_ACTION, FastReflection.ENUM_SB_ACTION_CHANGE);
            }
        } else {
            setField(packet, int.class, remove ? 1 : 0, 1);
        }

        if (!remove) {
            setField(packet, String.class, id, 1);
            setField(packet, int.class, score);
        }

        sendPacket(packet);
    }

    private void sendTeamPacket(int score, TeamMode mode) throws ReflectiveOperationException {
        if (mode == TeamMode.ADD_PLAYERS || mode == TeamMode.REMOVE_PLAYERS) {
            throw new UnsupportedOperationException();
        }

        Object packet = FastReflection.PACKET_SB_TEAM.newInstance();

        setField(packet, String.class, id + ':' + score); // Team name
        setField(packet, int.class, mode.ordinal(), FastReflection.VERSION_TYPE == FastReflection.VersionType.V1_8 ? 1 : 0); // Update mode

        if (mode == TeamMode.CREATE || mode == TeamMode.UPDATE) {
            String line = lines.get(score);
            String prefix;
            String suffix = null;

            if (line == null || line.isEmpty()) {
                prefix = getColorCode(score) + ChatColor.RESET;
            } else if (line.length() <= 16 || FastReflection.VERSION_TYPE == FastReflection.VersionType.V1_13) {
                prefix = line;
            } else {
                prefix = line.substring(0, 16);
                String color = ChatColor.getLastColors(prefix);
                suffix = (color.isEmpty() ? ChatColor.RESET : color) + line.substring(16);
            }

            if (FastReflection.VERSION_TYPE != FastReflection.VersionType.V1_13) {
                if (prefix.length() > 16 || (suffix != null && suffix.length() > 16)) {
                    throw new IllegalArgumentException("Line with score " + score + " is too long: " + line + '(' + prefix + '/' + suffix + ')');
                }
            }

            setComponentField(packet, prefix, 2); // Prefix
            setComponentField(packet, suffix == null ? "" : suffix, 3); // Suffix
            setField(packet, String.class, "always", 4); // Visibility for 1.8+
            setField(packet, String.class, "always", 5); // Collisions for 1.9+

            if (mode == TeamMode.CREATE) {
                setField(packet, Collection.class, Collections.singleton(getColorCode(score))); // Players in the team
            }
        }

        sendPacket(packet);
    }

    private String getColorCode(int score) {
        return ChatColor.values()[score].toString();
    }

    private void sendPacket(Object packet) throws ReflectiveOperationException {
        if (player == null) {
            throw new IllegalStateException("FastBoard is deleted");
        }

        FastReflection.sendPacket(player, packet);
    }

    private void setField(Object object, Class<?> fieldType, Object value) throws ReflectiveOperationException {
        setField(object, fieldType, value, 0);
    }

    private void setField(Object object, Class<?> fieldType, Object value, int count) throws ReflectiveOperationException {
        int i = 0;

        for (Field f : object.getClass().getDeclaredFields()) {
            if (f.getType() == fieldType && i++ == count) {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }

                f.set(object, value);
            }
        }
    }

    private void setComponentField(Object object, String value, int count) throws ReflectiveOperationException {
        if (FastReflection.VERSION_TYPE != FastReflection.VersionType.V1_13) {
            setField(object, String.class, value, count);
            return;
        }

        int i = 0;
        for (Field f : object.getClass().getDeclaredFields()) {
            if ((f.getType() == String.class || f.getType() == FastReflection.CHAT_COMPONENT_CLASS) && i++ == count) {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }

                f.set(object, FastReflection.getChatBaseComponent(value));
            }
        }
    }

    enum ObjectiveMode {

        CREATE, REMOVE, UPDATE

    }

    enum TeamMode {

        CREATE, REMOVE, UPDATE, ADD_PLAYERS, REMOVE_PLAYERS

    }
}
