package com.axieoffline.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.LruCache;

/**
 * Composes a creature sprite from layered body-part PNGs based on dominant genes.
 * Layer order: Back -> Tail -> Body -> Mouth -> Horn -> Ears -> Eyes
 */
public class PartComposer {

    private static final int PART_SIZE = 256;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(128) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private final Context context;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    public PartComposer(Context context) {
        this.context = context.getApplicationContext();
    }

    public Bitmap compose(Creature creature) {
        String cacheKey = buildCacheKey(creature);
        Bitmap cached = CACHE.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap result = Bitmap.createBitmap(PART_SIZE, PART_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        String cls = creature.axieClass.name().toLowerCase();

        // Back (slot 4 in BodyPart enum order: EYES=0,EARS=1,HORN=2,MOUTH=3,BACK=4,TAIL=5)
        drawPart(canvas, resolveName("back", cls, geneVariant(creature, Creature.BodyPart.BACK)));
        // Tail
        drawPart(canvas, resolveName("tail", cls, geneVariant(creature, Creature.BodyPart.TAIL)));
        // Body
        drawPart(canvas, "body_" + cls);
        // Mouth
        drawPart(canvas, resolveName("mouth", cls, geneVariant(creature, Creature.BodyPart.MOUTH)));
        // Horn
        drawPart(canvas, resolveName("horn", cls, geneVariant(creature, Creature.BodyPart.HORN)));
        // Ears
        drawPart(canvas, resolveName("ears", cls, geneVariant(creature, Creature.BodyPart.EARS)));
        // Eyes
        drawPart(canvas, resolveName("eyes", cls, geneVariant(creature, Creature.BodyPart.EYES)));

        CACHE.put(cacheKey, result);
        return result;
    }

    public void drawCreature(Canvas canvas, Creature creature, float cx, float cy, float size) {
        Bitmap bmp = compose(creature);
        if (bmp == null) {
            drawFallback(canvas, creature, cx, cy, size);
            return;
        }
        float scale = size / PART_SIZE;
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postTranslate(cx - size / 2f, cy - size / 2f);
        canvas.drawBitmap(bmp, m, paint);
    }

    private int geneVariant(Creature c, Creature.BodyPart part) {
        int idx = part.ordinal();
        if (c.genes == null || idx >= c.genes.length || c.genes[idx] == null) {
            return Math.abs(c.uid.hashCode() + idx) % 12;
        }
        Creature.Gene g = c.genes[idx].dominant;
        if (g == null || g.partId == null) {
            return Math.abs(c.uid.hashCode() + idx) % 12;
        }
        // Extract variant from partId if present, else hash
        String id = g.partId;
        int v = Math.abs(id.hashCode()) % 12;
        // Prefer explicit _N suffix
        int us = id.lastIndexOf('_');
        if (us >= 0 && us < id.length() - 1) {
            try {
                v = Integer.parseInt(id.substring(us + 1)) % 12;
            } catch (NumberFormatException ignored) {}
        }
        return v;
    }

    private String resolveName(String slot, String cls, int variant) {
        return slot + "_" + cls + "_" + variant;
    }

    private void drawPart(Canvas canvas, String resName) {
        Bitmap bmp = loadBitmap(resName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0, 0, paint);
        }
    }

    private Bitmap loadBitmap(String resName) {
        Bitmap cached = CACHE.get("raw_" + resName);
        if (cached != null && !cached.isRecycled()) return cached;

        Resources res = context.getResources();
        int id = res.getIdentifier(resName, "drawable", context.getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeResource(res, id, opts);
        if (bmp != null) {
            CACHE.put("raw_" + resName, bmp);
        }
        return bmp;
    }

    private String buildCacheKey(Creature c) {
        StringBuilder sb = new StringBuilder(c.axieClass.name());
        if (c.genes != null) {
            for (int i = 0; i < c.genes.length; i++) {
                sb.append('_');
                if (c.genes[i] != null && c.genes[i].dominant != null) {
                    sb.append(c.genes[i].dominant.partId);
                } else {
                    sb.append(i);
                }
            }
        } else {
            sb.append('_').append(c.uid);
        }
        return sb.toString();
    }

    private void drawFallback(Canvas canvas, Creature c, float cx, float cy, float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c.getClassColor());
        canvas.drawCircle(cx, cy, size * 0.4f, p);
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size * 0.2f);
        canvas.drawText(c.name != null ? c.name.substring(0, Math.min(3, c.name.length())) : "?", cx, cy + size * 0.07f, p);
    }

    public static void clearCache() {
        CACHE.evictAll();
    }
}
