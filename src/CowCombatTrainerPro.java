import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.combat.CombatStyle;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;

import java.awt.*;
import java.util.List;

@ScriptManifest(author = "daniscript18", name = "Cow Combat Trainer Pro", version = 1.0, description = "Entrena combate cíclico con vacas", category = Category.COMBAT)
public class CowCombatTrainerPro extends AbstractScript {

    private enum State {
        COMBAT,
        LOOT,
        BURY_BONES,
        BANK,
        WALK_TO_COWS,
        WALK_TO_BANK,
        GET_EQUIPMENT,
        EQUIP_ITEMS
    }

    Area tutorArea = new Area(3212, 3241, 3220, 3237);

    private enum CombatMode {
        ATTACK("Attack", CombatStyle.ATTACK),
        STRENGTH("Strength", CombatStyle.STRENGTH),
        DEFENCE("Defence", CombatStyle.DEFENCE);

        private final String styleName;
        private final CombatStyle combatStyleIndex;

        CombatMode(String styleName, CombatStyle combatStyleIndex) {
            this.styleName = styleName;
            this.combatStyleIndex = combatStyleIndex;
        }
    }

    // ✅ NUEVO: Método para detectar el modo de combate actual del juego
    private CombatMode getCurrentCombatModeFromGame() {
        CombatStyle currentStyle = Combat.getCombatStyle();

        if (currentStyle == null) {
            log("⚠️ No se pudo detectar el modo de combate actual");
            return CombatMode.ATTACK; // Valor por defecto
        }

        switch (currentStyle) {
            case ATTACK:
                return CombatMode.ATTACK;
            case STRENGTH:
                return CombatMode.STRENGTH;
            case DEFENCE:
                return CombatMode.DEFENCE;
            default:
                log("⚠️ Modo de combate no reconocido: " + currentStyle);
                return CombatMode.ATTACK;
        }
    }

    // ✅ MODIFICADO: Clase LevelRange con sincronización inicial
    private class LevelRange {
        private final int minLevel;
        private final int maxLevel;
        private CombatMode currentMode;
        private final boolean[] skillsCompleted;

        public LevelRange(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;

            // ✅ SINCRONIZAR con el modo actual del juego
            this.currentMode = getCurrentCombatModeFromGame();
            this.skillsCompleted = new boolean[3];

            log("🎯 Modo inicial detectado del juego: " + this.currentMode.styleName);
        }

        // ✅ NUEVO: Método para forzar sincronización
        public void syncWithGame() {
            CombatMode gameMode = getCurrentCombatModeFromGame();
            if (gameMode != this.currentMode) {
                log("🔄 Sincronizando: " + this.currentMode.styleName + " → " + gameMode.styleName);
                this.currentMode = gameMode;
            }
        }

        // ✅ MODIFICADO: shouldSwitchMode con verificación de sincronización
        public boolean shouldSwitchMode() {
            // Primero sincronizar con el juego
            syncWithGame();

            Skill targetSkill = getSkillForMode(currentMode);
            int currentLevel = Skills.getRealLevel(targetSkill);
            boolean shouldSwitch = currentLevel >= maxLevel;

            if (shouldSwitch) {
                log("✅ " + currentMode.styleName + " alcanzó nivel " + currentLevel + " (meta: " + maxLevel + ") - Cambiando modo");
                markSkillCompleted(currentMode);
            }

            return shouldSwitch;
        }

        // ✅ MODIFICADO: switchToNextMode con verificación extra
        public void switchToNextMode() {
            CombatMode nextMode = getNextAvailableMode();
            if (nextMode != null && nextMode != currentMode) {
                log("🔄 Cambiando de " + currentMode.styleName + " a " + nextMode.styleName);
                currentMode = nextMode;
                // ✅ FORZAR el cambio en el juego inmediatamente
                setCombatStyleImmediate();
            } else if (nextMode == null) {
                log("🔄 Todos los modos completados en este rango, reiniciando ciclo...");
                resetSkillsCompleted();
                currentMode = CombatMode.ATTACK;
                setCombatStyleImmediate();
            } else {
                log("ℹ️  Ya está en el modo correcto: " + currentMode.styleName);
            }
        }

