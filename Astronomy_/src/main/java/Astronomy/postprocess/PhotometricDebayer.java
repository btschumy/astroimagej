package Astronomy.postprocess;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import ij.process.IntProcessor;
import ij.process.ShortProcessor;
import util.GenericSwingDialog;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class PhotometricDebayer implements ExtendedPlugInFilter {

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        var gd = new GenericSwingDialog("Debayer");

        AtomicReference<Pallete> pallet = new AtomicReference<>(Pallete.RGGB);
        gd.addChoice("Subpixel arrangement", Pallete.names(), Pallete.RGGB.name(),
                (s) -> pallet.set(Pallete.valueOf(s)));
        gd.centerDialog(true);
        gd.showDialog();

        Color.resetStacks();

        if (gd.wasOKed()) {
            processImage(imp, pallet.get());
            for (Color color : Color.values()) {
                color.makeStackDisplayable(imp.getTitle()).show();
            }
        }

        return DOES_16 | NO_CHANGES | DONE;
    }

    @Override
    public void setNPasses(int nPasses) {
        // STUB
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        if (imp.getWidth() % 2 != 0 && imp.getHeight() % 2 != 0) {
            IJ.error("Image is not in the form of 2x2 subpixels.");
            return DONE;
        }
        return DOES_16;
    }

    @Override
    public void run(ImageProcessor ip) {
        // Not called
    }

    private void processImage(ImagePlus imp, Pallete pallete) {
        var stackSize = imp.getStackSize();
        var stack = imp.getStack();

        for (int slice = 1; slice <= stackSize; slice++) {
            var mim = MetaImage.createImage(stack.getProcessor(slice));
            for (Color color : Color.values()) {
                //todo header copy is broken
                color.stack.addSlice(stack.getSliceLabel(slice), mim.makeImageProcessor(pallete, color));
            }
        }
    }

    // raw coords -> metacoords (half the size of the original, represent the full subbixel array (2x2 of raw pixels))

    /**
     * x is width, y is height. Top left pixel is 0, 0
     */
    record MetaImage(MetaPixel[] pixels, int width, int height) {
        public MetaImage(int width, int height) {
            this(new MetaPixel[width * height], width, height);
        }

        private MetaImage(ImageProcessor ip) {
            this(ip.getWidth() / 2, ip.getHeight() / 2);
            if (!(ip instanceof ShortProcessor)) throw new InvalidParameterException("Must be 16-bit image.");
        }

        public MetaPixel getMetaPixel(int x, int y) {
            return pixels[x + (y * width)];
        }

        public static MetaImage createImage(ImageProcessor ip) {
            var mim = new MetaImage(ip);

            var pixels = ((short[]) ip.getPixels());
            for (int x = 0; x < mim.width(); x++) {
                for (int y = 0; y < mim.height(); y++) {
                    var xi = 2*x;//todo check coord conversion
                    var yi = 2*y;

                    // Extract subpixels
                    var topLeft = pixels[xi + (yi*mim.width())];
                    xi++;
                    var topRight = pixels[xi + (yi*mim.width())];
                    yi++; xi--;
                    var bottomLeft = pixels[xi + (yi*mim.width())];
                    xi++;
                    var bottomRight = pixels[xi + (yi*mim.width())];
                    mim.pixels[x + (y * mim.width)] = new MetaPixel(topLeft, topRight, bottomLeft, bottomRight);
                }
            }

            return mim;
        }

        public ImageProcessor makeImageProcessor(Pallete pallete, Color color) {
            var ip = color.makeImageProcessor.apply(width, height);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    ip.putPixel(x, y, pallete.getColorValue(getMetaPixel(x, y), color));
                }
            }

            return ip;
        }
    }

    /**
     * Describes the 2x2 super/meta pixel of the pallete
     */
    record MetaPixel(short topLeft, short topRight, short bottomLeft, short bottomRight) {}

    enum Pallete {//todo image icon for menu display
        BGGR {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.bottomRight;
                    case BLUE -> metaPixel.topLeft;
                    case GREEN -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        GBRG {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.bottomLeft;
                    case BLUE -> metaPixel.topRight;
                    case GREEN -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        GRBG {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.topRight;
                    case BLUE -> metaPixel.bottomLeft;
                    case GREEN -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        RGGB {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.topLeft;
                    case BLUE -> metaPixel.bottomRight;
                    case GREEN -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        };

        abstract int getColorValue(MetaPixel metaPixel, Color color);

        public static String[] names() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }

        private static short avgGreen(short a, short b) {
            return (short) ((a + b) / 2);
        }

        private static int sum(MetaPixel mp) {
            return mp.topLeft + mp.bottomLeft + mp.bottomRight + mp.topRight;
        }
    }

    enum Color {
        RED,
        GREEN,
        BLUE,
        LUMINOSITY(IntProcessor::new);

        public final BiFunction<Integer, Integer, ImageProcessor> makeImageProcessor;

        Color() {
            makeImageProcessor = ShortProcessor::new;
        }

        Color(BiFunction<Integer, Integer, ImageProcessor> makeImageProcessor) {
            this.makeImageProcessor = makeImageProcessor;
        }

        public ImageStack stack = new ImageStack();

        public ImagePlus makeStackDisplayable(String title) {
            if (this == LUMINOSITY) stack.setOptions("32-bit int");//todo when this is set luminosity breaks and the values become dumb
            return new ImagePlus(title + " (" + name() + ")", stack);
        }

        static void resetStacks() {
            for (Color value : values()) {
                value.stack = new ImageStack();
            }
        }
    }
}
