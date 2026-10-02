package com.axieoffline.model;

import java.util.Random;

public class BreedingSystem {

    private static final Random RNG = new Random();
    private static final int[] BREED_COSTS = {900, 1350, 2250, 3600, 5850, 9450, 15300};
    private static final String[] SLOT_NAMES = {"eyes", "ears", "horn", "mouth", "back", "tail"};

    public static int getBreedCost(Creature a, Creature b) {
        int costA = a.breedCount < 7 ? BREED_COSTS[a.breedCount] : Integer.MAX_VALUE;
        int costB = b.breedCount < 7 ? BREED_COSTS[b.breedCount] : Integer.MAX_VALUE;
        return costA + costB;
    }

    public static boolean canBreed(Creature a, Creature b) {
        if (a == null || b == null) return false;
        if (a.uid.equals(b.uid)) return false;
        if (a.breedCount >= 7 || b.breedCount >= 7) return false;
        if (a.isEgg || b.isEgg) return false;
        if (a.litterId != null && a.litterId.equals(b.litterId)) return false;
        if (a.uid.equals(b.parent1Uid) || a.uid.equals(b.parent2Uid)) return false;
        if (b.uid.equals(a.parent1Uid) || b.uid.equals(a.parent2Uid)) return false;
        return true;
    }

    public static Creature breed(Creature parentA, Creature parentB) {
        if (!canBreed(parentA, parentB)) return null;

        Creature child = new Creature();
        child.uid = "c_" + System.currentTimeMillis() + "_" + RNG.nextInt(99999);
        child.parent1Uid = parentA.uid;
        child.parent2Uid = parentB.uid;
        child.litterId = "lit_" + System.currentTimeMillis();
        child.level = 1;
        child.breedCount = 0;
        child.isEgg = true;
        child.eggMatureTime = System.currentTimeMillis() + 30_000L;

        child.axieClass = RNG.nextBoolean() ? parentA.axieClass : parentB.axieClass;
        child.name = child.axieClass.name() + " Offspring";

        child.genes = new Creature.PartGenes[6];
        for (int i = 0; i < 6; i++) {
            Creature.Gene d = inheritGene(parentA.genes[i], parentB.genes[i]);
            Creature.Gene r1 = inheritGene(parentA.genes[i], parentB.genes[i]);
            Creature.Gene r2 = inheritGene(parentA.genes[i], parentB.genes[i]);
            // 7% mystic mutation — still uses a visible variant of the class
            if (RNG.nextFloat() < 0.07f) {
                int v = RNG.nextInt(12);
                String slot = SLOT_NAMES[i];
                String cls = d.partClass.name().toLowerCase();
                d = new Creature.Gene(slot + "_" + cls + "_" + v, d.partClass, "Mystic " + d.name, v);
            }
            child.genes[i] = new Creature.PartGenes(d, r1, r2);
        }

        child.maxHp = 30;
        child.speed = 30;
        child.skill = 30;
        child.morale = 30;
        applyClassBonus(child);
        child.maxHp = (child.maxHp + parentA.maxHp + parentB.maxHp) / 3;
        child.speed = (child.speed + parentA.speed + parentB.speed) / 3;
        child.skill = (child.skill + parentA.skill + parentB.skill) / 3;
        child.morale = (child.morale + parentA.morale + parentB.morale) / 3;
        child.hp = child.maxHp;

        generateCardsFromGenes(child);

        parentA.breedCount++;
        parentB.breedCount++;

        return child;
    }

    private static Creature.Gene inheritGene(Creature.PartGenes a, Creature.PartGenes b) {
        float r = RNG.nextFloat();
        Creature.PartGenes src = RNG.nextBoolean() ? a : b;
        if (src == null) {
            int v = RNG.nextInt(12);
            return new Creature.Gene("eyes_beast_" + v, Creature.AxieClass.BEAST, "fallback", v);
        }
        if (r < 0.75f) return src.dominant.copy();
        if (r < 0.9375f) return src.recessive1.copy();
        return src.recessive2.copy();
    }

    private static void applyClassBonus(Creature c) {
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
    }

    private static void generateCardsFromGenes(Creature c) {
        c.cards = new java.util.ArrayList<>();
        c.cards.add(new Creature.Card("horn", "Horn", Creature.BodyPart.HORN, c.axieClass, 1, 60, 0, "attack"));
        c.cards.add(new Creature.Card("mouth", "Bite", Creature.BodyPart.MOUTH, c.axieClass, 1, 40, 0, "attack"));
        c.cards.add(new Creature.Card("back", "Shell", Creature.BodyPart.BACK, c.axieClass, 1, 0, 50, "shield"));
        c.cards.add(new Creature.Card("tail", "Sweep", Creature.BodyPart.TAIL, c.axieClass, 2, 80, 0, "attack"));
        c.cards.add(new Creature.Card("eyes", "Focus", Creature.BodyPart.EYES, c.axieClass, 0, 0, 0, "draw"));
        c.cards.add(new Creature.Card("ears", "Listen", Creature.BodyPart.EARS, c.axieClass, 1, 0, 30, "shield"));
        if (c.axieClass == Creature.AxieClass.PLANT) {
            c.cards.add(new Creature.Card("leaf", "Photo", Creature.BodyPart.BACK, c.axieClass, 1, 0, 0, "leaf"));
        } else if (c.axieClass == Creature.AxieClass.BEAST) {
            c.cards.add(new Creature.Card("rage", "Roar", Creature.BodyPart.MOUTH, c.axieClass, 1, 30, 0, "rage"));
        } else if (c.axieClass == Creature.AxieClass.AQUATIC) {
            c.cards.add(new Creature.Card("bubble", "Bubble", Creature.BodyPart.TAIL, c.axieClass, 1, 20, 0, "bubble"));
        }
    }

    public static boolean tryHatch(Creature egg) {
        if (egg == null || !egg.isEgg) return false;
        if (System.currentTimeMillis() < egg.eggMatureTime) return false;
        egg.isEgg = false;
        egg.name = egg.axieClass.name() + " #" + (egg.uid.hashCode() & 0xFFFF);
        return true;
    }
}
