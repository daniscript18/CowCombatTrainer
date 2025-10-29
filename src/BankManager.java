import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;

public class BankManager {
    private final CowCombatTrainerPro script;
    private final Area bankArea = BankLocation.LUMBRIDGE.getArea(5);
    private int hidesCollected = 0;
    private final String FOOD = "Trout";

    public BankManager(CowCombatTrainerPro script) {
        this.script = script;
    }

    public boolean shouldBank() {
        return Inventory.isFull() && !Inventory.contains("Bones");
    }

    public boolean isAtBank() {
        return bankArea.contains(Players.getLocal());
    }

    public void bankItems() {
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 3000);
            }
        } else {
            depositItems();
            withdrawFood();
            Bank.close();
        }
    }

    private void depositItems() {
        if (Inventory.contains("Cowhide")) {
            int hides = Inventory.count("Cowhide");
            Bank.depositAll("Cowhide");
            hidesCollected += hides;
            Sleep.sleep(Calculations.random(800, 1200));
        }
        Bank.depositAllExcept(item ->
                item != null && item.getName().contains(FOOD)
        );
    }

    private void withdrawFood() {
        if (!Inventory.contains(FOOD) && Bank.contains(FOOD)) {
            Bank.withdraw(FOOD, 10);
            Sleep.sleep(Calculations.random(600, 1000));
        }
    }

    public void walkToBank() {
        if (Walking.shouldWalk()) {
            Walking.walk(bankArea.getCenter());
            Sleep.sleepUntil(() -> bankArea.contains(Players.getLocal()), 1500);
        }
    }

    public int getHidesCollected() {
        return hidesCollected;
    }

    public Area getBankArea() {
        return bankArea;
    }
}