        // ... (el resto de los métodos se mantienen igual) ...
        private Skill getSkillForMode(CombatMode mode) {
            switch (mode) {
                case ATTACK: return Skill.ATTACK;
                case STRENGTH: return Skill.STRENGTH;
                case DEFENCE: return Skill.DEFENCE;
                default: return Skill.ATTACK;
            }
        }

        private void markSkillCompleted(CombatMode mode) {
            switch (mode) {
                case ATTACK:
                    skillsCompleted[0] = true;
                    log("🎯 Ataque marcado como COMPLETADO");
                    break;
                case STRENGTH:
                    skillsCompleted[1] = true;
                    log("🎯 Fuerza marcada como COMPLETADA");
                    break;
                case DEFENCE:
                    skillsCompleted[2] = true;
                    log("🎯 Defensa marcada como COMPLETADA");
                    break;
            }
        }

        private CombatMode getNextAvailableMode() {
            // Si ATTACK no está completado y no es el modo actual
            if (!skillsCompleted[0] && currentMode != CombatMode.ATTACK) {
                return CombatMode.ATTACK;
            }
            // Si STRENGTH no está completado y no es el modo actual
            if (!skillsCompleted[1] && currentMode != CombatMode.STRENGTH) {
                return CombatMode.STRENGTH;
            }
            // Si DEFENCE no está completado y no es el modo actual
            if (!skillsCompleted[2] && currentMode != CombatMode.DEFENCE) {
                return CombatMode.DEFENCE;
            }

            // Si el modo actual no está completado, mantenerlo
            if (!isCurrentModeCompleted()) {
                return currentMode;
            }

            return null;
        }

        private boolean isCurrentModeCompleted() {
            switch (currentMode) {
                case ATTACK: return skillsCompleted[0];
                case STRENGTH: return skillsCompleted[1];
                case DEFENCE: return skillsCompleted[2];
                default: return false;
            }
        }

        private void resetSkillsCompleted() {
            skillsCompleted[0] = false;
            skillsCompleted[1] = false;
            skillsCompleted[2] = false;
        }

        public boolean isRangeCompleted() {
            return Skills.getRealLevel(Skill.ATTACK) >= maxLevel &&
                    Skills.getRealLevel(Skill.STRENGTH) >= maxLevel &&
                    Skills.getRealLevel(Skill.DEFENCE) >= maxLevel;
        }

        public CombatMode getCurrentMode() {
            return currentMode;
        }

        public String getRangeInfo() {
            return minLevel + "-" + maxLevel;
        }

        public String getSkillsStatus() {
            return "A:" + (skillsCompleted[0] ? "✓" : Skills.getRealLevel(Skill.ATTACK)) +
                    " F:" + (skillsCompleted[1] ? "✓" : Skills.getRealLevel(Skill.STRENGTH)) +
                    " D:" + (skillsCompleted[2] ? "✓" : Skills.getRealLevel(Skill.DEFENCE));
        }
    }

    // ✅ NUEVO: Método para cambiar estilo inmediatamente
    private void setCombatStyleImmediate() {
        Combat.setCombatStyle(currentRange.getCurrentMode().combatStyleIndex);
        log("⚡ Estilo de combate cambiado a: " + currentRange.getCurrentMode().styleName);
        Sleep.sleep(1000); // Pausa corta para asegurar el cambio
    }

    // Sistema de múltiples áreas de vacas
    Area[] cowAreas = {
            new Area(3264, 3256, 3254, 3296),
            new Area(3253, 3279, 3245, 3298),
            new Area(3241, 3298, 3244, 3294),
            new Area(3244, 3282, 3243, 3293),
            new Area(3242, 3283, 3242, 3289),
            new Area(3241, 3284, 3241, 3287)
    };

    Area bankArea = BankLocation.LUMBRIDGE.getArea(5);

    private long startTime;
    private int cowsKilled = 0;
    private int bonesBuried = 0;
    private int hidesCollected = 0;

    // Sistema de rangos progresivos
    private final int[][] LEVEL_RANGES = {{1, 10}, {10, 20}, {20, 30}, {30, 40}, {40, 50}};
    private int currentRangeIndex = 0;
    private LevelRange currentRange;

    private final String FOOD = "Trout";

    // Variables para el sistema de múltiples áreas
    private Area currentCowArea;
    private long lastLootTime = 0;
    private long lastBoneBuryTime = 0;
    private static final long LOOT_COOLDOWN = 45000; // 45 segundos entre looters
    private static final long BONES_COOLDOWN = 60000; // 60 segundos entre enterrar huesos
    private int consecutiveNoCowsCount = 0; // Contador para cambio de área

