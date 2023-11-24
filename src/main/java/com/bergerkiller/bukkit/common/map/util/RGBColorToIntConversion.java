package com.bergerkiller.bukkit.common.map.util;

import java.util.function.Supplier;
import java.util.stream.IntStream;

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

    /**
     * Decodes int-encoded pixel data
     *
     * @param data Pixel data
     * @param pixelCount Pixel count
     * @param consumer Consumer callback to call for every pixel
     * @see Decoder
     */
    default void decode(int[] data, int pixelCount, RGBColorConsumer consumer) {
        new Decoder(this) {
            @Override
            public void onPixel(int index, int rgba) {
                consumer.onPixel(index, rgba);
            }
        }.decode(data, pixelCount);
    }

    /**
     * Decodes byte-encoded pixel data
     *
     * @param data Pixel data
     * @param pixelCount Pixel count
     * @param consumer Consumer callback to call for every pixel
     * @see Decoder
     */
    default void decode(byte[] data, int pixelCount, RGBColorConsumer consumer) {
        new Decoder(this) {
            @Override
            public void onPixel(int index, int rgba) {
                consumer.onPixel(index, rgba);
            }
        }.decode(data, pixelCount);
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

    /**
     * Consumes pixel RGB(A) values
     */
    @FunctionalInterface
    interface RGBColorConsumer {
        /**
         * Callback called for every pixel encountered
         *
         * @param index Index of the pixel
         * @param rgba Red green blue alpha component. For RGB data, the alpha
         *             channel is kept 0.
         */
        void onPixel(int index, int rgba);
    }

    /**
     * Decodes int[] or byte[] data, calling the callback with every pixel encountered.
     * Callback is called on multiple threads by default.
     */
    abstract class Decoder implements RGBColorConsumer {
        private final RGBColorToIntConversion converter;
        private int parallelism;

        public Decoder(RGBColorToIntConversion converter) {
            this.converter = converter;
            this.parallelism = Runtime.getRuntime().availableProcessors();
        }

        /**
         * Sets over how many parallel threads the decoding is performed. If set to 1 or less,
         * this decoder runs single-threaded. If more than 1, {@link #onPixel(int, int)} will be
         * called on multiple (worker) threads.
         * Is by default set to the number of cpu threads.
         *
         * @param parallelism Number of parallel tasks to decode on
         * @return this decoder
         */
        public Decoder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        private int computeParallelism(int pixelCount) {
            // Try to have at least 4 blocks processed per thread
            return Math.min(this.parallelism, pixelCount / (32 * 4));
        }

        /**
         * Decodes byte-encoded pixel data.
         *
         * @param data Pixel data, with 3 or 4 bytes per pixel storing the RGB(A) values
         * @param pixelCount Total number of pixels to decode
         */
        public void decode(byte[] data, int pixelCount) {
            int bytePosition = 0;
            int pixelPosition = 0;

            // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
            // This is a little more performant, especially with SIMD enabled
            {
                final int totalBytes = pixelCount * converter.singleBytesInputLength() - converter.byteBlockInputMinimumLength();
                final int parallelism = computeParallelism(pixelCount);

                if (parallelism > 1) {
                    final int blocksPerThread = (totalBytes / (parallelism * converter.byteBlockInputLength()));
                    final int bytesPerThread = converter.byteBlockInputLength() * blocksPerThread;
                    final int pixelsPerThread = 32 * blocksPerThread;

                    IntStream.range(0, parallelism)
                            .parallel()
                            .forEach(threadId -> {
                                int threadBytePosition = threadId * bytesPerThread;
                                int threadEndPosition = threadBytePosition + bytesPerThread;
                                int threadPixelPosition = threadId * pixelsPerThread;
                                int[] buff = new int[32];
                                while (threadBytePosition < threadEndPosition) {
                                    threadBytePosition = converter.byteBlockConvert32Pixels(data, threadBytePosition, buff);
                                    for (int i = 0; i < 32; i++) {
                                        onPixel(threadPixelPosition++, buff[i]);
                                    }
                                }
                            });

                    bytePosition += parallelism * bytesPerThread;
                    pixelPosition += parallelism * pixelsPerThread;
                } else {
                    int[] buff = new int[32];
                    while (bytePosition < totalBytes) {
                        bytePosition = converter.byteBlockConvert32Pixels(data, bytePosition, buff);
                        for (int i = 0; i < 32; i++) {
                            onPixel(pixelPosition++, buff[i]);
                        }
                    }
                }
            }

            // Perform a simple for loop for the few remaining pixels
            int step = converter.singleBytesInputLength();
            int limit = pixelCount * step;
            while (bytePosition < limit) {
                onPixel(pixelPosition++, converter.singleBytesToInt(data, bytePosition));
                bytePosition += step;
            }
        }

        /**
         * Decodes int-encoded pixel data.
         *
         * @param data Pixel data, with an int per pixel storing the RGB(A) values
         * @param pixelCount Total number of pixels to decode
         */
        public void decode(int[] data, int pixelCount) {
            int intPosition = 0;
            int pixelPosition = 0;

            // Process 32 pixel blocks of data by performing the byte[] conversion in bulk
            // This is a little more performant, especially with SIMD enabled
            {
                final int totalIntegers = pixelCount - 32;
                final int parallelism = computeParallelism(pixelCount);

                if (parallelism > 1) {
                    final int blocksPerThread = (totalIntegers / (parallelism * 32));
                    final int pixelsPerThread = 32 * blocksPerThread;

                    IntStream.range(0, parallelism)
                            .parallel()
                            .forEach(threadId -> {
                                int threadIntPosition = threadId * pixelsPerThread;
                                int threadEndPosition = threadIntPosition + pixelsPerThread;
                                int threadPixelPosition = threadId * pixelsPerThread;
                                int[] buff = new int[32];
                                while (threadIntPosition < threadEndPosition) {
                                    threadIntPosition = converter.intBlockConvert32Pixels(data, threadIntPosition, buff);
                                    for (int i = 0; i < 32; i++) {
                                        onPixel(threadPixelPosition++, buff[i]);
                                    }
                                }
                            });

                    intPosition += parallelism * pixelsPerThread;
                    pixelPosition += parallelism * pixelsPerThread;
                } else {
                    int[] buff = new int[32];
                    while (intPosition < totalIntegers) {
                        intPosition = converter.intBlockConvert32Pixels(data, intPosition, buff);
                        for (int i = 0; i < 32; i++) {
                            onPixel(pixelPosition++, buff[i]);
                        }
                    }
                }
            }

            // Perform a simple for loop for the few remaining pixels
            while (intPosition < pixelCount) {
                onPixel(pixelPosition++, converter.singleIntToInt(data[intPosition]));
                intPosition++;
            }
        }
    }
}
