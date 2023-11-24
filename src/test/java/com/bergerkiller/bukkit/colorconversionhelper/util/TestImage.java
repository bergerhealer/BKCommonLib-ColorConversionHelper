package com.bergerkiller.bukkit.colorconversionhelper.util;

import com.bergerkiller.bukkit.common.map.util.RGBColorToIntConversion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Images used for testing. Includes a ground truth int[] array that the conversions
 * should produce.
 *
 * @param <D> DataBuffer type
 */
public abstract class TestImage<D extends java.awt.image.DataBuffer> {
    public final Type type;
    public final int width;
    public final int height;
    public final int[] truth;
    public final int[] result;
    protected final D imageBuffer;
    protected ConvertMode lastUsedConversionMode;

    public static TestImage<?> load(String imageName, Type type) {
        switch (type) {
            case BYTE_RGB:
            case BYTE_BGR:
            case BYTE_ARGB:
            case BYTE_ABGR:
                return loadByte(imageName, type);
            case INT_RGB:
            case INT_BGR:
            case INT_ARGB:
            case INT_ABGR:
                return loadInt(imageName, type);
            default:
                throw new UnsupportedOperationException("Unknown type: " + type);
        }
    }

    public static TestImageByte loadByte(String imageName, Type type) {
        return new TestImageByte(loadImage(imageName), type);
    }

    public static TestImageInt loadInt(String imageName, Type type) {
        return new TestImageInt(loadImage(imageName), type);
    }

    public static Image loadImage(String imageName) {
        try {
            return ImageIO.read(new File("misc/" + imageName));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load image " + imageName, t);
        }
    }

    protected TestImage(Image inputImage, Type type, Class<D> dataBufferType) {
        this.type = type;
        width = inputImage.getWidth(null);
        height = inputImage.getHeight(null);

        // Decode truth int rgb pixels that our conversion methods should replicate
        {
            BufferedImage imageBuf;
            if (inputImage instanceof BufferedImage) {
                imageBuf = (BufferedImage) inputImage;
            } else {
                imageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D graphics = imageBuf.createGraphics();
                graphics.drawImage(inputImage, 0, 0, null);
                graphics.dispose();
            }

            int[] intPixels = new int[width * height];
            imageBuf.getRGB(0, 0, width, height, intPixels, 0, width);

            // Convert this ARGB color format to the RGBA format that we expect for output
            for (int i = 0; i < intPixels.length; i++) {
                int input = intPixels[i];
                int output = ((input >> 16) & 0xFF) | (input & 0xFF00FF00) | ((input & 0xFF) << 16);
                intPixels[i] = output;
            }

            // If the format does not have transparency, leave the alpha channel 0
            if (!type.hasTransparency()) {
                for (int i = 0; i < intPixels.length; i++) {
                    intPixels[i] &= 0xFFFFFF;
                }
            }

            this.truth = intPixels;
            this.result = new int[intPixels.length]; // Initialized up-front with all 0
        }

        // Encode a raw data buffer in the image format we desire
        {
            BufferedImage image;
            {
                image = new BufferedImage(width, height, type.bufferedImageType);
                Graphics2D g = image.createGraphics();
                g.drawImage(inputImage, 0, 0, null);
                g.dispose();
            }

            this.imageBuffer = dataBufferType.cast(image.getRaster().getDataBuffer());
        }
    }

    /**
     * Performs a conversion from the input image data, to the int[] {@link #result} buffer.
     *
     * @param mode Conversion mode
     * @return this image
     */
    public abstract TestImage<D> convert(ConvertMode mode);

    /**
     * Checks that the previous {@link #convert(ConvertMode) conversion} was correct
     *
     * @param mode Whether to show a window with the image if a failure is detected
     * @return this image
     */
    public TestImage<D> assertCorrect(DebugMode mode) {
        for (int i = 0; i < truth.length; i++) {
            if (truth[i] != result[i]) {
                System.err.println("Type: " + type);
                System.err.println("Conversion Mode: " + lastUsedConversionMode);
                System.err.println("Pixel mismatch at index " + i + " (x=" + (i % width) + " y=" + (i / width) + ")");
                System.err.println("Truth:    0x" + String.format("%08X", truth[i]));
                System.err.println("But was:  0x" + String.format("%08X", result[i]));
                if (mode == DebugMode.SHOW_IMAGE_ON_FAILURE) {
                    show();
                }
                fail("Pixel at index " + i + " does not match");
            }
        }
        return this;
    }

    /**
     * Displays a java awt frame window with the result of the color transformation
     *
     * @return this image
     */
    public TestImage<D> show() {
        // Create a BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get the underlying array from the BufferedImage
        int[] imagePixels = ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // Load the resulting pixels into the imagePixels
        for (int i = 0; i < result.length; i++) {
            int input = result[i];
            int output = ((input >> 16) & 0xFF) | (input & 0xFF00FF00) | ((input & 0xFF) << 16);
            imagePixels[i] = output;
        }

        // Set alpha channel to 255 if data stores no transparency so the window shows stuff
        if (!type.hasTransparency()) {
            for (int i = 0; i < imagePixels.length; i++) {
                imagePixels[i] |= 0xFF000000;
            }
        }

        DebugWindow.showMapForever(image);

        return this;
    }