    @Override
    public void onStart() {
        log(">>> Iniciando Bot de Entrenamiento Cíclico <<<");
        startTime = System.currentTimeMillis();
        currentRange = new LevelRange(LEVEL_RANGES[currentRangeIndex][0], LEVEL_RANGES[currentRangeIndex][1]);

        // Inicializar sistema de múltiples áreas
        currentCowArea = getRandomCowArea();

        log("Rango inicial: " + currentRange.getRangeInfo() + " - Modo: " + currentRange.getCurrentMode().styleName);
        log("Área inicial: " + currentCowArea);
    }

    @Override
    public void onExit() {
        log(">>> Bot detenido - Vacas eliminadas: " + cowsKilled + " <<<");
    }

    // Métodos para el sistema de múltiples áreas
    private Area getRandomCowArea() {
        return cowAreas[Calculations.random(0, cowAreas.length - 1)];
    }

    private boolean isInAnyCowArea() {
        for (Area area : cowAreas) {
            if (area.contains(Players.getLocal())) {
                return true;
            }
        }
        return false;
    }

    // Método para verificar si hay vacas disponibles en el área actual
    private boolean hasCowsInCurrentArea() {
        return NPCs.closest(npc ->
                npc != null &&
                        npc.getName().equals("Cow") &&
                        npc.canAttack() &&
                        !npc.isInCombat() &&
                        npc.getHealthPercent() > 0 &&
                        currentCowArea.contains(npc)
        ) != null;
    }

    // MODIFICADO: Cambio de área solo cuando no hay vacas
    private void checkAreaChange() {
        if (!hasCowsInCurrentArea()) {
            consecutiveNoCowsCount++;

            // Cambiar de área después de 3 ciclos sin vacas
            if (consecutiveNoCowsCount >= 3) {
                Area newArea = getRandomCowArea();
                if (!newArea.equals(currentCowArea)) {
                    currentCowArea = newArea;
                    log("Cambiando de área - No hay vacas en: " + currentCowArea);
                    log("Nueva área: " + newArea);
                }
                consecutiveNoCowsCount = 0; // Resetear contador
            }
        } else {
            consecutiveNoCowsCount = 0; // Resetear si hay vacas
        }
    }

    // Método para verificar si hay loot en el área
    private boolean isLootInArea() {
        return GroundItems.closest(item ->
                item != null &&
                        (item.getName().equals("Cowhide") || item.getName().equals("Bones")) &&
                        currentCowArea.contains(item) // Solo items en el área actual
        ) != null;
    }

    // Método para obtener todo el loot del área
    private void lootAllInArea() {
        List<GroundItem> lootItems = GroundItems.all(item ->
                item != null &&
                        (item.getName().equals("Cowhide") || item.getName().equals("Bones")) &&
                        currentCowArea.contains(item) &&
                        item.exists()
        );

        if (!lootItems.isEmpty()) {
            // Ordenar por distancia (más cercano primero)
            lootItems.sort((i1, i2) ->
                    (int)(i1.distance() - i2.distance()));

            // Looter máximo 3-5 items por sesión
            int maxLoot = Calculations.random(3, 5);
            int looted = 0;

            for (GroundItem item : lootItems) {
                if (looted >= maxLoot || Inventory.isFull()) break;

                if (item.interact()) {
                    log("Recogiendo: " + item.getName());
                    Sleep.sleepUntil(() -> !item.exists(), 2000);
                    Sleep.sleep(Calculations.random(600, 1000));
                    looted++;
                }
            }

            log("Sesión de loot completada: " + looted + " items recogidos");
        }

        // Actualizar cooldown sin importar si hubo loot o no
        lastLootTime = System.currentTimeMillis();
    }

