package com.flooent.translate;

import com.google.gson.annotations.SerializedName;

/**
 * Created by rajsingh on 08/04/17.
 */
public class Langs {
    public String code;
    public String name;
    public String nativeName;
    public boolean isSelected;

    @SerializedName ("country_flag")
    public String flagImagePath;

    public Langs(String code, String name, String flagImagePath, String nativeName) {
        this.code = code;
        this.name = name;
        this.flagImagePath = flagImagePath;
        this.nativeName = nativeName;
    }
}
