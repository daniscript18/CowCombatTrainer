import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;

public class AreaManager {
    private final CowCombatTrainerPro script;
    private final Area[] cowAreas;
    private Area currentCowArea;
    private int consecutiveNoCowsCount = 0;

    public AreaManager(CowCombatTrainerPro script) {
        this.script = script;
        this.cowAreas = new Area[]{
                new Area(3264, 3256, 3254, 3296),
                new Area(3253, 3279, 3245, 3298),
                new Area(3241, 3298, 3244, 3294),
                new Area(3244, 3282, 3243, 3293),
                new Area(3242, 3283, 3242, 3289),
                new Area(3241, 3284, 3241, 3287)
        };
        this.currentCowArea = getRandomCowArea();
    }

    public Area switchCowArea(NPC cow) {
        for (Area cowArea : cowAreas) {
            if (cowArea.contains(cow)) return cowArea;
        }
        return null;
    }

    public void checkAreaChange() {
        if (!hasCowsInCurrentArea()) {
            consecutiveNoCowsCount++;
            if (consecutiveNoCowsCount >= 1) {
                Area newArea = getRandomCowArea();
                if (!newArea.equals(currentCowArea)) {
                    currentCowArea = newArea;
                    script.log("Cambiando de área - No hay vacas");
                    script.log("Nueva área: " + newArea);
                }
                consecutiveNoCowsCount = 0;
            }
        } else {
            consecutiveNoCowsCount = 0;
        }
    }

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

    private Area getRandomCowArea() {
        return cowAreas[Calculations.random(0, cowAreas.length - 1)];
    }

    public boolean isInAnyCowArea() {
        for (Area area : cowAreas) {
            if (area.contains(Players.getLocal())) {
                return true;
            }
        }
        return false;
    }

    public Area getCurrentCowArea() {
        return currentCowArea;
    }

    public void setCurrentCowArea(Area area) {
        currentCowArea = area;
        return;
    }

    public void walkToCows() {
        if (Walking.shouldWalk()) {
            if(currentCowArea == null) currentCowArea = getRandomCowArea();
            Walking.walk(currentCowArea);
            Sleep.sleepUntil(() -> currentCowArea.contains(Players.getLocal()), 1500);
        }
    }
}