    public static final class TestImageByte extends TestImage<java.awt.image.DataBufferByte> {
        public final byte[] data;

        public TestImageByte(Image inputImage, Type type) {
            super(inputImage, type, java.awt.image.DataBufferByte.class);
            this.data = imageBuffer.getData();
            type.byteCorrection.accept(this.data);
        }

        @Override
        public TestImageByte convert(ConvertMode mode) {
            lastUsedConversionMode = mode;
            type.conversion(mode).byteBufferToIntRGB(data, result.length, result);
            return this;
        }

        @Override
        public TestImageByte assertCorrect(DebugMode mode) {
            return (TestImageByte) super.assertCorrect(mode);
        }

        @Override
        public TestImageByte show() {
            return (TestImageByte) super.show();
        }
    }

    public static final class TestImageInt extends TestImage<java.awt.image.DataBufferInt> {
        public final int[] data;

        public TestImageInt(Image inputImage, Type type) {
            super(inputImage, type, java.awt.image.DataBufferInt.class);
            this.data = imageBuffer.getData();
            type.intCorrection.accept(this.data);
        }

        @Override
        public TestImageInt convert(ConvertMode mode) {
            lastUsedConversionMode = mode;
            type.conversion(mode).intBufferToIntRGB(data, result.length, result);
            return this;
        }

        @Override
        public TestImageInt assertCorrect(DebugMode mode) {
            return (TestImageInt) super.assertCorrect(mode);
        }

        @Override
        public TestImageInt show() {
            return (TestImageInt) super.show();
        }
    }

    public enum DebugMode {
        /** Default operation */
        DEFAULT,
        /** Shows the converted image in a window on conversion failure */
        SHOW_IMAGE_ON_FAILURE
    }

    public enum ConvertMode {
        /** Base conversion method that doesn't rely on any special cpu capabilities */
        BASE,
        /** Optimized conversion method that makes use of SIMD, if available */
        SIMD
    }

    /**
     * A type of image format that can be loaded. Since BufferedImage does not seem to support
     * all RGB color formats natively, some are emulated.
     */
    public enum Type {
        BYTE_RGB(BufferedImage.TYPE_3BYTE_BGR, RGBColorToIntConversion.RGB, t -> t.byteCorrection = bytes -> {
            for (int i = 0; i < (bytes.length - 2); i += 3) {
                byte b = bytes[i];
                bytes[i] = bytes[i + 2];
                bytes[i + 2] = b;
            }
        }),
        BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR, RGBColorToIntConversion.BGR),
        BYTE_ARGB(BufferedImage.TYPE_4BYTE_ABGR, RGBColorToIntConversion.ARGB, t -> t.byteCorrection = bytes -> {
            for (int i = 0; i < (bytes.length - 3); i += 4) {
                byte b = bytes[i + 1];
                bytes[i + 1] = bytes[i + 3];
                bytes[i + 3] = b;
            }
        }),
        BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR, RGBColorToIntConversion.ABGR),
        INT_RGB(BufferedImage.TYPE_INT_RGB, RGBColorToIntConversion.RGB),
        INT_BGR(BufferedImage.TYPE_INT_BGR, RGBColorToIntConversion.BGR),
        INT_ARGB(BufferedImage.TYPE_INT_ARGB, RGBColorToIntConversion.ARGB),
        INT_ABGR(BufferedImage.TYPE_INT_ARGB, RGBColorToIntConversion.ABGR, t -> t.intCorrection = ints -> {
            for (int i = 0; i < ints.length; i++) {
                int input = ints[i];
                int output = ((input >> 16) & 0xFF) | (input & 0xFF00FF00) | ((input & 0xFF) << 16);
                ints[i] = output;
            }
        });

        private final int bufferedImageType;
        private final RGBColorToIntConversion simdConversion;
        private final RGBColorToIntConversion baseConversion;
        private Consumer<byte[]> byteCorrection = b -> {};
        private Consumer<int[]> intCorrection = i -> {};

        Type(int bufferedImageType, RGBColorToIntConversion conversion, Consumer<Type> adj) {
            this(bufferedImageType, conversion);
            adj.accept(this);
        }

        Type(int bufferedImageType, RGBColorToIntConversion conversion) {
            this.bufferedImageType = bufferedImageType;
            this.simdConversion = conversion;
            this.baseConversion = conversion.noSIMD();
        }

        public boolean hasTransparency() {
            return this.simdConversion.hasTransparency();
        }

        public RGBColorToIntConversion conversion(ConvertMode mode) {
            return (mode == ConvertMode.SIMD) ? simdConversion : baseConversion;
        }
    }
}
