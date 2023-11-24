package com.bergerkiller.bukkit.common.map.util;

import java.util.function.Supplier;

/**
 * Helper math routines for converting byte[] and int[] RGB(A) pixel data into
 * int RGBA color values that are compatible with the MapColorPalette
 * color mapping data for input.
 */
public interface RGBColorToIntConversion {
    RGBColorToIntConversion BGR = SIMDLoader.tryCreateSIMD(BaseBGRToInt::new, "bgr");
    RGBColorToIntConversion RGB = SIMDLoader.tryCreateSIMD(BaseRGBToInt::new, "rgb");
    RGBColorToIntConversion ABGR = SIMDLoader.tryCreateSIMD(BaseABGRToInt::new, "abgr");
    RGBColorToIntConversion ARGB = SIMDLoader.tryCreateSIMD(BaseARGBToInt::new, "argb");

    /**
     * Gets whether this format supports transparency at all. If false, then
     * the alpha component of the output RGB int value is kept at 0.
     *
     * @return True if the RGB int output includes transparency, False if
     *         this value is kept 0.
     */
    boolean hasTransparency();

    /**
     * Gets whether the JDK17+ experimental vector SIMD API is used to perform color conversions
     *
     * @return True if SIMD is used
     */
    default boolean isUsingSIMD() {
        return false;
    }

    /**
     * If this is {@link #isUsingSIMD()}, returns a conversion mode that doesn't.
     *
     * @return This conversion mode, but with SIMD disabled
     */
    default RGBColorToIntConversion noSIMD() {
        return this;
    }

    /**
     * The number of input bytes of a single color value.
     * {@link #byteBlockInputLength()} is a multiple of this value.
     *
     * @return Single color input length. Is typically 3 or 4.
     */
    int singleBytesInputLength();

    /**
     * Performs a single conversion of a single color value
     *
     * @param input Input byte data
     * @param inputOffset Offset into the input array
     * @return Output RGB int value
     */
    int singleBytesToInt(byte[] input, int inputOffset);

    /**
     * Performs a single conversion of a single RGB color value into the
     * correct format RGBA color value
     *
     * @param input Input RGB color value
     * @return Output RGB int value
     */
    int singleIntToInt(int input);

    /**
     * The number of input bytes of a single block of data being converted.
     * Returns {@link #singleBytesInputLength()} x 32.
     *
     * @return Input byte count
     */
    default int byteBlockInputLength() {
        return 32 * singleBytesInputLength();
    }

    /**
     * Minimum number of bytes the input of {@link #byteBlockConvert32Pixels(byte[], int, int[])}
     * requires.
     *
     * @return Minimum input byte count. Can be more than {@link #byteBlockInputLength()}, in which
     *         case extra bytes are read that aren't used in the result.
     */
    default int byteBlockInputMinimumLength() {
        return 32 * singleBytesInputLength();
    }

