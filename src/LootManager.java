import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.items.GroundItem;

import java.util.List;

public class LootManager {
    private final CowCombatTrainerPro script;
    private final AreaManager areaManager;
    private long lastLootTime = 0;
    private static final long LOOT_COOLDOWN = 45000;

    public LootManager(CowCombatTrainerPro script, AreaManager areaManager) {
        this.script = script;
        this.areaManager = areaManager;
    }

    public boolean shouldLoot() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastLootTime > LOOT_COOLDOWN &&
                Calculations.random(1, 4) == 1 &&
                isLootInArea() &&
                !Inventory.isFull();
    }

    public void lootAllInArea() {
        List<GroundItem> lootItems = GroundItems.all(item ->
                item != null &&
                        (item.getName().equals("Cowhide") || item.getName().equals("Bones")) &&
                        areaManager.getCurrentCowArea().contains(item) &&
                        item.exists()
        );

        if (!lootItems.isEmpty()) {
            lootItems.sort((i1, i2) -> (int)(i1.distance() - i2.distance()));

            int maxLoot = Calculations.random(3, 5);
            int looted = 0;

            for (GroundItem item : lootItems) {
                if (looted >= maxLoot || Inventory.isFull()) break;
                if (item.interact()) {
                    script.log("Recogiendo: " + item.getName());
                    Sleep.sleepUntil(() -> !item.exists(), 2000);
                    Sleep.sleep(Calculations.random(600, 1000));
                    looted++;
                }
            }
            script.log("Sesión de loot completada: " + looted + " items recogidos");
        }
        lastLootTime = System.currentTimeMillis();
    }

    private boolean isLootInArea() {
        return GroundItems.closest(item ->
                item != null &&
                        (item.getName().equals("Cowhide") || item.getName().equals("Bones")) &&
                        areaManager.getCurrentCowArea().contains(item)
        ) != null;
    }

    public long getTimeUntilNextLoot() {
        return Math.max(0, (lastLootTime + LOOT_COOLDOWN) - System.currentTimeMillis());
    }
}