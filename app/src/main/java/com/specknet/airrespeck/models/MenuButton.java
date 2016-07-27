package com.specknet.airrespeck.models;

public class MenuButton {
    public enum buttonType {
        HOME, AIR_QUALITY, GRAPHS
    }

    private buttonType type;
    private String label;
    private int image;

    public MenuButton(final buttonType type, final String label, final int image) {
        this.type = type;
        this.label = label;
        this.image = image;
    }

    public buttonType getType(){
        return type;
    }

    public String getLabel() {
        return label;
    }

    public int getImage() {
        return image;
    }
}
