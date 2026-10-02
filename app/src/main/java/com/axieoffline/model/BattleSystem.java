package com.axieoffline.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BattleSystem {

    public enum Mode { CLASSIC, ORIGINS }
    public enum Phase {
        CHOOSE_FIRST, RPS, PLAYER_TURN, ENEMY_TURN, ROUND_END, VICTORY, DEFEAT
    }

    public Mode mode = Mode.ORIGINS;
    public Phase phase = Phase.CHOOSE_FIRST;

    public List<Creature> playerTeam = new ArrayList<>();
    public List<Creature> enemyTeam = new ArrayList<>();
    public int playerActiveIdx = 0;
    public int enemyActiveIdx = 0;

    public int playerEnergy = 0;
    public int enemyEnergy = 0;
    public List<Creature.Card> playerHand = new ArrayList<>();
    public List<Creature.Card> enemyHand = new ArrayList<>();
    public List<Creature.Card> playerDeck = new ArrayList<>();
    public List<Creature.Card> enemyDeck = new ArrayList<>();

    public int round = 0;
    public boolean playerGoesFirst = true;
    public boolean playerChoseFirst = false;
    public boolean enemyChoseFirst = false;
    public List<String> log = new ArrayList<>();

    private final Random rng = new Random();

    public void startBattle(List<Creature> player, List<Creature> enemy, Mode m) {
        mode = m;
        playerTeam = new ArrayList<>();
        for (Creature c : player) {
            Creature copy = copyForBattle(c);
            copy.resetBattleState();
            playerTeam.add(copy);
        }
        enemyTeam = new ArrayList<>();
        for (Creature c : enemy) {
            Creature copy = copyForBattle(c);
            copy.resetBattleState();
            enemyTeam.add(copy);
        }
        playerActiveIdx = 0;
        enemyActiveIdx = 0;
        playerHand.clear();
        enemyHand.clear();
        playerDeck.clear();
        enemyDeck.clear();
        for (Creature c : playerTeam) {
            for (Creature.Card card : c.cards) {
                playerDeck.add(card.copy());
                playerDeck.add(card.copy());
            }
        }
        for (Creature c : enemyTeam) {
            for (Creature.Card card : c.cards) {
                enemyDeck.add(card.copy());
                enemyDeck.add(card.copy());
            }
        }
        Collections.shuffle(playerDeck, rng);
        Collections.shuffle(enemyDeck, rng);
        round = 0;
        phase = Phase.CHOOSE_FIRST;
        log.clear();
        log("Battle starts!");
    }

    private Creature copyForBattle(Creature src) {
        Creature c = new Creature();
        c.uid = src.uid;
        c.name = src.name;
        c.axieClass = src.axieClass;
        c.level = src.level;
        c.maxHp = src.maxHp;
        c.speed = src.speed;
        c.skill = src.skill;
        c.morale = src.morale;
        c.ascension = src.ascension;
        c.cards = new ArrayList<>();
        for (Creature.Card card : src.cards) {
            c.cards.add(card.copy());
        }
        return c;
    }

    public void chooseFirst(boolean wantFirst) {
        playerChoseFirst = wantFirst;
        enemyChoseFirst = rng.nextBoolean();
        if (playerChoseFirst != enemyChoseFirst) {
            playerGoesFirst = playerChoseFirst;
            beginRound();
        } else {
            phase = Phase.RPS;
            log("Both chose same. RPS!");
        }
    }

    public void resolveRPS(int playerChoice) {
        // 0 rock, 1 paper, 2 scissors
        int enemyChoice = rng.nextInt(3);
        if (playerChoice == enemyChoice) {
            log("RPS tie. Retry.");
            return;
        }
        boolean playerWins = (playerChoice == 0 && enemyChoice == 2)
                || (playerChoice == 1 && enemyChoice == 0)
                || (playerChoice == 2 && enemyChoice == 1);
        playerGoesFirst = playerWins;
        log(playerWins ? "You win RPS! Go first." : "Enemy wins RPS. You go second.");
        beginRound();
    }

    private void beginRound() {
        round++;
        if (mode == Mode.ORIGINS) {
            // Origins: energy/cards do not carry over
            playerEnergy = 0;
            enemyEnergy = 0;
            playerHand.clear();
            enemyHand.clear();
        }
        // Energy & draw by round (Origins style)
        if (round == 1) {
            if (playerGoesFirst) {
                playerEnergy += 1;
                drawCards(true, 3);
                enemyEnergy += 2;
                drawCards(false, 4);
            } else {
                playerEnergy += 2;
                drawCards(true, 4);
                enemyEnergy += 1;
                drawCards(false, 3);
            }
        } else if (round == 2) {
            if (playerGoesFirst) {
                playerEnergy += 2;
                drawCards(true, 4);
                enemyEnergy += 3;
                drawCards(false, 4);
            } else {
                playerEnergy += 3;
                drawCards(true, 4);
                enemyEnergy += 2;
                drawCards(false, 4);
            }
        } else {
            playerEnergy += 3;
            drawCards(true, 5);
            enemyEnergy += 3;
            drawCards(false, 5);
        }
        if (mode == Mode.CLASSIC) {
            // Classic baseline
            if (round == 1) {
                playerEnergy = 3;
                enemyEnergy = 3;
                drawCards(true, 6);
                drawCards(false, 6);
            } else {
                playerEnergy += 2;
                enemyEnergy += 2;
                drawCards(true, 3);
                drawCards(false, 3);
            }
        }
        phase = playerGoesFirst ? Phase.PLAYER_TURN : Phase.ENEMY_TURN;
        if (phase == Phase.ENEMY_TURN) {
            enemyAITurn();
        }
        log("Round " + round);
    }

    private void drawCards(boolean player, int n) {
        List<Creature.Card> deck = player ? playerDeck : enemyDeck;
        List<Creature.Card> hand = player ? playerHand : enemyHand;
        for (int i = 0; i < n && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
    }

    public boolean playCard(int handIndex, int targetEnemyIdx) {
        if (phase != Phase.PLAYER_TURN) return false;
        if (handIndex < 0 || handIndex >= playerHand.size()) return false;
        Creature.Card card = playerHand.get(handIndex);
        if (playerEnergy < card.energyCost) return false;

        Creature user = getPlayerActive();
        if (user == null || !user.isAlive()) return false;

        playerEnergy -= card.energyCost;
        applyCard(user, card, true, targetEnemyIdx);
        if (!card.retain) {
            playerHand.remove(handIndex);
        }
        checkDeaths();
        if (phase == Phase.VICTORY || phase == Phase.DEFEAT) return true;
        return true;
    }

    public void endPlayerTurn() {
        if (phase != Phase.PLAYER_TURN) return;
        applyEndOfTurnEffects(true);
        checkDeaths();
        if (phase == Phase.VICTORY || phase == Phase.DEFEAT) return;
        phase = Phase.ENEMY_TURN;
        enemyAITurn();
    }

    private void enemyAITurn() {
        Creature enemy = getEnemyActive();
        if (enemy == null || !enemy.isAlive()) {
            endEnemyTurn();
            return;
        }
        // Simple AI: play affordable attack cards targeting lowest HP or class weakness
        List<Integer> toPlay = new ArrayList<>();
        int energyLeft = enemyEnergy;
        for (int i = 0; i < enemyHand.size(); i++) {
            Creature.Card c = enemyHand.get(i);
            if (c.energyCost <= energyLeft && (c.attack > 0 || "rage".equals(c.effect) || "leaf".equals(c.effect))) {
                toPlay.add(i);
                energyLeft -= c.energyCost;
                if (toPlay.size() >= 3) break;
            }
        }
        // Play from high index to low to keep indices valid
        Collections.sort(toPlay, Collections.reverseOrder());
        for (int idx : toPlay) {
            if (idx >= enemyHand.size()) continue;
            Creature.Card card = enemyHand.get(idx);
            if (enemyEnergy < card.energyCost) continue;
            enemyEnergy -= card.energyCost;
            int target = findBestPlayerTarget(card);
            applyCard(enemy, card, false, target);
            if (!card.retain) {
                enemyHand.remove(idx);
            }
            checkDeaths();
            if (phase == Phase.VICTORY || phase == Phase.DEFEAT) return;
        }
        endEnemyTurn();
    }

    private int findBestPlayerTarget(Creature.Card card) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < playerTeam.size(); i++) {
            Creature p = playerTeam.get(i);
            if (!p.isAlive()) continue;
            int score = 1000 - p.currentHp;
            if (p.taunt) score += 500;
            if (Creature.isStrongAgainst(getEnemyActive().axieClass, p.axieClass)) score += 200;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best >= 0 ? best : 0;
    }

    private void endEnemyTurn() {
        applyEndOfTurnEffects(false);
        checkDeaths();
        if (phase == Phase.VICTORY || phase == Phase.DEFEAT) return;
        // Next round
        if (mode == Mode.ORIGINS) {
            // discard non-retain
            playerHand.removeIf(c -> !c.retain);
            enemyHand.removeIf(c -> !c.retain);
        }
        beginRound();
    }

    private void applyCard(Creature user, Creature.Card card, boolean isPlayer, int targetIdx) {
        List<Creature> targets = isPlayer ? enemyTeam : playerTeam;
        Creature target = null;
        if (targetIdx >= 0 && targetIdx < targets.size() && targets.get(targetIdx).isAlive()) {
            target = targets.get(targetIdx);
        } else {
            for (Creature t : targets) {
                if (t.isAlive()) {
                    target = t;
                    break;
                }
            }
        }

        float mult = 1.0f;
        // Same class card bonus +15%
        if (card.cardClass == user.axieClass) {
            mult *= 1.15f;
        }
        if (target != null) {
            if (Creature.isStrongAgainst(user.axieClass, target.axieClass)) {
                mult *= 1.15f;
            } else if (Creature.isWeakAgainst(user.axieClass, target.axieClass)) {
                mult *= 0.85f;
            }
        }

        // Skill combo bonus (simplified: if multiple cards already played this turn, approximate)
        float skillBonus = (1f + (user.skill * 0.55f - 12.25f) / 100f * 0.985f);
        if (skillBonus < 1f) skillBonus = 1f;

        if (user.furyForm) {
            mult *= 1.5f;
        }

        int dmg = Math.round(card.attack * mult * skillBonus);

        // Classic crit
        if (mode == Mode.CLASSIC && target != null && card.attack > 0) {
            float critChance = user.morale / 100f;
            critChance -= target.speed / 200f;
            if (critChance > 0 && rng.nextFloat() < critChance) {
                dmg *= 2;
                log(user.name + " CRIT!");
            }
        }

        // Rage stacks add damage
        if (user.rage > 0 && card.attack > 0) {
            dmg += user.rage;
        }

        if (target != null && target.fragile) {
            dmg = Math.round(dmg * 1.1f);
            if (target.currentShield > 0) dmg = Math.round(dmg * 1.1f);
        }

        if (card.shield > 0) {
            user.currentShield += Math.round(card.shield * (card.cardClass == user.axieClass ? 1.15f : 1f));
            log(user.name + " gains " + card.shield + " shield");
        }

        if (dmg > 0 && target != null) {
            if (target.fearTurns > 0) {
                log(user.name + " is feared, cannot attack");
            } else {
                int remaining = dmg;
                if (target.currentShield > 0) {
                    int absorbed = Math.min(target.currentShield, remaining);
                    target.currentShield -= absorbed;
                    remaining -= absorbed;
                }
                target.currentHp -= remaining;
                // Rage on taking damage
                target.rage = Math.min(10, target.rage + 1);
                log(user.name + " uses " + card.name + " -> " + target.name + " (-" + dmg + ")");
                if (target.rage >= 10) {
                    target.rage = 0;
                    target.furyForm = true;
                    log(target.name + " enters FURY!");
                }
            }
        }

        // Effects
        if ("leaf".equals(card.effect)) {
            user.leaf = Math.min(10, user.leaf + 1);
            log(user.name + " gains Leaf (" + user.leaf + ")");
        } else if ("rage".equals(card.effect)) {
            user.rage = Math.min(10, user.rage + 2);
            log(user.name + " gains Rage (" + user.rage + ")");
            if (user.rage >= 10) {
                user.rage = 0;
                user.furyForm = true;
                log(user.name + " enters FURY!");
            }
        } else if ("bubble".equals(card.effect) && target != null) {
            target.bubble = Math.min(4, target.bubble + 1);
            log(target.name + " Bubble " + target.bubble);
            if (target.bubble >= 4) {
                // Bubble bomb AoE
                for (Creature t : targets) {
                    if (t.isAlive()) {
                        t.currentHp -= 40;
                    }
                }
                target.bubble = 0;
                log("Bubble Bomb!");
            }
        } else if ("draw".equals(card.effect)) {
            drawCards(isPlayer, 1);
        } else if ("taunt".equals(card.effect)) {
            user.taunt = true;
        } else if ("fear".equals(card.effect) && target != null) {
            target.fearTurns = 2;
        } else if ("dispel".equals(card.effect) && target != null) {
            target.leaf = 0;
            target.rage = 0;
            target.bubble = 0;
            target.taunt = false;
            target.fragile = false;
            target.furyForm = false;
        } else if ("fragile".equals(card.effect) && target != null) {
            target.fragile = true;
        }
    }

    private void applyEndOfTurnEffects(boolean playerSide) {
        List<Creature> team = playerSide ? playerTeam : enemyTeam;
        for (Creature c : team) {
            if (!c.isAlive()) continue;
            if (c.leaf > 0) {
                int heal = c.leaf * 4;
                int room = c.getBattleMaxHp() - c.currentHp;
                int actualHeal = Math.min(heal, room);
                c.currentHp += actualHeal;
                int excess = heal - actualHeal;
                if (excess > 0) c.currentShield += excess;
            }
            if (c.fearTurns > 0) c.fearTurns--;
            c.furyForm = false; // Fury lasts one turn
            c.taunt = false;
        }
    }

    private void checkDeaths() {
        // Advance active index if dead
        while (playerActiveIdx < playerTeam.size() && !playerTeam.get(playerActiveIdx).isAlive()) {
            playerActiveIdx++;
        }
        while (enemyActiveIdx < enemyTeam.size() && !enemyTeam.get(enemyActiveIdx).isAlive()) {
            enemyActiveIdx++;
        }
        boolean playerAlive = false;
        for (Creature c : playerTeam) if (c.isAlive()) playerAlive = true;
        boolean enemyAlive = false;
        for (Creature c : enemyTeam) if (c.isAlive()) enemyAlive = true;

        if (!enemyAlive) {
            phase = Phase.VICTORY;
            log("VICTORY!");
        } else if (!playerAlive) {
            phase = Phase.DEFEAT;
            log("DEFEAT");
        }
    }

    public Creature getPlayerActive() {
        if (playerActiveIdx < playerTeam.size()) return playerTeam.get(playerActiveIdx);
        for (Creature c : playerTeam) if (c.isAlive()) return c;
        return null;
    }

    public Creature getEnemyActive() {
        if (enemyActiveIdx < enemyTeam.size()) return enemyTeam.get(enemyActiveIdx);
        for (Creature c : enemyTeam) if (c.isAlive()) return c;
        return null;
    }

    private void log(String msg) {
        log.add(msg);
        if (log.size() > 30) log.remove(0);
    }

    /** Turn order comparator for sequential (Origins) */
    public static Comparator<Creature> turnOrderComparator() {
        return (a, b) -> {
            if (b.speed != a.speed) return Integer.compare(b.speed, a.speed);
            if (a.currentHp != b.currentHp) return Integer.compare(a.currentHp, b.currentHp);
            if (b.skill != a.skill) return Integer.compare(b.skill, a.skill);
            if (b.morale != a.morale) return Integer.compare(b.morale, a.morale);
            return a.uid.compareTo(b.uid);
        };
    }
}