    // MODIFICADO: Estado getState con nuevo sistema de loot
    private State getState() {
        // PRIORIDAD 1: Equipamiento y recuperación
        if (hasItemsToEquip()) {
            return State.EQUIP_ITEMS;
        }

        if (needEquipment() && !tutorArea.contains(Players.getLocal())) {
            return State.GET_EQUIPMENT;
        }

        if (needEquipment() && tutorArea.contains(Players.getLocal())) {
            return State.GET_EQUIPMENT;
        }

        // PRIORIDAD 2: Gestión de inventario
        if (Inventory.isFull() && !bankArea.contains(Players.getLocal()) && !Inventory.contains("Bones")) {
            return State.WALK_TO_BANK;
        }

        if (bankArea.contains(Players.getLocal()) && Inventory.isFull()) {
            return State.BANK;
        }

        // PRIORIDAD 3: Movimiento
        if (!isInAnyCowArea()) {
            return State.WALK_TO_COWS;
        }

        // PRIORIDAD 4: Loot por ÁREA (no por vaca específica)
        long currentTime = System.currentTimeMillis();
        boolean shouldLoot = currentTime - lastLootTime > LOOT_COOLDOWN &&
                Calculations.random(1, 4) == 1; // 25% de probabilidad

        if (shouldLoot && isLootInArea() && !Inventory.isFull()) {
            return State.LOOT;
        }

        // PRIORIDAD 5: Enterrar huesos OCASIONALMENTE
        boolean shouldBuryBones = currentTime - lastBoneBuryTime > BONES_COOLDOWN &&
                Calculations.random(1, 6) == 1 && // 16% de probabilidad
                Inventory.contains("Bones");

        if (shouldBuryBones) {
            return State.BURY_BONES;
        }

        // PRIORIDAD 6: COMBATE (estado por defecto)
        return State.COMBAT;
    }

    @Override
    public int onLoop() {
        switch (getState()) {
            case WALK_TO_COWS:
                walkToCows();
                break;
            case WALK_TO_BANK:
                walkToBank();
                break;
            case EQUIP_ITEMS:
                equipItems();
                break;
            case GET_EQUIPMENT:
                getEquipmentFromTutor();
                break;
            case COMBAT:
                attackCow();
                break;
            case LOOT:
                lootItems();
                break;
            case BURY_BONES:
                buryBones();
                break;
            case BANK:
                bankItems();
                break;
        }
        updateCombatSystem();
        checkAreaChange();
        return Calculations.random(600, 800);
    }

    private boolean needEquipment() {
        // Verificar si tenemos al menos una espada y escudo
        boolean hasWeapon = Inventory.contains("Training sword") || Equipment.contains("Training sword");
        boolean hasShield = Inventory.contains("Training shield") || Equipment.contains("Training shield");

        // Si no tenemos arma O no tenemos escudo, necesitamos equipo
        return !hasWeapon || !hasShield;
    }

    private boolean hasItemsToEquip() {
        return Inventory.contains("Training sword") || Inventory.contains("Training shield");
    }

    private void equipItems() {
        // Prioridad de equipamiento (en orden)
        String[] equipmentPriority = {"Training sword", "Training shield"};

        for (String itemName : equipmentPriority) {
            if (Inventory.contains(itemName) && !Equipment.contains(itemName)) {
                if (Inventory.interact(itemName)) {
                    log("Equipando: " + itemName);
                    Sleep.sleep(Calculations.random(800, 1200));
                    break; // Equipar un item por ciclo para ser más natural
                }
            }
        }
    }

    private void getEquipmentFromTutor() {
        if (!tutorArea.contains(Players.getLocal())) {
            // Caminar hacia el tutor
            if (Walking.shouldWalk()) {
                Walking.walk(tutorArea.getCenter());
                Sleep.sleepUntil(() -> tutorArea.contains(Players.getLocal()), 8000);
            }
            return;
        }

        // Buscar al Melee Combat Tutor
        NPC tutor = NPCs.closest(npc ->
                npc != null &&
                        npc.getName().equals("Melee combat tutor") &&
                        npc.interact()
        );

        if (tutor != null) {
            if (Dialogues.inDialogue()) {
                // Avanzar en el diálogo
                if (Dialogues.canContinue()) {
                    Dialogues.continueDialogue();
                    Sleep.sleep(Calculations.random(1200, 2000));
                }

                // Buscar opciones para obtener equipo
                if (Dialogues.areOptionsAvailable()) {
                    if (Dialogues.chooseOption(4)) {
                        Sleep.sleep(Calculations.random(1500, 2500));
                    }
                }
            } else {
                // Iniciar conversación
                if (tutor.interact()) {
                    Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                }
            }
        }
    }

