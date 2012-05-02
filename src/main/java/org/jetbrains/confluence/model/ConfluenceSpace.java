package org.jetbrains.confluence.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceSpace {

    private String key = "";
    private String name = "";

    public ConfluenceSpace(String key, String name) {
        this.key = key;
        this.name = name;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public String getName() {
        return name;
    }

}
