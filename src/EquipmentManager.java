import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;

public class EquipmentManager {
    private final CowCombatTrainerPro script;
    private final Area tutorArea = new Area(3212, 3241, 3220, 3237);

    public EquipmentManager(CowCombatTrainerPro script) {
        this.script = script;
    }

    public boolean needEquipment() {
        boolean hasWeapon = Inventory.contains("Training sword") || Equipment.contains("Training sword");
        boolean hasShield = Inventory.contains("Training shield") || Equipment.contains("Training shield");
        return !hasWeapon || !hasShield;
    }

    public boolean hasItemsToEquip() {
        return Inventory.contains("Training sword") || Inventory.contains("Training shield");
    }

    public void equipItems() {
        String[] equipmentPriority = {"Training sword", "Training shield"};
        for (String itemName : equipmentPriority) {
            if (Inventory.contains(itemName) && !Equipment.contains(itemName)) {
                if (Inventory.interact(itemName)) {
                    script.log("Equipando: " + itemName);
                    Sleep.sleep(Calculations.random(800, 1200));
                    break;
                }
            }
        }
    }

    public void getEquipmentFromTutor() {
        if (!tutorArea.contains(Players.getLocal())) {
            if (Walking.shouldWalk()) {
                Walking.walk(tutorArea.getCenter());
                Sleep.sleepUntil(() -> tutorArea.contains(Players.getLocal()), 1500);
            }
            return;
        }

        NPC tutor = NPCs.closest(npc -> npc != null && npc.getName().equals("Melee combat tutor"));

        if (tutor != null) {
            if(!Dialogues.inDialogue()) tutor.interact();
            handleTutorDialogue();
        }
    }

    private void handleTutorDialogue() {
        if (Dialogues.inDialogue()) {
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(Calculations.random(1200, 2000));
            }
            if (Dialogues.areOptionsAvailable()) {
                if (Dialogues.chooseOption(4)) {
                    Sleep.sleep(Calculations.random(1200, 2000));
                }
            }
        }
    }
}