    // ✅ MODIFICADO: updateCombatSystem con sincronización
    private void updateCombatSystem() {
        // Sincronizar siempre con el juego
        currentRange.syncWithGame();

        // Debug info
        if (Calculations.random(1, 15) == 1) {
            log("=== SISTEMA DE COMBATE ===");
            log("📍 Rango: " + currentRange.getRangeInfo());
            log("🎯 Modo Bot: " + currentRange.getCurrentMode().styleName);
            log("🎮 Modo Juego: " + getCurrentCombatModeFromGame().styleName);
            log("📊 Progreso: " + currentRange.getSkillsStatus());
            log("📈 Niveles - A:" + Skills.getRealLevel(Skill.ATTACK) +
                    " F:" + Skills.getRealLevel(Skill.STRENGTH) +
                    " D:" + Skills.getRealLevel(Skill.DEFENCE));
            log("========================");
        }

        // Verificar si necesitamos avanzar al siguiente rango
        if (currentRange.isRangeCompleted()) {
            currentRangeIndex++;
            if (currentRangeIndex < LEVEL_RANGES.length) {
                int newMin = LEVEL_RANGES[currentRangeIndex][0];
                int newMax = LEVEL_RANGES[currentRangeIndex][1];
                currentRange = new LevelRange(newMin, newMax);
                log("🎉 ¡RANGO COMPLETADO! Avanzando a: " + currentRange.getRangeInfo());
                setCombatStyleImmediate();
            } else {
                log("🏆 ¡TODOS LOS RANGOS COMPLETADOS!");
                stop();
            }
        } else {
            // Verificar si necesitamos cambiar de modo
            if (currentRange.shouldSwitchMode()) {
                log("🔄 ¡SOLICITUD DE CAMBIO DETECTADA!");
                currentRange.switchToNextMode();
            }
        }
    }

    // MEJORADO: Método de combate con cambio de área inteligente
    private void attackCow() {
        NPC cow = NPCs.closest(npc ->
                npc != null &&
                        npc.getName().equals("Cow") &&
                        npc.canAttack() &&
                        !npc.isInCombat() &&
                        npc.getHealthPercent() > 0 &&
                        currentCowArea.contains(npc)
        );

        if (cow != null && cow.interact()) {
            log("Atacando vaca en área: " + currentCowArea);
            Sleep.sleepUntil(() -> Players.getLocal().isInCombat(), 3000);

            // Esperar a que termine el combate
            Sleep.sleepUntil(() -> !Players.getLocal().isInCombat(), 30000);

            // Pequeña pausa humana después del combate
            Sleep.sleep(Calculations.random(1000, 2000));
            cowsKilled++;
        } else {
            // No hay vacas atacables, incrementar contador para cambio de área
            consecutiveNoCowsCount++;
        }
    }

    // MODIFICADO: Método lootItems para usar el nuevo sistema
    private void lootItems() {
        log("Iniciando sesión de loot en el área...");
        lootAllInArea();
    }

    // MEJORADO: Enterrar huesos rápido
    private void buryBones() {
        int bonesToBury = Calculations.random(3, 8); // Enterrar 3-8 huesos por sesión
        int bonesBuriedThisSession = 0;

        log("Iniciando sesión de enterrar huesos...");

        while (bonesBuriedThisSession < bonesToBury && Inventory.contains("Bones")) {
            if (Inventory.interact("Bones", "Bury")) {
                bonesBuried++;
                bonesBuriedThisSession++;
                log("Enterrando hueso " + bonesBuriedThisSession + "/" + bonesToBury);

                // Pausa variable entre huesos (más natural)
                Sleep.sleep(Calculations.random(600, 1200));

                // Pequeña probabilidad de pausa más larga (comportamiento humano)
                if (Calculations.random(1, 10) == 1) {
                    Sleep.sleep(Calculations.random(1500, 2500));
                }
            } else {
                break; // Si falla la interacción, salir del bucle
            }
        }

        log("Sesión de huesos completada: " + bonesBuriedThisSession + " huesos enterrados");
        lastBoneBuryTime = System.currentTimeMillis(); // Actualizar cooldown
    }

