package com.specknet.airrespeck.utils;

public class ButtonDesc {
    public enum buttonType {
        HOME, DASHBOARD, SETTINGS
    }

    private buttonType type;
    private String label;
    private int image;

    public ButtonDesc(final buttonType type, final String label, final int image) {
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