    /**
     * Converts 32 RGB pixels exactly, storing the result in the 32-length int buffer.
     * The byte buffer input must store at least {@link #byteBlockInputMinimumLength()}
     * byte values.
     *
     * @param input Input byte data
     * @param inputOffset Offset into the input data array
     * @param buffer Output int buffer of length 32
     * @return Input offset advanced by how many bytes were read
     */
    default int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
        int len = singleBytesInputLength();
        for (int i = 0; i < 32; i++) {
            buffer[i] = singleBytesToInt(input, inputOffset);
            inputOffset += len;
        }
        return inputOffset;
    }

    /**
     * Converts 32 RGB pixels exactly, storing the result in the 32-length int buffer.
     *
     * @param input Input int data
     * @param inputOffset Offset into the input data array
     * @param buffer Output int buffer of length 32
     * @return Input offset advanced by how many integers were read
     */
    default int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
        for (int i = 0; i < 32; i++) {
            buffer[i] = singleIntToInt(input[inputOffset++]);
        }
        return inputOffset;
    }


    default void decodeIntBuffer(int[] intPixels, int pixelCount, RGBColorConsumer consumer) {
        int intPosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartIntPosition = pixelCount - 32;
            while (intPosition < maxStartIntPosition) {
                intPosition = intBlockConvert32Pixels(intPixels, intPosition, buff);
                for (int i = 0; i < 32; i++) {
                    consumer.accept(pixelPosition++, buff[i]);
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        while (intPosition < pixelCount) {
            consumer.accept(pixelPosition++, singleIntToInt(intPixels[intPosition]));
            intPosition++;
        }
    }

    default void decodeByteBuffer(byte[] bytePixels, int pixelCount, RGBColorConsumer consumer) {
        int bytePosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartBytePosition = (pixelCount * singleBytesInputLength()) - byteBlockInputMinimumLength();
            while (bytePosition < maxStartBytePosition) {
                bytePosition = byteBlockConvert32Pixels(bytePixels, bytePosition, buff);
                for (int i = 0; i < 32; i++) {
                    consumer.accept(pixelPosition++, buff[i]);
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        int step = singleBytesInputLength();
        int limit = pixelCount * step;
        while (bytePosition < limit) {
            consumer.accept(pixelPosition++, singleBytesToInt(bytePixels, bytePosition));
            bytePosition += step;
        }
    }



    default void intBufferToIntRGB(int[] intPixels, int pixelCount, int[] result) {
        int intPosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartIntPosition = pixelCount - 32;
            while (intPosition < maxStartIntPosition) {
                intPosition = intBlockConvert32Pixels(intPixels, intPosition, buff);
                for (int i = 0; i < 32; i++) {
                    result[pixelPosition++] = buff[i];
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        while (intPosition < pixelCount) {
            result[pixelPosition++] = singleIntToInt(intPixels[intPosition]);
            intPosition++;
        }
    }

    default void byteBufferToIntRGB(byte[] bytePixels, int pixelCount, int[] result) {
        int bytePosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartBytePosition = (pixelCount * singleBytesInputLength()) - byteBlockInputMinimumLength();
            while (bytePosition < maxStartBytePosition) {
                bytePosition = byteBlockConvert32Pixels(bytePixels, bytePosition, buff);
                for (int i = 0; i < 32; i++) {
                    result[pixelPosition++] = buff[i];
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        int step = singleBytesInputLength();
        while (pixelPosition < pixelCount) {
            result[pixelPosition] = singleBytesToInt(bytePixels, bytePosition);
            pixelPosition++;
            bytePosition += step;
        }
    }

    // Probably to be removed
    default void byteBufferToMapColors(byte[] bytePixels, int pixelCount, RGBToMapColorFunction converter, byte[] result) {
        int bytePosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartBytePosition = (pixelCount * singleBytesInputLength()) - byteBlockInputMinimumLength();
            while (bytePosition < maxStartBytePosition) {
                bytePosition = byteBlockConvert32Pixels(bytePixels, bytePosition, buff);
                for (int i = 0; i < 32; i++) {
                    result[pixelPosition++] = converter.toMapColor(buff[i]);
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        int step = singleBytesInputLength();
        int limit = pixelCount * step;
        while (bytePosition < limit) {
            result[pixelPosition++] = converter.toMapColor(singleBytesToInt(bytePixels, bytePosition));
            bytePosition += step;
        }
    }

    // Probably to be removed
    default void intBufferToMapColors(int[] intPixels, int pixelCount, RGBToMapColorFunction converter, byte[] result) {
        int intPosition = 0;
        int pixelPosition = 0;

        // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
        // This is a little more performant, especially with SIMD enabled
        {
            int[] buff = new int[32];
            int maxStartIntPosition = pixelCount - 32;
            while (intPosition < maxStartIntPosition) {
                intPosition = intBlockConvert32Pixels(intPixels, intPosition, buff);
                for (int i = 0; i < 32; i++) {
                    result[pixelPosition++] = converter.toMapColor(buff[i]);
                }
            }
        }

        // Perform a simple for loop for the remaining bytes
        while (intPosition < pixelCount) {
            result[pixelPosition++] = converter.toMapColor(singleIntToInt(intPixels[intPosition]));
            intPosition++;
        }
    }

    /**
     * Hidden logic for initializing the SIMD optimizations
     */
    class SIMDLoader {
        private static Throwable simdError = null;

        /**
         * Gets the error that occurred trying to load the SIMD optimization for the current platform.
         * Returns null if there were no errors and SIMD initialized correctly.
         *
         * @return SIMD loader error, null if none occurred
         */
        public static Throwable getError() {
            // Ensure this stuff is initialized so simdError makes sense
            RGBColorToIntConversion.ARGB.hasTransparency();

            // Return error, if any
            return simdError;
        }

        @SuppressWarnings("Since15")
        private static RGBColorToIntConversion tryCreateSIMD(Supplier<RGBColorToIntConversion> base, String simdFactoryName) {
            String simdName = RGBColorToIntConversion.class.getName();
            simdName = simdName.substring(0, simdName.lastIndexOf('.')) + ".SIMDColorConversion";
            try {
                if (!isVectorModulePresent()) {
                    throw new UnsupportedOperationException("Incubator vector module is not loaded");
                }

                Class<?> simdColorConversionType = Class.forName(simdName);
                java.lang.reflect.Method factoryMethod = simdColorConversionType.getMethod(simdFactoryName);
                factoryMethod.setAccessible(true);
                return (RGBColorToIntConversion) factoryMethod.invoke(null);
            } catch (Throwable t) {
                simdError = t;
                return base.get();
            }
        }

        @SuppressWarnings("Since15")
        private static boolean isVectorModulePresent() {
            try {
                ModuleLayer layer = ModuleLayer.boot();
                java.util.Optional<Module> module = layer.findModule("jdk.incubator.vector");
                return module.isPresent();
            } catch (Throwable t) {
                return false;
            }
        }
    }

    @FunctionalInterface
    interface RGBColorConsumer {
        void accept(int pixelIndex, int rgb);
    }

    /**
     * Maps RGB colors to byte Minecraft map colors
     */
    @FunctionalInterface
    interface RGBToMapColorFunction {
        byte toMapColor(int rgb);
    }
}
