package tech.smartboot.mqtt.plugin.spec.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 2/3/26
 */
public class Schema {
    private final List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }
}