    private void bankItems() {
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 3000);
            }
        } else {
            if (Inventory.contains("Cowhide")) {
                int hides = Inventory.count("Cowhide");
                Bank.depositAll("Cowhide");
                hidesCollected += hides;
                Sleep.sleep(Calculations.random(800, 1200));
            }

            Bank.depositAllExcept(item ->
                    item != null && (
                            item.getName().contains(FOOD)
                    )
            );

            if (!Inventory.contains(FOOD) && Bank.contains(FOOD)) {
                Bank.withdraw(FOOD, 10);
                Sleep.sleep(Calculations.random(600, 1000));
            }

            Bank.close();
        }
    }

    private void walkToCows() {
        if (Walking.shouldWalk(Calculations.random(2, 4))) {
            Walking.walk(currentCowArea.getCenter());
            Sleep.sleepUntil(() -> currentCowArea.contains(Players.getLocal()), 8000);
        }
    }

    private void walkToBank() {
        if (Walking.shouldWalk(Calculations.random(2, 4))) {
            Walking.walk(bankArea.getCenter());
            Sleep.sleepUntil(() -> bankArea.contains(Players.getLocal()), 8000);
        }
    }

    // ✅ ACTUALIZADO: Panel de paint con nueva información
    @Override
    public void onPaint(Graphics g) {
        long runTime = System.currentTimeMillis() - startTime;
        double hoursRunning = runTime / 3600000.0;

        int cowsPerHour = (int) (hoursRunning > 0 ? cowsKilled / hoursRunning : 0);

        // Información de cooldowns
        long timeUntilNextLoot = Math.max(0, (lastLootTime + LOOT_COOLDOWN) - System.currentTimeMillis());
        long timeUntilNextBones = Math.max(0, (lastBoneBuryTime + BONES_COOLDOWN) - System.currentTimeMillis());
        boolean hasCows = hasCowsInCurrentArea();

        // Panel de estadísticas mejorado
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 30, 250, 220); // Aumentado para nueva información

        g.setColor(Color.CYAN);
        g.drawString("Cow Combat Trainer PRO", 20, 50);

        g.setColor(Color.YELLOW);
        g.drawString("Rango: " + currentRange.getRangeInfo(), 20, 70);
        g.drawString("Modo: " + currentRange.getCurrentMode().styleName, 20, 85);

        // ✅ NUEVA LÍNEA: Estado de habilidades
        g.setColor(Color.ORANGE);
        g.drawString("Progreso: " + currentRange.getSkillsStatus(), 20, 100);

        g.setColor(Color.WHITE);
        g.drawString("Acción: " + getState().name(), 20, 115);
        g.drawString("Tiempo: " + formatTime(runTime), 20, 130);
        g.drawString("Vacas: " + cowsKilled + " (" + cowsPerHour + "/h)", 20, 145);
        g.drawString("Huesos: " + bonesBuried, 20, 160);
        g.drawString("Cuero: " + hidesCollected, 20, 175);

        // Información de cooldowns
        g.setColor(timeUntilNextLoot > 0 ? Color.ORANGE : Color.GREEN);
        g.drawString("Loot: " + (timeUntilNextLoot > 0 ? formatTime(timeUntilNextLoot) : "LISTO"), 20, 190);

        g.setColor(timeUntilNextBones > 0 ? Color.ORANGE : Color.GREEN);
        g.drawString("Huesos: " + (timeUntilNextBones > 0 ? formatTime(timeUntilNextBones) : "LISTO"), 20, 205);

        g.setColor(hasCows ? Color.GREEN : Color.RED);
        g.drawString("VacAS: " + (hasCows ? "SÍ" : "NO"), 150, 190);

        // Niveles actuales
        g.setColor(getLevelColor(Skill.ATTACK));
        g.drawString("Ataque: " + Skills.getRealLevel(Skill.ATTACK), 150, 115);

        g.setColor(getLevelColor(Skill.STRENGTH));
        g.drawString("Fuerza: " + Skills.getRealLevel(Skill.STRENGTH), 150, 130);

        g.setColor(getLevelColor(Skill.DEFENCE));
        g.drawString("Defensa: " + Skills.getRealLevel(Skill.DEFENCE), 150, 145);
    }

    private Color getLevelColor(Skill skill) {
        int level = Skills.getRealLevel(skill);
        int targetLevel = LEVEL_RANGES[currentRangeIndex][1];

        if (level >= targetLevel) return Color.GREEN;
        if (level >= targetLevel - 3) return Color.YELLOW;
        return Color.WHITE;
    }

    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        return String.format("%02d:%02d:%02d", h, m % 60, s % 60);
    }
}