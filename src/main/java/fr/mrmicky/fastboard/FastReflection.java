package fr.mrmicky.fastboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author MrMicky
 */
public class FastReflection {


    private static final String PACKAGE_NMS = "net.minecraft.server.";
    private static final String PACKAGE_OCB = "org.bukkit.craftbukkit.";

    public static final String VERSION;
    public static final VersionType VERSION_TYPE;

    // Packet sending
    private static final Field PLAYER_CONNECTION;
    private static final Method SEND_PACKET;
    private static final Method PLAYER_GET_HANDLE;

    // Utils
    public static final Class<?> CHAT_COMPONENT_CLASS;
    public static final Method MESSAGE_FROM_STRING;

    // Packets
    public static final Constructor<?> PACKET_SB_OBJ;
    public static final Constructor<?> PACKET_SB_DISPLAY_OBJ;
    public static final Constructor<?> PACKET_SB_SCORE;
    public static final Constructor<?> PACKET_SB_TEAM;

    // Enums
    public static final Class<?> ENUM_SB_HEALTH_DISPLAY;
    public static final Class<?> ENUM_SB_ACTION;
    public static final Object ENUM_SB_HEALTH_DISPLAY_INTEGER;
    public static final Object ENUM_SB_ACTION_CHANGE;
    public static final Object ENUM_SB_ACTION_REMOVE;

    static {
        VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(PACKAGE_OCB.length());

        if (isClassExistsNMS("ScoreboardServer$Action")) {
            VERSION_TYPE = VersionType.V1_13;
        } else if (isClassExistsNMS("IScoreboardCriteria$EnumScoreboardHealthDisplay")) {
            VERSION_TYPE = VersionType.V1_8;
        } else {
            VERSION_TYPE = VersionType.V1_7;
        }

        try {
            Class<?> craftChatMessageClass = getClassOCB("util.CraftChatMessage");

            Class<?> entityPlayerClass = getClassNMS("EntityPlayer");
            Class<?> playerConnectionClass = getClassNMS("PlayerConnection");
            Class<?> craftPlayerClass = getClassOCB("entity.CraftPlayer");

            MESSAGE_FROM_STRING = craftChatMessageClass.getDeclaredMethod("fromString", String.class);
            CHAT_COMPONENT_CLASS = getClassNMS("IChatBaseComponent");

            PLAYER_GET_HANDLE = craftPlayerClass.getDeclaredMethod("getHandle");
            PLAYER_CONNECTION = entityPlayerClass.getDeclaredField("playerConnection");
            SEND_PACKET = playerConnectionClass.getDeclaredMethod("sendPacket", getClassNMS("Packet"));

            PACKET_SB_OBJ = getClassNMS("PacketPlayOutScoreboardObjective").getConstructor();
            PACKET_SB_DISPLAY_OBJ = getClassNMS("PacketPlayOutScoreboardDisplayObjective").getConstructor();
            PACKET_SB_SCORE = getClassNMS("PacketPlayOutScoreboardScore").getConstructor();
            PACKET_SB_TEAM = getClassNMS("PacketPlayOutScoreboardTeam").getConstructor();

            if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_8)) {
                ENUM_SB_HEALTH_DISPLAY = getClassNMS("IScoreboardCriteria$EnumScoreboardHealthDisplay");

                if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)) {
                    ENUM_SB_ACTION = getClassNMS("ScoreboardServer$Action");
                } else {
                    ENUM_SB_ACTION = getClassNMS("PacketPlayOutScoreboardScore$EnumScoreboardAction");
                }

                ENUM_SB_HEALTH_DISPLAY_INTEGER = enumValueof(ENUM_SB_HEALTH_DISPLAY, "INTEGER");

                ENUM_SB_ACTION_CHANGE = enumValueof(ENUM_SB_ACTION, "CHANGE");
                ENUM_SB_ACTION_REMOVE = enumValueof(ENUM_SB_ACTION, "REMOVE");
            } else {
                ENUM_SB_HEALTH_DISPLAY = null;
                ENUM_SB_ACTION = null;

                ENUM_SB_HEALTH_DISPLAY_INTEGER = null;
                ENUM_SB_ACTION_CHANGE = null;
                ENUM_SB_ACTION_REMOVE = null;
            }

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private FastReflection() {
        throw new UnsupportedOperationException();
    }

    public static void sendPacket(Player p, Object packet) throws ReflectiveOperationException {
        Object entityPlayer = PLAYER_GET_HANDLE.invoke(p);
        Object playerConnection = PLAYER_CONNECTION.get(entityPlayer);
        SEND_PACKET.invoke(playerConnection, packet);
    }

    public static Object getChatBaseComponent(String s) throws ReflectiveOperationException {
        return Array.get(MESSAGE_FROM_STRING.invoke(null, s), 0);
    }

    private static Class<?> getClassNMS(String name) throws ClassNotFoundException {
        return Class.forName(PACKAGE_NMS + VERSION + '.' + name);
    }

    private static Object enumValueof(Class<?> enumClass, String name) throws ReflectiveOperationException {
        return enumClass.getField(name).get(null);
    }

    private static Class<?> getClassOCB(String name) throws ClassNotFoundException {
        return Class.forName(PACKAGE_OCB + VERSION + '.' + name);
    }

    private static boolean isClassExistsNMS(String className) {
        try {
            Class.forName(PACKAGE_NMS + VERSION + '.' + className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    enum VersionType {
        V1_7, V1_8, V1_13;

        public boolean isHigherOrEqual(VersionType ver) {
            return ordinal() >= ver.ordinal();
        }

        public boolean isLowerOrEqual(VersionType ver) {
            return ordinal() <= ver.ordinal();
        }
    }
}
