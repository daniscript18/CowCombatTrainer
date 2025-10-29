import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;

import java.awt.*;

@ScriptManifest(author = "daniscript18", name = "Cow Combat Trainer Pro", version = 1.0, description = "Entrena combate cíclico con vacas", category = Category.COMBAT)
public class CowCombatTrainerPro extends AbstractScript {

    private enum State {
        COMBAT, LOOT, BURY_BONES, BANK, WALK_TO_COWS, WALK_TO_BANK, GET_EQUIPMENT, EQUIP_ITEMS
    }

    // Managers
    private CombatManager combatManager;
    private AreaManager areaManager;
    private LootManager lootManager;
    private EquipmentManager equipmentManager;
    private BoneManager boneManager;
    private BankManager bankManager;

    private long startTime;
    private int cowsKilled = 0;

    @Override
    public void onStart() {
        log(">>> Iniciando Bot de Entrenamiento Cíclico <<<");
        startTime = System.currentTimeMillis();

        // Inicializar managers
        areaManager = new AreaManager(this);
        combatManager = new CombatManager(this);
        lootManager = new LootManager(this, areaManager);
        equipmentManager = new EquipmentManager(this);
        boneManager = new BoneManager(this);
        bankManager = new BankManager(this);

        log("Bot modularizado inicializado correctamente");
    }

    @Override
    public void onExit() {
        log(">>> Bot detenido - Vacas eliminadas: " + cowsKilled + " <<<");
    }

    private State getState() {
        // PRIORIDAD 1: Equipamiento
        if (equipmentManager.hasItemsToEquip()) {
            return State.EQUIP_ITEMS;
        }
        if (equipmentManager.needEquipment()) {
            return State.GET_EQUIPMENT;
        }

        // PRIORIDAD 2: Gestión de inventario
        if (bankManager.shouldBank() && !bankManager.isAtBank()) {
            return State.WALK_TO_BANK;
        }
        if (bankManager.isAtBank() && Inventory.isFull()) {
            return State.BANK;
        }

        // PRIORIDAD 3: Movimiento
        if (!areaManager.isInAnyCowArea()) {
            return State.WALK_TO_COWS;
        }

        // PRIORIDAD 4: Loot
        if (lootManager.shouldLoot()) {
            return State.LOOT;
        }

        // PRIORIDAD 5: Huesos
        if (boneManager.shouldBuryBones()) {
            return State.BURY_BONES;
        }

        // PRIORIDAD 6: Combate
        return State.COMBAT;
    }

    @Override
    public int onLoop() {
        State currentState = getState();

        switch (currentState) {
            case WALK_TO_COWS: areaManager.walkToCows(); break;
            case WALK_TO_BANK: bankManager.walkToBank(); break;
            case EQUIP_ITEMS: equipmentManager.equipItems(); break;
            case GET_EQUIPMENT: equipmentManager.getEquipmentFromTutor(); break;
            case COMBAT: attackCow(); break;
            case LOOT: lootManager.lootAllInArea(); break;
            case BURY_BONES: boneManager.buryBones(); break;
            case BANK: bankManager.bankItems(); break;
        }

        // Actualizaciones del sistema
        combatManager.update();
        areaManager.checkAreaChange();

        return Calculations.random(600, 800);
    }

    private void attackCow() {
        NPC cow = NPCs.closest(npc ->
                npc != null &&
                        npc.getName().equals("Cow") &&
                        npc.canAttack() &&
                        !npc.isInCombat() &&
                        npc.getHealthPercent() > 0
        );

        if(!areaManager.getCurrentCowArea().contains(cow)) areaManager.setCurrentCowArea(areaManager.switchCowArea(cow));

        if (cow != null && cow.interact()) {
            Sleep.sleepUntil(() -> Players.getLocal().isInCombat(), 3000);
            Sleep.sleepUntil(() -> !Players.getLocal().isInCombat(), 60000);
            Sleep.sleep(Calculations.random(1000, 2000));
            cowsKilled++;
        }
    }

    @Override
    public void onPaint(Graphics g) {
        long runTime = System.currentTimeMillis() - startTime;
        double hoursRunning = runTime / 3600000.0;
        int cowsPerHour = (int) (hoursRunning > 0 ? cowsKilled / hoursRunning : 0);

        // Panel de estadísticas
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 30, 250, 220);

        g.setColor(Color.CYAN);
        g.drawString("Cow Combat Trainer PRO", 20, 50);

        g.setColor(Color.YELLOW);
        g.drawString(combatManager.getCurrentRange().getRangeInfo(), 20, 70);
        g.drawString("Modo: " + combatManager.getCurrentRange().getCurrentMode().styleName, 20, 85);

        g.setColor(Color.ORANGE);
        g.drawString("Progreso: " + combatManager.getCurrentRange().getSkillsStatus(), 20, 100);

        g.setColor(Color.WHITE);
        g.drawString("Acción: " + getState().name(), 20, 115);
        g.drawString("Tiempo: " + formatTime(runTime), 20, 130);
        g.drawString("Vacas: " + cowsKilled + " (" + cowsPerHour + "/h)", 20, 145);
        g.drawString("Huesos: " + boneManager.getBonesBuried(), 20, 160);
        g.drawString("Cuero: " + bankManager.getHidesCollected(), 20, 175);

        // Cooldowns
        g.setColor(lootManager.getTimeUntilNextLoot() > 0 ? Color.ORANGE : Color.GREEN);
        g.drawString("Loot: " + (lootManager.getTimeUntilNextLoot() > 0 ?
                formatTime(lootManager.getTimeUntilNextLoot()) : "LISTO"), 20, 190);

        g.setColor(boneManager.getTimeUntilNextBones() > 0 ? Color.ORANGE : Color.GREEN);
        g.drawString("Huesos: " + (boneManager.getTimeUntilNextBones() > 0 ?
                formatTime(boneManager.getTimeUntilNextBones()) : "LISTO"), 20, 205);
    }

    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        return String.format("%02d:%02d:%02d", h, m % 60, s % 60);
    }
}