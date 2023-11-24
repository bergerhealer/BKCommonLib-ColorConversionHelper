package com.bergerkiller.bukkit.common.map.util;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorSpecies;

/**
 * Uses the experimental JDK incubator vector API to perform the block
 * conversion of the input data to RGBA.
 */
abstract class SIMDColorConversion implements RGBColorToIntConversion {
    private final boolean hasTransparency;
    protected final RGBColorToIntConversion base;
    protected final VectorSpecies<Byte> byteSpecies;
    protected final VectorSpecies<Integer> intSpecies;
    protected final VectorShuffle<Byte> shuffle;
    protected final VectorShuffle<Byte> intShuffle;

    public static RGBColorToIntConversion bgr() {
        return opaque(new BaseBGRToInt(), new int[] {2, 1, 0}, new int[] {0, 1, 2, 3});
    }

    public static RGBColorToIntConversion rgb() {
        return opaque(new BaseRGBToInt(), new int[] {0, 1, 2}, new int[] {2, 1, 0, 3});
    }

    public static RGBColorToIntConversion abgr() {
        return transparent(new BaseABGRToInt(), new int[] {3, 2, 1, 0}, new int[] {0, 1, 2, 3});
    }

    public static RGBColorToIntConversion argb() {
        return transparent(new BaseARGBToInt(), new int[] {1, 2, 3, 0}, new int[] {2, 1, 0, 3});
    }

