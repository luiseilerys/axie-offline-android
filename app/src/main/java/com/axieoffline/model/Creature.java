package com.axieoffline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Creature {

    public enum AxieClass {
        BEAST, PLANT, REPTILE, AQUATIC, BIRD, BUG, MECH, DAWN, DUSK
    }

    public enum BodyPart {
        EYES, EARS, HORN, MOUTH, BACK, TAIL
    }

    public static class Gene {
        public String partId;
        public AxieClass partClass;
        public String name;
        public int variant;

        public Gene(String partId, AxieClass partClass, String name, int variant) {
            this.partId = partId;
            this.partClass = partClass;
            this.name = name;
            this.variant = variant;
        }

        public Gene copy() {
            return new Gene(partId, partClass, name, variant);
        }
    }

    public static class PartGenes {
        public Gene dominant;
        public Gene recessive1;
        public Gene recessive2;

        public PartGenes(Gene d, Gene r1, Gene r2) {
            this.dominant = d;
            this.recessive1 = r1;
            this.recessive2 = r2;
        }

        public PartGenes copy() {
            return new PartGenes(dominant.copy(), recessive1.copy(), recessive2.copy());
        }
    }

    public static class Card {
        public String id;
        public String name;
        public BodyPart part;
        public AxieClass cardClass;
        public int energyCost;
        public int attack;
        public int shield;
        public String effect;
        public boolean retain;

        public Card(String id, String name, BodyPart part, AxieClass cardClass,
                    int energyCost, int attack, int shield, String effect) {
            this.id = id;
            this.name = name;
            this.part = part;
            this.cardClass = cardClass;
            this.energyCost = energyCost;
            this.attack = attack;
            this.shield = shield;
            this.effect = effect;
            this.retain = false;
        }

        public Card copy() {
            Card c = new Card(id, name, part, cardClass, energyCost, attack, shield, effect);
            c.retain = retain;
            return c;
        }
    }

    public String uid;
    public String name;
    public AxieClass axieClass;
    public int level;
    public int hp;
    public int maxHp;
    public int speed;
    public int skill;
    public int morale;
    public int breedCount;
    public int axp;
    public int ascension;
    public PartGenes[] genes;
    public List<Card> cards;
    public String parent1Uid;
    public String parent2Uid;
    public String litterId;
    public boolean isEgg;
    public long eggMatureTime;

    public int currentHp;
    public int currentShield;
    public int rage;
    public int leaf;
    public int bubble;
    public boolean taunt;
    public int fearTurns;
    public boolean fragile;
    public boolean furyForm;

    private static final Random RNG = new Random();

    public Creature() {
        genes = new PartGenes[6];
        cards = new ArrayList<>();
        level = 1;
        breedCount = 0;
        axp = 0;
        ascension = 0;
        isEgg = false;
    }

    public static Creature createStarter(AxieClass cls, String name) {
        Creature c = new Creature();
        c.uid = "c_" + System.currentTimeMillis() + "_" + RNG.nextInt(99999);
        c.name = name;
        c.axieClass = cls;
        applyClassBonuses(c);
        generateDefaultGenes(c);
        generateCards(c);
        c.currentHp = c.maxHp;
        return c;
    }

    private static void applyClassBonuses(Creature c) {
        c.maxHp = 30;
        c.speed = 30;
        c.skill = 30;
        c.morale = 30;

        switch (c.axieClass) {
            case PLANT: c.maxHp += 3; c.morale += 1; break;
            case AQUATIC: c.speed += 3; c.maxHp += 1; break;
            case BEAST: c.maxHp += 1; c.skill += 3; break;
            case BIRD: c.speed += 3; c.morale += 1; break;
            case BUG: c.skill += 3; c.morale += 1; break;
            case REPTILE: c.maxHp += 3; c.skill += 1; break;
            case MECH: c.skill += 2; c.speed += 2; break;
            case DAWN: c.morale += 3; c.speed += 1; break;
            case DUSK: c.maxHp += 2; c.morale += 2; break;
        }
        c.hp = c.maxHp;
    }

    private static final String[] SLOT_NAMES = {"eyes", "ears", "horn", "mouth", "back", "tail"};

    private static void generateDefaultGenes(Creature c) {
        BodyPart[] parts = BodyPart.values();
        String cls = c.axieClass.name().toLowerCase();
        for (int i = 0; i < 6; i++) {
            int v = RNG.nextInt(12);
            int v1 = RNG.nextInt(12);
            int v2 = RNG.nextInt(12);
            String slot = SLOT_NAMES[i];
            Gene d = new Gene(slot + "_" + cls + "_" + v, c.axieClass, slot + v, v);
            Gene r1 = new Gene(slot + "_" + randomClassName() + "_" + v1, randomClass(), "rec1", v1);
            Gene r2 = new Gene(slot + "_" + randomClassName() + "_" + v2, randomClass(), "rec2", v2);
            c.genes[i] = new PartGenes(d, r1, r2);
        }
    }

    private static AxieClass randomClass() {
        AxieClass[] vals = AxieClass.values();
        return vals[RNG.nextInt(vals.length)];
    }

    private static String randomClassName() {
        return randomClass().name().toLowerCase();
    }

    private static void generateCards(Creature c) {
        c.cards.clear();
        c.cards.add(makeCard("horn_atk", "Horn Strike", BodyPart.HORN, c.axieClass, 1, 60, 0, "attack"));
        c.cards.add(makeCard("mouth_bite", "Bite", BodyPart.MOUTH, c.axieClass, 1, 40, 0, "attack"));
        c.cards.add(makeCard("back_shell", "Shell", BodyPart.BACK, c.axieClass, 1, 0, 50, "shield"));
        c.cards.add(makeCard("tail_sweep", "Tail Sweep", BodyPart.TAIL, c.axieClass, 2, 80, 0, "attack"));
        c.cards.add(makeCard("eyes_focus", "Focus", BodyPart.EYES, c.axieClass, 0, 0, 0, "draw"));
        c.cards.add(makeCard("ears_listen", "Listen", BodyPart.EARS, c.axieClass, 1, 0, 30, "shield"));

        switch (c.axieClass) {
            case PLANT:
                c.cards.add(makeCard("leaf_heal", "Photo", BodyPart.BACK, c.axieClass, 1, 0, 0, "leaf"));
                break;
            case BEAST:
                c.cards.add(makeCard("rage_roar", "Roar", BodyPart.MOUTH, c.axieClass, 1, 30, 0, "rage"));
                break;
            case AQUATIC:
                c.cards.add(makeCard("bubble", "Bubble", BodyPart.TAIL, c.axieClass, 1, 20, 0, "bubble"));
                break;
            default:
                c.cards.add(makeCard("special", "Special", BodyPart.HORN, c.axieClass, 2, 50, 20, "attack"));
                break;
        }
    }

    private static Card makeCard(String id, String name, BodyPart part, AxieClass cls,
                                 int cost, int atk, int sh, String effect) {
        return new Card(id, name, part, cls, cost, atk, sh, effect);
    }

    public void resetBattleState() {
        currentHp = maxHp + level + (ascension * 2);
        currentShield = 0;
        rage = 0;
        leaf = 0;
        bubble = 0;
        taunt = false;
        fearTurns = 0;
        fragile = false;
        furyForm = false;
    }

    public int getBattleMaxHp() {
        return maxHp + level + (ascension * 2);
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public float getPurity() {
        int matches = 0;
        int total = 0;
        for (PartGenes pg : genes) {
            if (pg == null) continue;
            total += 2;
            if (pg.recessive1.partId.equals(pg.dominant.partId)) matches++;
            if (pg.recessive2.partId.equals(pg.dominant.partId)) matches++;
        }
        return total == 0 ? 0 : (float) matches / total;
    }

    public static boolean isStrongAgainst(AxieClass attacker, AxieClass defender) {
        int a = groupOf(attacker);
        int d = groupOf(defender);
        return (a + 1) % 3 == d;
    }

    public static boolean isWeakAgainst(AxieClass attacker, AxieClass defender) {
        int a = groupOf(attacker);
        int d = groupOf(defender);
        return (d + 1) % 3 == a;
    }

    private static int groupOf(AxieClass c) {
        switch (c) {
            case PLANT:
            case REPTILE:
            case DUSK:
                return 0;
            case AQUATIC:
            case BIRD:
            case DAWN:
                return 1;
            default:
                return 2;
        }
    }

    public int getClassColor() {
        switch (axieClass) {
            case BEAST: return 0xFFF59E0B;
            case PLANT: return 0xFF22C55E;
            case REPTILE: return 0xFF10B981;
            case AQUATIC: return 0xFF3B82F6;
            case BIRD: return 0xFF8B5CF6;
            case BUG: return 0xFFA3E635;
            case MECH: return 0xFF94A3B8;
            case DAWN: return 0xFFFBBF24;
            case DUSK: return 0xFF6366F1;
            default: return 0xFFFFFFFF;
        }
    }
}
