package com.axieoffline.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerData {

    private static final String PREFS = "axie_offline_save";
    private static final String[] SLOT_NAMES = {"eyes", "ears", "horn", "mouth", "back", "tail"};
    private static final Random RNG = new Random();

    public int shards = 2000;
    public List<Creature> creatures = new ArrayList<>();
    public int battlesWon = 0;
    public int battlesLost = 0;

    public void initNewGame() {
        shards = 2000;
        creatures.clear();
        creatures.add(Creature.createStarter(Creature.AxieClass.BEAST, "Bumpy"));
        creatures.add(Creature.createStarter(Creature.AxieClass.PLANT, "Leafy"));
        creatures.add(Creature.createStarter(Creature.AxieClass.AQUATIC, "Splash"));
        battlesWon = 0;
        battlesLost = 0;
    }

    public void save(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject root = new JSONObject();
            root.put("shards", shards);
            root.put("battlesWon", battlesWon);
            root.put("battlesLost", battlesLost);
            JSONArray arr = new JSONArray();
            for (Creature c : creatures) {
                arr.put(creatureToJson(c));
            }
            root.put("creatures", arr);
            sp.edit().putString("data", root.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean load(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = sp.getString("data", null);
            if (raw == null) return false;
            JSONObject root = new JSONObject(raw);
            shards = root.optInt("shards", 2000);
            battlesWon = root.optInt("battlesWon", 0);
            battlesLost = root.optInt("battlesLost", 0);
            creatures.clear();
            JSONArray arr = root.optJSONArray("creatures");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    Creature c = creatureFromJson(arr.getJSONObject(i));
                    if (c != null) creatures.add(c);
                }
            }
            return !creatures.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject creatureToJson(Creature c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("uid", c.uid);
        o.put("name", c.name);
        o.put("class", c.axieClass.name());
        o.put("level", c.level);
        o.put("maxHp", c.maxHp);
        o.put("speed", c.speed);
        o.put("skill", c.skill);
        o.put("morale", c.morale);
        o.put("breedCount", c.breedCount);
        o.put("axp", c.axp);
        o.put("ascension", c.ascension);
        o.put("parent1", c.parent1Uid);
        o.put("parent2", c.parent2Uid);
        o.put("litter", c.litterId);
        o.put("isEgg", c.isEgg);
        o.put("eggMature", c.eggMatureTime);
        if (c.genes != null) {
            JSONArray garr = new JSONArray();
            for (int i = 0; i < c.genes.length; i++) {
                JSONObject g = new JSONObject();
                if (c.genes[i] != null && c.genes[i].dominant != null) {
                    g.put("d", c.genes[i].dominant.partId);
                    g.put("dv", c.genes[i].dominant.variant);
                    g.put("dc", c.genes[i].dominant.partClass.name());
                    if (c.genes[i].recessive1 != null) {
                        g.put("r1", c.genes[i].recessive1.partId);
                        g.put("r1v", c.genes[i].recessive1.variant);
                    }
                    if (c.genes[i].recessive2 != null) {
                        g.put("r2", c.genes[i].recessive2.partId);
                        g.put("r2v", c.genes[i].recessive2.variant);
                    }
                }
                garr.put(g);
            }
            o.put("genes", garr);
        }
        return o;
    }

    private Creature creatureFromJson(JSONObject o) throws Exception {
        Creature c = new Creature();
        c.uid = o.getString("uid");
        c.name = o.getString("name");
        c.axieClass = Creature.AxieClass.valueOf(o.getString("class"));
        c.level = o.optInt("level", 1);
        c.maxHp = o.optInt("maxHp", 30);
        c.hp = c.maxHp;
        c.speed = o.optInt("speed", 30);
        c.skill = o.optInt("skill", 30);
        c.morale = o.optInt("morale", 30);
        c.breedCount = o.optInt("breedCount", 0);
        c.axp = o.optInt("axp", 0);
        c.ascension = o.optInt("ascension", 0);
        c.parent1Uid = o.optString("parent1", null);
        c.parent2Uid = o.optString("parent2", null);
        c.litterId = o.optString("litter", null);
        c.isEgg = o.optBoolean("isEgg", false);
        c.eggMatureTime = o.optLong("eggMature", 0);

        c.genes = new Creature.PartGenes[6];
        JSONArray garr = o.optJSONArray("genes");
        String cls = c.axieClass.name().toLowerCase();
        for (int i = 0; i < 6; i++) {
            int v = Math.abs((c.uid + i).hashCode()) % 12;
            if (garr != null && i < garr.length()) {
                JSONObject g = garr.getJSONObject(i);
                if (g.has("d")) {
                    int dv = g.optInt("dv", v);
                    Creature.AxieClass dc = c.axieClass;
                    try { dc = Creature.AxieClass.valueOf(g.optString("dc", c.axieClass.name())); } catch (Exception ignored) {}
                    Creature.Gene d = new Creature.Gene(g.getString("d"), dc, "D", dv);
                    Creature.Gene r1 = d.copy();
                    Creature.Gene r2 = d.copy();
                    if (g.has("r1")) {
                        r1 = new Creature.Gene(g.getString("r1"), dc, "R1", g.optInt("r1v", dv));
                    }
                    if (g.has("r2")) {
                        r2 = new Creature.Gene(g.getString("r2"), dc, "R2", g.optInt("r2v", dv));
                    }
                    c.genes[i] = new Creature.PartGenes(d, r1, r2);
                    continue;
                }
            }
            String slot = SLOT_NAMES[i];
            Creature.Gene d = new Creature.Gene(slot + "_" + cls + "_" + v, c.axieClass, slot, v);
            c.genes[i] = new Creature.PartGenes(d, d.copy(), d.copy());
        }

        c.cards = new ArrayList<>();
        c.cards.add(new Creature.Card("h", "Horn", Creature.BodyPart.HORN, c.axieClass, 1, 60, 0, "attack"));
        c.cards.add(new Creature.Card("m", "Bite", Creature.BodyPart.MOUTH, c.axieClass, 1, 40, 0, "attack"));
        c.cards.add(new Creature.Card("b", "Shell", Creature.BodyPart.BACK, c.axieClass, 1, 0, 50, "shield"));
        c.cards.add(new Creature.Card("t", "Sweep", Creature.BodyPart.TAIL, c.axieClass, 2, 80, 0, "attack"));
        c.cards.add(new Creature.Card("e", "Focus", Creature.BodyPart.EYES, c.axieClass, 0, 0, 0, "draw"));
        return c;
    }

    public void rewardVictory() {
        shards += 150 + battlesWon * 10;
        battlesWon++;
        for (Creature c : creatures) {
            if (!c.isEgg) {
                c.axp += 50;
                if (c.axp >= c.level * 100) {
                    c.axp -= c.level * 100;
                    c.level++;
                    c.maxHp += 1;
                }
            }
        }
    }

    public void recordDefeat() {
        battlesLost++;
        shards += 30;
    }
}