    private static RGBColorToIntConversion transparent(RGBColorToIntConversion base, int[] byte_rgb, int[] int_rgb) {
        int byteVectorLength = ByteVector.SPECIES_PREFERRED.length();

        if (byteVectorLength == 8) {
            // 2 pixels per operation, so 16 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 16; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 2);
                        inputOffset += 2 * 4;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 16; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 2);
                        inputOffset += 2;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 16) {
            // 4 pixels per operation, so 8 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 8; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 4);
                        inputOffset += 4 * 4;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 8; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 4);
                        inputOffset += 4;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 64) {
            // 16 pixels per operation, so 2 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 2; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 16);
                        inputOffset += 16 * 4;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 2; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 16);
                        inputOffset += 16;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 128) {
            // 32 pixels per operation, so 1 operation for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    ByteVector.fromArray(byteSpecies, input, inputOffset)
                            .rearrange(shuffle)
                            .reinterpretAsInts()
                            .intoArray(buffer, 0);
                    return inputOffset + (32 * 4);
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    IntVector.fromArray(intSpecies, input, inputOffset)
                            .reinterpretAsBytes()
                            .rearrange(intShuffle)
                            .reinterpretAsInts()
                            .intoArray(buffer, 0);
                    return inputOffset + 32;
                }
            };
        } else {
            // 8 pixels per operation, so 4 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_256, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 4; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 8);
                        inputOffset += 8 * 4;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 4; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 8);
                        inputOffset += 8;
                    }
                    return inputOffset;
                }
            };
        }
    }

    private static RGBColorToIntConversion opaque(RGBColorToIntConversion base, int[] byte_rgb, int[] int_rgb) {
        int byteVectorLength = ByteVector.SPECIES_PREFERRED.length();

        if (byteVectorLength == 8) {
            // 2 pixels per operation, so 16 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 16; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .withLane(7, (byte) 0)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 2);
                        inputOffset += 2 * 3;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 16; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .and(0xFFFFFF)
                                .intoArray(buffer, i * 2);
                        inputOffset += 2;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 16) {
            // 4 pixels per operation, so 8 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 8; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .withLane(15, (byte) 0)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 4);
                        inputOffset += 4 * 3;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 8; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .and(0xFFFFFF)
                                .intoArray(buffer, i * 4);
                        inputOffset += 4;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 64) {
            // 16 pixels per operation, so 2 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 2; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .withLane(63, (byte) 0)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 16);
                        inputOffset += 16 * 3;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 2; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .and(0xFFFFFF)
                                .intoArray(buffer, i * 16);
                        inputOffset += 16;
                    }
                    return inputOffset;
                }
            };
        } else if (byteVectorLength == 128) {
            // 32 pixels per operation, so 1 operation for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_PREFERRED, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    ByteVector.fromArray(byteSpecies, input, inputOffset)
                            .withLane(127, (byte) 0)
                            .rearrange(shuffle)
                            .reinterpretAsInts()
                            .intoArray(buffer, 0);
                    return inputOffset + (32 * 3);
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    IntVector.fromArray(intSpecies, input, inputOffset)
                            .reinterpretAsBytes()
                            .rearrange(intShuffle)
                            .reinterpretAsInts()
                            .and(0xFFFFFF)
                            .intoArray(buffer, 0);
                    return inputOffset + 32;
                }
            };
        } else {
            // 8 pixels per operation, so 4 operations for all 32 pixels
            return new SIMDColorConversion(ByteVector.SPECIES_256, base, byte_rgb, int_rgb) {
                @Override
                public int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 4; i++) {
                        ByteVector.fromArray(byteSpecies, input, inputOffset)
                                .withLane(31, (byte) 0)
                                .rearrange(shuffle)
                                .reinterpretAsInts()
                                .intoArray(buffer, i * 8);
                        inputOffset += 8 * 3;
                    }
                    return inputOffset;
                }

                @Override
                public int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer) {
                    for (int i = 0; i < 4; i++) {
                        IntVector.fromArray(intSpecies, input, inputOffset)
                                .reinterpretAsBytes()
                                .rearrange(intShuffle)
                                .reinterpretAsInts()
                                .and(0xFFFFFF)
                                .intoArray(buffer, i * 8);
                        inputOffset += 8;
                    }
                    return inputOffset;
                }
            };
        }
    }

    protected SIMDColorConversion(VectorSpecies<Byte> species, RGBColorToIntConversion base, int[] byte_rgb, int[] int_rgb) {
        this.hasTransparency = (byte_rgb.length == 4);
        this.byteSpecies = species;
        this.intSpecies = species.withLanes(int.class);
        this.base = base;

        // Shuffles all bytes to turn it into RGBA where A is set to zero using the last vector lane
        {
            int length = species.length();
            int[] shuffleInts = new int[length];
            int i = 0, ctr = 0;
            while (i < length) {
                shuffleInts[i++] = ctr + byte_rgb[0];
                shuffleInts[i++] = ctr + byte_rgb[1];
                shuffleInts[i++] = ctr + byte_rgb[2];
                if (hasTransparency) {
                    shuffleInts[i++] = ctr + byte_rgb[3];
                    ctr += 4;
                } else {
                    shuffleInts[i++] = length - 1;
                    ctr += 3;
                }
            }
            this.shuffle = VectorShuffle.fromArray(species, shuffleInts, 0);
        }

        // Shuffles all int components to turn it into RGBA
        {
            int length = species.length();
            int[] shuffleInts = new int[length];
            int i = 0, ctr = 0;
            while (i < length) {
                shuffleInts[i++] = ctr + int_rgb[0];
                shuffleInts[i++] = ctr + int_rgb[1];
                shuffleInts[i++] = ctr + int_rgb[2];
                shuffleInts[i++] = ctr + int_rgb[3]; // Transparency is always stored in an int...
                ctr += 4;
            }
            this.intShuffle = VectorShuffle.fromArray(species, shuffleInts, 0);
        }
    }

    @Override
    public boolean isUsingSIMD() {
        return true;
    }

    @Override
    public RGBColorToIntConversion noSIMD() {
        return base;
    }

    @Override
    public boolean hasTransparency() {
        return hasTransparency;
    }

    @Override
    public int singleBytesInputLength() {
        return hasTransparency ? 4 : 3;
    }

    @Override
    public int singleBytesToInt(byte[] input, int inputOffset) {
        return base.singleBytesToInt(input, inputOffset);
    }

    @Override
    public int singleIntToInt(int input) {
        return base.singleIntToInt(input);
    }

    @Override
    public int byteBlockInputMinimumLength() {
        return 32 * 4;
    }

    @Override
    public abstract int byteBlockConvert32Pixels(byte[] input, int inputOffset, int[] buffer);

    @Override
    public abstract int intBlockConvert32Pixels(int[] input, int inputOffset, int[] buffer);
}
