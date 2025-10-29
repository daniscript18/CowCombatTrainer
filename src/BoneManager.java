import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Sleep;

public class BoneManager {
    private final CowCombatTrainerPro script;
    private long lastBoneBuryTime = 0;
    private static final long BONES_COOLDOWN = 60000;
    private int bonesBuried = 0;

    public BoneManager(CowCombatTrainerPro script) {
        this.script = script;
    }

    public boolean shouldBuryBones() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastBoneBuryTime > BONES_COOLDOWN &&
                Calculations.random(1, 6) == 1 &&
                Inventory.contains("Bones");
    }

    public void buryBones() {
        int bonesToBury = Calculations.random(3, 8);
        int bonesBuriedThisSession = 0;

        script.log("Iniciando sesión de enterrar huesos...");

        while (bonesBuriedThisSession < bonesToBury && Inventory.contains("Bones")) {
            if (Inventory.interact("Bones", "Bury")) {
                bonesBuried++;
                bonesBuriedThisSession++;
                script.log("Enterrando hueso " + bonesBuriedThisSession + "/" + bonesToBury);
                Sleep.sleep(Calculations.random(600, 1200));

                if (Calculations.random(1, 10) == 1) {
                    Sleep.sleep(Calculations.random(1500, 2500));
                }
            } else {
                break;
            }
        }

        script.log("Sesión de huesos completada: " + bonesBuriedThisSession + " huesos enterrados");
        lastBoneBuryTime = System.currentTimeMillis();
    }

    public int getBonesBuried() {
        return bonesBuried;
    }

    public long getTimeUntilNextBones() {
        return Math.max(0, (lastBoneBuryTime + BONES_COOLDOWN) - System.currentTimeMillis());
    }
}