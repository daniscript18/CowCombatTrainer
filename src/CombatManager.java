import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.combat.CombatStyle;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.utilities.Sleep;

import static org.dreambot.api.utilities.Logger.log;

public class CombatManager {
    public enum CombatMode {
        ATTACK("Attack", CombatStyle.ATTACK),
        STRENGTH("Strength", CombatStyle.STRENGTH),
        DEFENCE("Defence", CombatStyle.DEFENCE);

        final String styleName;
        private final CombatStyle combatStyleIndex;

        CombatMode(String styleName, CombatStyle combatStyleIndex) {
            this.styleName = styleName;
            this.combatStyleIndex = combatStyleIndex;
        }
    }

    private CombatMode getCurrentCombatModeFromGame() {
        CombatStyle currentStyle = Combat.getCombatStyle();

        if (currentStyle == null) {
            return CombatMode.ATTACK;
        }

        switch (currentStyle) {
            case STRENGTH:
                return CombatMode.STRENGTH;
            case DEFENCE:
                return CombatMode.DEFENCE;
            default:
                return CombatMode.ATTACK;
        }
    }

    public class LevelRange {
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
                case STRENGTH:
                    return Skill.STRENGTH;
                case DEFENCE:
                    return Skill.DEFENCE;
                default:
                    return Skill.ATTACK;
            }
        }

        private void markSkillCompleted(CombatMode mode) {
            switch (mode) {
                case ATTACK:
                    skillsCompleted[0] = true;
                    break;
                case STRENGTH:
                    skillsCompleted[1] = true;
                    break;
                case DEFENCE:
                    skillsCompleted[2] = true;
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
                case ATTACK:
                    return skillsCompleted[0];
                case STRENGTH:
                    return skillsCompleted[1];
                case DEFENCE:
                    return skillsCompleted[2];
                default:
                    return false;
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

    private final CowCombatTrainerPro script;
    private LevelRange currentRange;
    private int currentRangeIndex = 0;
    private final int[][] LEVEL_RANGES = {{1, 10}, {10, 20}, {20, 30}, {30, 40}, {40, 50}};

    public CombatManager(CowCombatTrainerPro script) {
        this.script = script;
        this.currentRange = new LevelRange(LEVEL_RANGES[currentRangeIndex][0], LEVEL_RANGES[currentRangeIndex][1]);
    }

    public void update() {
        currentRange.syncWithGame();

        if (currentRange.isRangeCompleted()) {
            advanceToNextRange();
        } else if (currentRange.shouldSwitchMode()) {
            currentRange.switchToNextMode();
        }
    }

    private void advanceToNextRange() {
        currentRangeIndex++;
        if (currentRangeIndex < LEVEL_RANGES.length) {
            int newMin = LEVEL_RANGES[currentRangeIndex][0];
            int newMax = LEVEL_RANGES[currentRangeIndex][1];
            currentRange = new LevelRange(newMin, newMax);
            script.log("🎉 ¡RANGO COMPLETADO! Avanzando a: " + currentRange.getRangeInfo());
            setCombatStyleImmediate();
        } else {
            script.log("🏆 ¡TODOS LOS RANGOS COMPLETADOS!");
            script.stop();
        }
    }

    private void setCombatStyleImmediate() {
        Combat.setCombatStyle(currentRange.getCurrentMode().combatStyleIndex);
        script.log("⚡ Estilo de combate cambiado a: " + currentRange.getCurrentMode().styleName);
        Sleep.sleep(1000);
    }

    public LevelRange getCurrentRange() {
        return currentRange;
    }

    public String getDebugInfo() {
        return "📍 Rango: " + currentRange.getRangeInfo() + "\n" +
                "🎯 Modo: " + currentRange.getCurrentMode().styleName + "\n" +
                "📊 Progreso: " + currentRange.getSkillsStatus() + "\n" +
                "📈 Niveles - A:" + Skills.getRealLevel(Skill.ATTACK) +
                " F:" + Skills.getRealLevel(Skill.STRENGTH) +
                " D:" + Skills.getRealLevel(Skill.DEFENCE);
    }
}