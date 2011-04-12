package edu.stanford.mobisocial.dungbeetle.model;

public class Presence{
    public static final int AVAILABLE = 0;
    public static final int BUSY = 1;
    public static final int AWAY = 2;

    public static final String[] presences = new String[] { "Available", "Busy", "Away" };
    public static final int[] colors = new int[] {0xFF5DF22C, 0xFFFA3434, 0xFFFAA834};
}
