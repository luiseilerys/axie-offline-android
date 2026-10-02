package com.axieoffline;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.axieoffline.model.Creature;
import com.axieoffline.model.GameState;
import com.axieoffline.model.PartComposer;

import java.util.ArrayList;
import java.util.List;

/** Axie-style home screen renderer matching the classic outdoor lobby look. */
public class HomeScreen {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long animStart = System.currentTimeMillis();
    private final float density;

    public HomeScreen(float density) {
        this.density = density;
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private float dp(float v) { return v * density; }

    public void draw(Canvas canvas, int W, int H, GameState state, PartComposer composer,
                     List<RectF> buttons, List<String> buttonActions) {
        float t = (System.currentTimeMillis() - animStart) / 1000f;
        drawForestBackground(canvas, W, H);
        drawTopBar(canvas, W, H, state);
        drawShowcaseCreatures(canvas, W, H, t, state, composer);
        drawSideMenu(canvas, W, H, buttons, buttonActions);
        drawPlayButton(canvas, W, H, buttons, buttonActions);
    }

    private void drawForestBackground(Canvas canvas, int W, int H) {
        paint.setShader(new LinearGradient(0, 0, 0, H * 0.55f,
                0xFFB8E8A0, 0xFF6BCB77, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H * 0.55f, paint);
        paint.setShader(null);

        paint.setShader(new LinearGradient(0, H * 0.45f, 0, H,
                0xFF5BB86A, 0xFF2D8A3E, Shader.TileMode.CLAMP));
        canvas.drawRect(0, H * 0.45f, W, H, paint);
        paint.setShader(null);

        paint.setColor(0x22FFFFFF);
        for (int i = 0; i < 5; i++) {
            float cx = W * (0.15f + i * 0.18f);
            Path ray = new Path();
            ray.moveTo(cx - dp(30), 0);
            ray.lineTo(cx + dp(30), 0);
            ray.lineTo(cx + dp(80), H * 0.5f);
            ray.lineTo(cx - dp(80), H * 0.5f);
            ray.close();
            canvas.drawPath(ray, paint);
        }

        paint.setColor(0xFF3D9B4F);
        drawTree(canvas, W * 0.08f, H * 0.42f, dp(70));
        drawTree(canvas, W * 0.92f, H * 0.40f, dp(85));
        paint.setColor(0xFF2E7D3E);
        drawTree(canvas, W * 0.0f, H * 0.48f, dp(55));
        drawTree(canvas, W * 1.0f, H * 0.50f, dp(60));
        drawTree(canvas, W * 0.22f, H * 0.38f, dp(45));
        drawTree(canvas, W * 0.78f, H * 0.36f, dp(50));

        paint.setColor(0xFF4CAF50);
        for (int i = 0; i < 18; i++) {
            float gx = W * (0.05f + (i * 0.053f) % 0.9f);
            float gy = H * (0.72f + ((i * 17) % 20) / 100f);
            canvas.drawOval(gx - dp(8), gy - dp(4), gx + dp(8), gy + dp(6), paint);
            paint.setColor(0xFF66BB6A);
            canvas.drawOval(gx - dp(4), gy - dp(10), gx + dp(4), gy, paint);
            paint.setColor(0xFF4CAF50);
        }

        paint.setColor(0xFF8D6E63);
        canvas.drawOval(W * 0.12f, H * 0.78f, W * 0.22f, H * 0.86f, paint);
        paint.setColor(0xFFA1887F);
        canvas.drawOval(W * 0.14f, H * 0.79f, W * 0.20f, H * 0.84f, paint);
        paint.setColor(0xFF8D6E63);
        canvas.drawOval(W * 0.75f, H * 0.82f, W * 0.88f, H * 0.90f, paint);

        drawMushroom(canvas, W * 0.30f, H * 0.80f, dp(14), 0xFFE53935);
        drawMushroom(canvas, W * 0.68f, H * 0.77f, dp(11), 0xFFEF5350);
        drawMushroom(canvas, W * 0.08f, H * 0.70f, dp(10), 0xFFFF7043);

        paint.setShader(new RadialGradient(W * 0.55f, H * 0.58f, W * 0.35f,
                0x33000000, 0x00000000, Shader.TileMode.CLAMP));
        canvas.drawCircle(W * 0.55f, H * 0.58f, W * 0.35f, paint);
        paint.setShader(null);
    }

    private void drawTree(Canvas canvas, float cx, float baseY, float size) {
        paint.setColor(0xFF5D4037);
        canvas.drawRect(cx - size * 0.08f, baseY - size * 0.3f, cx + size * 0.08f, baseY + size * 0.05f, paint);
        paint.setColor(0xFF2E7D32);
        canvas.drawCircle(cx, baseY - size * 0.45f, size * 0.35f, paint);
        paint.setColor(0xFF388E3C);
        canvas.drawCircle(cx - size * 0.2f, baseY - size * 0.35f, size * 0.28f, paint);
        canvas.drawCircle(cx + size * 0.2f, baseY - size * 0.38f, size * 0.30f, paint);
        paint.setColor(0xFF43A047);
        canvas.drawCircle(cx, baseY - size * 0.55f, size * 0.25f, paint);
    }

    private void drawMushroom(Canvas canvas, float cx, float cy, float size, int capColor) {
        paint.setColor(0xFFFFF8E1);
        canvas.drawRect(cx - size * 0.15f, cy - size * 0.3f, cx + size * 0.15f, cy + size * 0.4f, paint);
        paint.setColor(capColor);
        canvas.drawOval(cx - size * 0.55f, cy - size * 0.7f, cx + size * 0.55f, cy - size * 0.1f, paint);
        paint.setColor(0xFFFFEBEE);
        canvas.drawCircle(cx - size * 0.2f, cy - size * 0.45f, size * 0.12f, paint);
        canvas.drawCircle(cx + size * 0.15f, cy - size * 0.35f, size * 0.1f, paint);
    }

    private void drawTopBar(Canvas canvas, int W, int H, GameState state) {
        float logoX = dp(16);
        float logoY = dp(28);

        paint.setColor(0xFFE91E63);
        float hx = logoX + dp(10);
        float hy = logoY - dp(4);
        canvas.drawCircle(hx - dp(5), hy, dp(7), paint);
        canvas.drawCircle(hx + dp(5), hy, dp(7), paint);
        Path heart = new Path();
        heart.moveTo(hx - dp(12), hy + dp(2));
        heart.lineTo(hx, hy + dp(14));
        heart.lineTo(hx + dp(12), hy + dp(2));
        heart.close();
        canvas.drawPath(heart, paint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(dp(18));
        textPaint.setColor(0xFF2E7D32);
        textPaint.setFakeBoldText(true);
        canvas.drawText("AXIEFINITY", logoX + dp(28), logoY + dp(4), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.CENTER);

        int totalAxp = 0;
        for (Creature c : state.player.creatures) totalAxp += c.axp;
        int slp = 50 + state.player.battlesWon * 15;

        float pillH = dp(28);
        float pillY = dp(12);
        float gap = dp(6);
        float right = W - dp(12);

        right = drawCurrencyPill(canvas, right, pillY, pillH, 0xFF42A5F5, "\u2726", String.valueOf(totalAxp), gap);
        right = drawCurrencyPill(canvas, right, pillY, pillH, 0xFFAB47BC, "\u25C6", String.valueOf(state.player.shards), gap);
        drawCurrencyPill(canvas, right, pillY, pillH, 0xFF66BB6A, "\u2665", String.valueOf(slp), gap);
    }

    private float drawCurrencyPill(Canvas canvas, float right, float y, float h, int color, String icon, String value, float gap) {
        textPaint.setTextSize(dp(12));
        float textW = textPaint.measureText(value);
        float pillW = dp(28) + textW + dp(14);
        float left = right - pillW;

        paint.setColor(0x33000000);
        canvas.drawRoundRect(left + dp(2), y + dp(2), right + dp(2), y + h + dp(2), h / 2, h / 2, paint);
        paint.setColor(0xEEFFFFFF);
        canvas.drawRoundRect(left, y, right, y + h, h / 2, h / 2, paint);
        paint.setColor(color);
        canvas.drawCircle(left + h / 2, y + h / 2, h * 0.38f, paint);
        textPaint.setTextSize(dp(11));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, left + h / 2, y + h * 0.68f, textPaint);
        textPaint.setTextSize(dp(12));
        textPaint.setColor(0xFF333333);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(value, left + h + dp(4), y + h * 0.68f, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        return left - gap;
    }

    private void drawShowcaseCreatures(Canvas canvas, int W, int H, float t, GameState state, PartComposer composer) {
        List<Creature> show = new ArrayList<>();
        for (Creature c : state.player.creatures) {
            if (!c.isEgg) {
                show.add(c);
                if (show.size() >= 3) break;
            }
        }
        if (show.isEmpty()) return;

        float centerY = H * 0.52f;
        float spacing = W * 0.22f;
        float startX = W * 0.55f - (show.size() - 1) * spacing / 2f;

        for (int i = 0; i < show.size(); i++) {
            Creature c = show.get(i);
            float bob = (float) Math.sin(t * 2.2 + i * 1.3) * dp(6);
            float x = startX + i * spacing;
            float y = centerY + bob;
            float size = dp(110);

            paint.setColor(0x44000000);
            canvas.drawOval(x - size * 0.28f, y + size * 0.32f, x + size * 0.28f, y + size * 0.42f, paint);
            composer.drawCreature(canvas, c, x, y, size);

            float bx = x + size * 0.28f;
            float by = y - size * 0.42f;
            paint.setColor(0xFFFFFFFF);
            canvas.drawRoundRect(bx - dp(10), by - dp(16), bx + dp(10), by + dp(6), dp(6), dp(6), paint);
            Path tail = new Path();
            tail.moveTo(bx - dp(4), by + dp(4));
            tail.lineTo(bx + dp(2), by + dp(14));
            tail.lineTo(bx + dp(6), by + dp(4));
            tail.close();
            canvas.drawPath(tail, paint);
            textPaint.setTextSize(dp(14));
            textPaint.setColor(0xFFE53935);
            textPaint.setFakeBoldText(true);
            canvas.drawText("!", bx, by + dp(2), textPaint);
            textPaint.setFakeBoldText(false);

            textPaint.setTextSize(dp(11));
            textPaint.setColor(0xFF1B5E20);
            canvas.drawText(c.name, x, y + size * 0.48f, textPaint);
        }
    }

    private void drawSideMenu(Canvas canvas, int W, int H, List<RectF> buttons, List<String> buttonActions) {
        String[] labels = {"Shop", "Axies", "Teams", "Inventory", "Collection", "Craft"};
        String[] actions = {"shop", "collection", "battle", "inventory", "collection", "breed"};
        int[] colors = {0xFFFF8A65, 0xFF81C784, 0xFF64B5F6, 0xFFFFD54F, 0xFFBA68C8, 0xFF4DB6AC};

        float btnW = dp(72);
        float btnH = dp(36);
        float left = dp(10);
        float startY = H * 0.22f;
        float gap = dp(8);

        for (int i = 0; i < labels.length; i++) {
            float y = startY + i * (btnH + gap);
            RectF r = new RectF(left, y, left + btnW, y + btnH);
            paint.setColor(0x44000000);
            canvas.drawRoundRect(r.left + dp(2), r.top + dp(2), r.right + dp(2), r.bottom + dp(2), dp(18), dp(18), paint);
            paint.setColor(0xF2FFFFFF);
            canvas.drawRoundRect(r, dp(18), dp(18), paint);
            paint.setColor(colors[i]);
            canvas.drawCircle(left + dp(16), y + btnH / 2, dp(10), paint);
            textPaint.setTextSize(dp(11));
            textPaint.setColor(0xFF333333);
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(labels[i], left + dp(30), y + btnH * 0.65f, textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            buttons.add(r);
            buttonActions.add(actions[i]);
        }
    }

    private void drawPlayButton(Canvas canvas, int W, int H, List<RectF> buttons, List<String> buttonActions) {
        float bw = dp(130);
        float bh = dp(52);
        float right = W - dp(16);
        float bottom = H - dp(24);
        RectF r = new RectF(right - bw, bottom - bh, right, bottom);

        paint.setColor(0x66000000);
        canvas.drawRoundRect(r.left + dp(3), r.top + dp(4), r.right + dp(3), r.bottom + dp(4), dp(26), dp(26), paint);
        paint.setShader(new LinearGradient(r.left, r.top, r.left, r.bottom, 0xFFEF5350, 0xFFC62828, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, dp(26), dp(26), paint);
        paint.setShader(null);
        paint.setColor(0x44FFFFFF);
        canvas.drawRoundRect(r.left + dp(8), r.top + dp(4), r.right - dp(8), r.top + dp(18), dp(10), dp(10), paint);
        textPaint.setTextSize(dp(22));
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        canvas.drawText("PLAY", r.centerX(), r.centerY() + dp(8), textPaint);
        textPaint.setFakeBoldText(false);
        buttons.add(r);
        buttonActions.add("battle");
    }
}
