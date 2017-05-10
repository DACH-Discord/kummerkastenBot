package nikos.kummerkastenBot.util;

import java.awt.Color;

public class AnonUser {
    private final String id;
    private final Color color;

    public AnonUser (final String anonID, final Color anonColor) {
        this.id = anonID;
        this.color = anonColor;
    }

    public String  getID() {
        return this.id;
    }

    public Color getColor() {
        return this.color;
    }
}
