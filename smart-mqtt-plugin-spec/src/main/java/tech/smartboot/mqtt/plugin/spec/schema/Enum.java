package tech.smartboot.mqtt.plugin.spec.schema;

/**
 * @author 三刀
 * @version v1.0 2/3/26
 */
public class Enum {
    private final String value;
    private final String desc;

    public Enum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static Enum of(String value, String desc) {
        return new Enum(value, desc);
    }

    public String getValue() {
        return value;
    }


    public String getDesc() {
        return desc;
    }

}
