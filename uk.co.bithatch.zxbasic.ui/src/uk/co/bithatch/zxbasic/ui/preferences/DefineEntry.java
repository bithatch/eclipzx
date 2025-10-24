package uk.co.bithatch.zxbasic.ui.preferences;
public class DefineEntry {
    private String name;
    private String value;

    public DefineEntry(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public DefineEntry() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
