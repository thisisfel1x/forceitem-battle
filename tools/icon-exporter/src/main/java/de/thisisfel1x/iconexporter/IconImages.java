package de.thisisfel1x.iconexporter;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Pixel operations on {@link NativeImage}s. All colors are ARGB
 */
final class IconImages {

    private IconImages() {
    }

    /**
     * Recovers a transparent image from the same content rendered on black and on white
     */
    static NativeImage extract(NativeImage onBlack, NativeImage onWhite, int x0, int y0, int size) {
        NativeImage icon = new NativeImage(size, size, true);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int black = onBlack.getPixel(x0 + x, y0 + y);
                int white = onWhite.getPixel(x0 + x, y0 + y);

                int difference = (red(white) - red(black)) + (green(white) - green(black)) + (blue(white) - blue(black));
                int alpha = clamp(255 - Math.round(difference / 3f));
                if (alpha == 0) {
                    icon.setPixel(x, y, 0);
                    continue;
                }

                icon.setPixel(x, y, argb(alpha,
                        unpremultiply(red(black), alpha),
                        unpremultiply(green(black), alpha),
                        unpremultiply(blue(black), alpha)));
            }
        }
        return icon;
    }

    /**
     * Box filter downscale with alpha weighting, so transparent pixels do not darken the edges.
     * The source size must be a multiple of the target size
     */
    static NativeImage downscale(NativeImage source, int targetSize) {
        int factor = source.getWidth() / targetSize;
        if (factor <= 1) {
            NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
            copy.copyFrom(source);
            return copy;
        }

        NativeImage target = new NativeImage(targetSize, targetSize, true);
        int samples = factor * factor;

        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                long alphaSum = 0, redSum = 0, greenSum = 0, blueSum = 0;

                for (int dy = 0; dy < factor; dy++) {
                    for (int dx = 0; dx < factor; dx++) {
                        int pixel = source.getPixel(x * factor + dx, y * factor + dy);
                        int alpha = alpha(pixel);
                        alphaSum += alpha;
                        redSum += (long) red(pixel) * alpha;
                        greenSum += (long) green(pixel) * alpha;
                        blueSum += (long) blue(pixel) * alpha;
                    }
                }

                if (alphaSum == 0) {
                    target.setPixel(x, y, 0);
                    continue;
                }
                target.setPixel(x, y, argb(
                        (int) Math.round((double) alphaSum / samples),
                        (int) Math.round((double) redSum / alphaSum),
                        (int) Math.round((double) greenSum / alphaSum),
                        (int) Math.round((double) blueSum / alphaSum)));
            }
        }
        return target;
    }

    private static int unpremultiply(int channel, int alpha) {
        return clamp(Math.round(channel * 255f / alpha));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}
