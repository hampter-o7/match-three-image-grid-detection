package logic;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class EdgeDetection {

    private static final int GRAY_MASK = 0xFF;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_BLACK = 0;

    public static final int[][] SOBEL = {
            { -1, 0, 1 },
            { -2, 0, 2 },
            { -1, 0, 1 }
    };
    public static final int[][] SCHARR = {
            { -3, 0, 3 },
            { -10, 0, 10 },
            { -3, 0, 3 }
    };
    public static final double PRESET_ALPHA = 0.3;
    public static final double PRESET_BETA = 0.5;

    public static BufferedImage getThresholdGradient(BufferedImage image, int[][] filter, double alpha, double beta) {
        AtomicInteger maxGradient = new AtomicInteger(-1);
        BufferedImage gradientImage = calculateGradientAndAngle(image, filter, null, maxGradient);
        BufferedImage thresholdGradient = thresholdGradient(gradientImage, maxGradient, alpha, beta);
        return thresholdGradient;
    }

    private static BufferedImage calculateGradientAndAngle(BufferedImage image, int[][] filter, double[][] angleTable,
            AtomicInteger maxGradient) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage gradientImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 1; i < height - 1; i++) {
            outer: for (int j = 1; j < width - 1; j++) {
                int Gx = 0;
                int Gy = 0;
                for (int y = 0; y < filter.length; y++) {
                    for (int x = 0; x < filter.length; x++) {
                        if ((image.getRGB(j + y - 1, i + x - 1) & GRAY_MASK) > 100) {
                            gradientImage.setRGB(j, i, 0);
                            if (angleTable != null) {
                                angleTable[i][j] = 0;
                            }
                            continue outer;
                        }
                        Gx += filter[y][x] * (image.getRGB(j + y - 1, i + x - 1) & GRAY_MASK);
                        Gy += filter[x][y] * (image.getRGB(j + y - 1, i + x - 1) & GRAY_MASK);
                    }
                }
                double G = Math.sqrt(Math.pow(Gx, 2) + Math.pow(Gy, 2));
                int roundedG = (int) Math.round(G);
                if (roundedG > maxGradient.get()) {
                    maxGradient.set(roundedG);
                }
                int gradientRgb = roundedG << 16 | roundedG << 8 | roundedG;
                gradientImage.setRGB(j, i, gradientRgb);
                if (angleTable == null) {
                    continue;
                }
                double angle = Math.atan2(Gy, Gx) * 180 / Math.PI;
                if (angle < 0) {
                    angle += 180;
                }
                angleTable[i][j] = angle;
            }
        }
        SaveImage.saveImage(gradientImage, "gradient");
        return gradientImage;
    }

    private static BufferedImage thresholdGradient(BufferedImage image, AtomicInteger maxGradient, double alpha,
            double beta) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage thresholdGradient = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int minThreshold = (int) Math.round(beta * alpha * maxGradient.get());
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int amplitude = image.getRGB(j, i) & GRAY_MASK;
                if (amplitude > minThreshold) {
                    thresholdGradient.setRGB(j, i, COLOR_WHITE);
                }
            }
        }
        SaveImage.saveImage(thresholdGradient, "threshold-gradient");
        return thresholdGradient;
    }

    @Deprecated
    public static BufferedImage getCannyEdge(BufferedImage image, int[][] filter, double alpha, double beta) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] angleTable = new double[height][width];
        AtomicInteger maxGradient = new AtomicInteger(-1);
        BufferedImage gradientImage = calculateGradientAndAngle(image, filter, angleTable, maxGradient);
        BufferedImage cannyEdge = makeCannyEdge(gradientImage, angleTable);
        BufferedImage hysteresisThreshold = hysteresisThreshold(cannyEdge, maxGradient, alpha, beta);
        return hysteresisThreshold;
    }

    @Deprecated
    private static BufferedImage makeCannyEdge(BufferedImage image, double[][] angleTable) {

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage cannyEdge = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                int q = -1;
                int r = -1;

                if (0 <= angleTable[i][j] || angleTable[i][j] < 22.5 || 157.5 <= angleTable[i][j]
                        || angleTable[i][j] < 180) {
                    q = image.getRGB(j, i + 1) & GRAY_MASK;
                    r = image.getRGB(j, i - 1) & GRAY_MASK;
                } else if (22.5 <= angleTable[i][j] || angleTable[i][j] < 67.5) {
                    q = image.getRGB(j + 1, i - 1) & GRAY_MASK;
                    r = image.getRGB(j - 1, i + 1) & GRAY_MASK;
                } else if (67.5 <= angleTable[i][j] || angleTable[i][j] < 112.5) {
                    q = image.getRGB(j + 1, i) & GRAY_MASK;
                    r = image.getRGB(j - 1, i) & GRAY_MASK;
                } else if (122.5 <= angleTable[i][j] || angleTable[i][j] < 157.5) {
                    q = image.getRGB(j - 1, i - 1) & GRAY_MASK;
                    r = image.getRGB(j + 1, i + 1) & GRAY_MASK;
                }

                if ((image.getRGB(j, i) & GRAY_MASK) >= q && (image.getRGB(j, i) & GRAY_MASK) >= r) {
                    cannyEdge.setRGB(j, i, image.getRGB(j, i));
                } else {
                    cannyEdge.setRGB(j, i, COLOR_BLACK);
                }
            }
        }
        SaveImage.saveImage(cannyEdge, "canny-edge");
        return cannyEdge;
    }

    @Deprecated
    private static BufferedImage hysteresisThreshold(BufferedImage image, AtomicInteger maxGradient, double alpha,
            double beta) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage hysteresisThreshold = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int maxThreshold = (int) Math.round(alpha * maxGradient.get());
        int minThreshold = (int) Math.round(beta * maxThreshold);
        ArrayList<int[]> weakPixels = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int amplitude = image.getRGB(j, i) & GRAY_MASK;
                if (amplitude <= minThreshold) {
                    hysteresisThreshold.setRGB(j, i, COLOR_BLACK);
                } else if (amplitude >= maxThreshold) {
                    hysteresisThreshold.setRGB(j, i, COLOR_WHITE);
                } else {
                    weakPixels.add(new int[] { j, i });
                }
            }
        }

        for (int[] weakPixel : weakPixels) {
            int y = weakPixel[0];
            int x = weakPixel[1];
            int a = y != 0 ? -1 : 0;
            int b = y != height ? 1 : 0;
            int c = x != 0 ? -1 : 0;
            int d = x != width ? 1 : 0;
            outer: for (int i = a; i < b; i++) {
                for (int j = c; j < d; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    if (hysteresisThreshold.getRGB(y + i, x + j) == 255) {
                        hysteresisThreshold.setRGB(y, x, COLOR_WHITE);
                        break outer;
                    }
                }
                hysteresisThreshold.setRGB(y, x, COLOR_BLACK);
            }
        }
        SaveImage.saveImage(hysteresisThreshold, "hysteresis-threshold");
        return hysteresisThreshold;
    }
}