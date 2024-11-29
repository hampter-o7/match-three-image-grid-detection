package logic;

@FunctionalInterface
public interface GrayScaleFunction {
    int apply(int red, int green, int blue);
}