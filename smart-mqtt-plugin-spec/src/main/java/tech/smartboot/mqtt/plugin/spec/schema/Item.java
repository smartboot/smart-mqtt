package tech.smartboot.mqtt.plugin.spec.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 2/3/26
 */
public class Item {
    private final String name;
    private final String type;
    private final String desc;
    private final List<Item> items = new ArrayList<>();
    private final List<Enum> enums = new ArrayList<>();

    public static Item Object(String name, String desc) {
        return new Item(name, "object", desc);
    }

    public static Item Int(String name, String desc) {
        return new Item(name, "int", desc) {
            @Override
            public void addItems(Item... items) {
                throw new UnsupportedOperationException("int类型不能添加子集");
            }
        };
    }

    public static Item String(String name, String desc) {
        return new Item(name, "string", desc) {
            @Override
            public void addItems(Item... items) {
                throw new UnsupportedOperationException("string类型不能添加子集");
            }
        };
    }

    public static Item TextArea(String name, String desc) {
        return new Item(name, "textarea", desc) {
            @Override
            public void addItems(Item... items) {
                throw new UnsupportedOperationException("textarea类型不能添加子集");
            }
        };
    }

    public static Item Password(String name, String desc) {
        return new Item(name, "password", desc) {
            @Override
            public void addItems(Item... items) {
                throw new UnsupportedOperationException("password类型不能添加子集");
            }
        };
    }

    public Item(String name, String type, String desc) {
        this.name = name;
        this.type = type;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }


    public String getDesc() {
        return desc;
    }

    public List<Enum> getEnums() {
        return enums;
    }

    public String getType() {
        return type;
    }

    public Item addEnums(Enum... enums) {
        this.enums.addAll(Arrays.asList(enums));
        return this;
    }

    public void addItems(Item... items) {
        this.items.addAll(Arrays.asList(items));
    }

    public List<Item> getItems() {
        return items;
    }
}
