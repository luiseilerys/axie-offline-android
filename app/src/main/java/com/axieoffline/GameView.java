package com.axieoffline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.axieoffline.model.BattleSystem;
import com.axieoffline.model.BreedingSystem;
import com.axieoffline.model.Creature;
import com.axieoffline.model.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private Thread thread;
    private volatile boolean running;
    private final SurfaceHolder holder;
    private final GameState state;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();

    private int W, H;
    private float touchX, touchY;
    private boolean touched;

    // UI hit areas
    private final List<RectF> buttons = new ArrayList<>();
    private final List<String> buttonActions = new ArrayList<>();
    private final List<RectF> cardRects = new ArrayList<>();
    private final List<RectF> creatureRects = new ArrayList<>();

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        state = new GameState();
        if (!state.player.load(context)) {
            state.player.initNewGame();
            state.player.save(context);
        }
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        try {
            if (thread != null) thread.join(500);
        } catch (InterruptedException ignored) {}
        state.player.save(getContext());
    }

    public void saveAndStop() {
        pause();
        state.player.save(getContext());
    }

    @Override
    public void run() {
        long target = 16; // ~60fps
        while (running) {
            long start = System.currentTimeMillis();
            if (holder.getSurface().isValid()) {
                Canvas c = holder.lockCanvas();
                if (c != null) {
                    try {
                        update();
                        render(c);
                    } finally {
                        holder.unlockCanvasAndPost(c);
                    }
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < target) {
                try { Thread.sleep(target - elapsed); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update() {
        // Hatch eggs
        for (Creature c : state.player.creatures) {
            if (c.isEgg) BreedingSystem.tryHatch(c);
        }
    }

    private void render(Canvas canvas) {
        W = canvas.getWidth();
        H = canvas.getHeight();
        canvas.drawColor(0xFF1A1A2E);

        buttons.clear();
        buttonActions.clear();
        cardRects.clear();
        creatureRects.clear();

        switch (state.screen) {
            case HOME: drawHome(canvas); break;
            case COLLECTION: drawCollection(canvas); break;
            case BATTLE_SETUP: drawBattleSetup(canvas); break;
            case BATTLE: drawBattle(canvas); break;
            case BREED: drawBreed(canvas); break;
            case DETAIL: drawDetail(canvas); break;
        }

        if (state.hasMessage()) {
            paint.setColor(0xCC000000);
            canvas.drawRect(0, H * 0.4f, W, H * 0.55f, paint);
            textPaint.setTextSize(dp(18));
            textPaint.setColor(Color.WHITE);
            canvas.drawText(state.message, W / 2f, H * 0.48f, textPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private void drawHome(Canvas canvas) {
        textPaint.setTextSize(dp(28));
        textPaint.setColor(0xFFE94560);
        canvas.drawText("Axie Offline", W / 2f, H * 0.15f, textPaint);
        textPaint.setTextSize(dp(14));
        textPaint.setColor(0xFFAAAAAA);
        canvas.drawText("Classic + Origins mechanics", W / 2f, H * 0.2f, textPaint);

        textPaint.setTextSize(dp(16));
        textPaint.setColor(0xFFFFD700);
        canvas.drawText("Shards: " + state.player.shards, W / 2f, H * 0.28f, textPaint);

        float y = H * 0.38f;
        addButton(canvas, "Battle", "battle", y); y += dp(56);
        addButton(canvas, "Collection", "collection", y); y += dp(56);
        addButton(canvas, "Breeding", "breed", y); y += dp(56);
        addButton(canvas, "New Game", "newgame", y);
    }

    private void addButton(Canvas canvas, String label, String action, float y) {
        float left = W * 0.15f;
        float right = W * 0.85f;
        float h = dp(44);
        RectF r = new RectF(left, y, right, y + h);
        paint.setColor(0xFF0F3460);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(r, dp(12), dp(12), paint);
        paint.setColor(0xFFE94560);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        canvas.drawRoundRect(r, dp(12), dp(12), paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(dp(16));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, W / 2f, y + h * 0.65f, textPaint);
        buttons.add(r);
        buttonActions.add(action);
    }

    private void drawCollection(Canvas canvas) {
        textPaint.setTextSize(dp(22));
        textPaint.setColor(0xFFE94560);
        canvas.drawText("Collection", W / 2f, dp(40), textPaint);
        addBackButton(canvas);

        float cardW = (W - dp(48)) / 2f;
        float cardH = dp(100);
        float x0 = dp(16);
        float y0 = dp(70);
        int i = 0;
        for (Creature c : state.player.creatures) {
            float x = x0 + (i % 2) * (cardW + dp(16));
            float y = y0 + (i / 2) * (cardH + dp(12));
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            paint.setColor(0xFF16213E);
            canvas.drawRoundRect(r, dp(10), dp(10), paint);
            paint.setColor(c.getClassColor());
            canvas.drawCircle(x + dp(28), y + dp(36), dp(18), paint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(dp(13));
            textPaint.setColor(Color.WHITE);
            canvas.drawText(c.isEgg ? "EGG" : c.name, x + dp(54), y + dp(28), textPaint);
            textPaint.setTextSize(dp(11));
            textPaint.setColor(0xFFAAAAAA);
            canvas.drawText(c.axieClass.name() + " Lv" + c.level, x + dp(54), y + dp(46), textPaint);
            canvas.drawText("HP " + c.maxHp + " SPD " + c.speed, x + dp(54), y + dp(62), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            creatureRects.add(r);
            i++;
        }
    }

    private void addBackButton(Canvas canvas) {
        RectF r = new RectF(dp(8), dp(8), dp(80), dp(40));
        paint.setColor(0xFF0F3460);
        canvas.drawRoundRect(r, dp(8), dp(8), paint);
        textPaint.setTextSize(dp(14));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Back", r.centerX(), r.centerY() + dp(5), textPaint);
        buttons.add(r);
        buttonActions.add("back");
    }

    private void drawBattleSetup(Canvas canvas) {
        textPaint.setTextSize(dp(20));
        textPaint.setColor(0xFFE94560);
        canvas.drawText("Select Team (3)", W / 2f, dp(40), textPaint);
        addBackButton(canvas);

        float cardW = (W - dp(48)) / 2f;
        float cardH = dp(80);
        float y0 = dp(60);
        int i = 0;
        for (Creature c : state.player.creatures) {
            if (c.isEgg) { i++; continue; }
            float x = dp(16) + (i % 2) * (cardW + dp(16));
            float y = y0 + (i / 2) * (cardH + dp(10));
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            boolean sel = state.selectedTeam.contains(c);
            paint.setColor(sel ? 0xFFE94560 : 0xFF16213E);
            canvas.drawRoundRect(r, dp(8), dp(8), paint);
            paint.setColor(c.getClassColor());
            canvas.drawCircle(x + dp(24), y + cardH / 2, dp(16), paint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(dp(12));
            textPaint.setColor(Color.WHITE);
            canvas.drawText(c.name, x + dp(48), y + dp(30), textPaint);
            canvas.drawText(c.axieClass.name(), x + dp(48), y + dp(48), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            creatureRects.add(r);
            i++;
        }
        float by = H - dp(70);
        addButton(canvas, "Start Origins", "start_origins", by - dp(56));
        addButton(canvas, "Start Classic", "start_classic", by);
    }

    private void drawBattle(Canvas canvas) {
        BattleSystem b = state.battle;

        // Enemy side
        float ey = dp(50);
        for (int i = 0; i < b.enemyTeam.size(); i++) {
            Creature c = b.enemyTeam.get(i);
            float x = W * (0.2f + i * 0.3f);
            drawUnit(canvas, c, x, ey, i == b.enemyActiveIdx);
        }

        // Player side
        float py = H * 0.38f;
        for (int i = 0; i < b.playerTeam.size(); i++) {
            Creature c = b.playerTeam.get(i);
            float x = W * (0.2f + i * 0.3f);
            drawUnit(canvas, c, x, py, i == b.playerActiveIdx);
        }

        // Energy
        textPaint.setTextSize(dp(14));
        textPaint.setColor(0xFFFFD700);
        canvas.drawText("Energy: " + b.playerEnergy + "  Round: " + b.round, W / 2f, H * 0.52f, textPaint);

        // Phase UI
        if (b.phase == BattleSystem.Phase.CHOOSE_FIRST) {
            addButton(canvas, "Go First", "first", H * 0.6f);
            addButton(canvas, "Go Second", "second", H * 0.6f + dp(56));
        } else if (b.phase == BattleSystem.Phase.RPS) {
            addButton(canvas, "Rock", "rps0", H * 0.58f);
            addButton(canvas, "Paper", "rps1", H * 0.58f + dp(50));
            addButton(canvas, "Scissors", "rps2", H * 0.58f + dp(100));
        } else if (b.phase == BattleSystem.Phase.PLAYER_TURN) {
            // Cards
            float cardW = Math.min(dp(72), (W - dp(20)) / Math.max(1, b.playerHand.size()));
            float cardH = dp(90);
            float startX = (W - cardW * b.playerHand.size()) / 2f;
            float cy = H - cardH - dp(60);
            for (int i = 0; i < b.playerHand.size(); i++) {
                Creature.Card card = b.playerHand.get(i);
                float x = startX + i * cardW;
                RectF r = new RectF(x + dp(2), cy, x + cardW - dp(2), cy + cardH);
                boolean canPlay = b.playerEnergy >= card.energyCost;
                paint.setColor(canPlay ? 0xFF0F3460 : 0xFF333333);
                canvas.drawRoundRect(r, dp(6), dp(6), paint);
                paint.setColor(0xFFE94560);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRoundRect(r, dp(6), dp(6), paint);
                paint.setStyle(Paint.Style.FILL);
                textPaint.setTextSize(dp(10));
                textPaint.setColor(Color.WHITE);
                canvas.drawText(card.name, r.centerX(), cy + dp(18), textPaint);
                canvas.drawText(card.energyCost + "E", r.centerX(), cy + dp(36), textPaint);
                if (card.attack > 0) canvas.drawText("ATK " + card.attack, r.centerX(), cy + dp(52), textPaint);
                if (card.shield > 0) canvas.drawText("SH " + card.shield, r.centerX(), cy + dp(66), textPaint);
                cardRects.add(r);
            }
            addButton(canvas, "End Turn", "endturn", H - dp(48));
        } else if (b.phase == BattleSystem.Phase.VICTORY || b.phase == BattleSystem.Phase.DEFEAT) {
            textPaint.setTextSize(dp(24));
            textPaint.setColor(b.phase == BattleSystem.Phase.VICTORY ? 0xFF4ADE80 : 0xFFE94560);
            canvas.drawText(b.phase == BattleSystem.Phase.VICTORY ? "VICTORY!" : "DEFEAT", W / 2f, H * 0.6f, textPaint);
            addButton(canvas, "Continue", "battle_done", H * 0.7f);
        }

        // Log
        textPaint.setTextSize(dp(10));
        textPaint.setColor(0xFFCCCCCC);
        textPaint.setTextAlign(Paint.Align.LEFT);
        float ly = H * 0.55f;
        int start = Math.max(0, b.log.size() - 4);
        for (int i = start; i < b.log.size(); i++) {
            canvas.drawText(b.log.get(i), dp(8), ly, textPaint);
            ly += dp(14);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawUnit(Canvas canvas, Creature c, float x, float y, boolean active) {
        float r = dp(28);
        paint.setColor(c.isAlive() ? c.getClassColor() : 0xFF444444);
        canvas.drawCircle(x, y, r, paint);
        if (active && c.isAlive()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(0xFFE94560);
            canvas.drawCircle(x, y, r + dp(4), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        // HP bar
        float barW = dp(60);
        float barH = dp(6);
        float bx = x - barW / 2;
        float by = y + r + dp(6);
        paint.setColor(0xFF333333);
        canvas.drawRect(bx, by, bx + barW, by + barH, paint);
        float pct = c.getBattleMaxHp() > 0 ? Math.max(0, (float) c.currentHp / c.getBattleMaxHp()) : 0;
        paint.setColor(0xFF4ADE80);
        canvas.drawRect(bx, by, bx + barW * pct, by + barH, paint);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(c.name, x, y - r - dp(8), textPaint);
        if (c.currentShield > 0) {
            textPaint.setColor(0xFF60A5FA);
            canvas.drawText("SH " + c.currentShield, x, by + dp(18), textPaint);
        }
        if (c.rage > 0) {
            textPaint.setColor(0xFFEF4444);
            canvas.drawText("R" + c.rage, x + r + dp(8), y, textPaint);
        }
        if (c.furyForm) {
            textPaint.setColor(0xFFFF6B6B);
            canvas.drawText("FURY", x, y + dp(4), textPaint);
        }
    }

    private void drawBreed(Canvas canvas) {
        textPaint.setTextSize(dp(20));
        textPaint.setColor(0xFFE94560);
        canvas.drawText("Breeding", W / 2f, dp(40), textPaint);
        addBackButton(canvas);

        textPaint.setTextSize(dp(12));
        textPaint.setColor(0xFFAAAAAA);
        canvas.drawText("Select 2 parents. Cost depends on breed count.", W / 2f, dp(60), textPaint);

        float cardW = (W - dp(48)) / 2f;
        float cardH = dp(70);
        float y0 = dp(80);
        int i = 0;
        for (Creature c : state.player.creatures) {
            if (c.isEgg) continue;
            float x = dp(16) + (i % 2) * (cardW + dp(16));
            float y = y0 + (i / 2) * (cardH + dp(8));
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            boolean sel = c == state.breedA || c == state.breedB;
            paint.setColor(sel ? 0xFFE94560 : 0xFF16213E);
            canvas.drawRoundRect(r, dp(8), dp(8), paint);
            paint.setColor(c.getClassColor());
            canvas.drawCircle(x + dp(20), y + cardH / 2, dp(14), paint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(dp(11));
            textPaint.setColor(Color.WHITE);
            canvas.drawText(c.name + " (B" + c.breedCount + ")", x + dp(42), y + dp(28), textPaint);
            canvas.drawText(c.axieClass.name(), x + dp(42), y + dp(46), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            creatureRects.add(r);
            i++;
        }

        if (state.breedA != null && state.breedB != null) {
            int cost = BreedingSystem.getBreedCost(state.breedA, state.breedB);
            boolean ok = BreedingSystem.canBreed(state.breedA, state.breedB) && state.player.shards >= cost;
            textPaint.setTextSize(dp(14));
            textPaint.setColor(ok ? 0xFFFFD700 : 0xFFE94560);
            canvas.drawText("Cost: " + cost + " shards", W / 2f, H - dp(90), textPaint);
            if (ok) addButton(canvas, "Breed", "do_breed", H - dp(70));
        }
    }

    private void drawDetail(Canvas canvas) {
        Creature c = state.detailCreature;
        if (c == null) return;
        addBackButton(canvas);
        paint.setColor(c.getClassColor());
        canvas.drawCircle(W / 2f, H * 0.22f, dp(40), paint);
        textPaint.setTextSize(dp(22));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(c.name, W / 2f, H * 0.35f, textPaint);
        textPaint.setTextSize(dp(14));
        textPaint.setColor(0xFFAAAAAA);
        canvas.drawText(c.axieClass.name() + "  Level " + c.level, W / 2f, H * 0.4f, textPaint);

        String[] lines = {
                "HP: " + c.maxHp,
                "Speed: " + c.speed,
                "Skill: " + c.skill,
                "Moral: " + c.morale,
                "Breeds: " + c.breedCount + "/7",
                "Purity: " + String.format("%.0f%%", c.getPurity() * 100),
                "AXP: " + c.axp
        };
        float y = H * 0.48f;
        textPaint.setTextSize(dp(14));
        textPaint.setColor(Color.WHITE);
        for (String line : lines) {
            canvas.drawText(line, W / 2f, y, textPaint);
            y += dp(22);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchX = event.getX();
            touchY = event.getY();
            handleTouch(touchX, touchY);
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTouch(float x, float y) {
        // Buttons
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains(x, y)) {
                onAction(buttonActions.get(i));
                return;
            }
        }

        if (state.screen == GameState.Screen.COLLECTION) {
            int idx = 0;
            for (RectF r : creatureRects) {
                if (r.contains(x, y) && idx < state.player.creatures.size()) {
                    state.detailCreature = state.player.creatures.get(idx);
                    state.screen = GameState.Screen.DETAIL;
                    return;
                }
                idx++;
            }
        }

        if (state.screen == GameState.Screen.BATTLE_SETUP) {
            List<Creature> selectable = new ArrayList<>();
            for (Creature c : state.player.creatures) if (!c.isEgg) selectable.add(c);
            int idx = 0;
            for (RectF r : creatureRects) {
                if (r.contains(x, y) && idx < selectable.size()) {
                    Creature c = selectable.get(idx);
                    if (state.selectedTeam.contains(c)) {
                        state.selectedTeam.remove(c);
                    } else if (state.selectedTeam.size() < 3) {
                        state.selectedTeam.add(c);
                    }
                    return;
                }
                idx++;
            }
        }

        if (state.screen == GameState.Screen.BREED) {
            List<Creature> selectable = new ArrayList<>();
            for (Creature c : state.player.creatures) if (!c.isEgg) selectable.add(c);
            int idx = 0;
            for (RectF r : creatureRects) {
                if (r.contains(x, y) && idx < selectable.size()) {
                    Creature c = selectable.get(idx);
                    if (state.breedA == c) state.breedA = null;
                    else if (state.breedB == c) state.breedB = null;
                    else if (state.breedA == null) state.breedA = c;
                    else if (state.breedB == null) state.breedB = c;
                    return;
                }
                idx++;
            }
        }

        if (state.screen == GameState.Screen.BATTLE && state.battle.phase == BattleSystem.Phase.PLAYER_TURN) {
            for (int i = 0; i < cardRects.size(); i++) {
                if (cardRects.get(i).contains(x, y)) {
                    state.battle.playCard(i, state.battle.enemyActiveIdx);
                    return;
                }
            }
        }
    }

    private void onAction(String action) {
        switch (action) {
            case "back":
                state.screen = GameState.Screen.HOME;
                break;
            case "battle":
                state.selectedTeam.clear();
                state.screen = GameState.Screen.BATTLE_SETUP;
                break;
            case "collection":
                state.screen = GameState.Screen.COLLECTION;
                break;
            case "breed":
                state.breedA = null;
                state.breedB = null;
                state.screen = GameState.Screen.BREED;
                break;
            case "newgame":
                state.player.initNewGame();
                state.player.save(getContext());
                state.showMessage("New game started", 1500);
                break;
            case "start_origins":
            case "start_classic":
                if (state.selectedTeam.size() != 3) {
                    state.showMessage("Select 3 creatures", 1500);
                    return;
                }
                List<Creature> enemies = new ArrayList<>();
                Creature.AxieClass[] classes = Creature.AxieClass.values();
                for (int i = 0; i < 3; i++) {
                    enemies.add(Creature.createStarter(classes[rng.nextInt(classes.length)], "Enemy" + (i + 1)));
                }
                BattleSystem.Mode mode = action.equals("start_classic")
                        ? BattleSystem.Mode.CLASSIC : BattleSystem.Mode.ORIGINS;
                state.battle.startBattle(state.selectedTeam, enemies, mode);
                state.screen = GameState.Screen.BATTLE;
                break;
            case "first":
                state.battle.chooseFirst(true);
                break;
            case "second":
                state.battle.chooseFirst(false);
                break;
            case "rps0":
                state.battle.resolveRPS(0);
                break;
            case "rps1":
                state.battle.resolveRPS(1);
                break;
            case "rps2":
                state.battle.resolveRPS(2);
                break;
            case "endturn":
                state.battle.endPlayerTurn();
                break;
            case "battle_done":
                if (state.battle.phase == BattleSystem.Phase.VICTORY) {
                    state.player.rewardVictory();
                } else {
                    state.player.recordDefeat();
                }
                state.player.save(getContext());
                state.screen = GameState.Screen.HOME;
                break;
            case "do_breed":
                if (state.breedA != null && state.breedB != null) {
                    int cost = BreedingSystem.getBreedCost(state.breedA, state.breedB);
                    if (BreedingSystem.canBreed(state.breedA, state.breedB) && state.player.shards >= cost) {
                        state.player.shards -= cost;
                        Creature child = BreedingSystem.breed(state.breedA, state.breedB);
                        if (child != null) {
                            state.player.creatures.add(child);
                            state.player.save(getContext());
                            state.showMessage("Egg created! Hatches in 30s", 2000);
                            state.breedA = null;
                            state.breedB = null;
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        pause();
    }
}
