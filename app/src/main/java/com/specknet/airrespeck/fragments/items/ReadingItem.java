package com.specknet.airrespeck.fragments.items;

public class ReadingItem {
    public String name;
    public String units;
    public int value;

    public ReadingItem(String name, String units, int value) {
        this.name = name;
        this.units = units;
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " (" + units + ") = " + String.valueOf(value);
    }
}
