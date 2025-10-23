package com.bergerkiller.bukkit.common.map.util;

import com.bergerkiller.bukkit.common.map.util.util.TestImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.text.NumberFormat;
import java.util.Locale;

public class BenchmarkTest {
    public static final byte[] mapping = new byte[16777217];
    static {
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = (byte) (i & 0xFF);
        }
        mapping[mapping.length - 1] = 0;
    }

    public static byte getColor(int rgb) {
        if ((rgb & 0x80000000) != 0) {
            return mapping[rgb & 0xFFFFFF];
        } else {
            return 0;
        }
    }

    @ParameterizedTest
    @CsvSource({
            // Base operations
            "BYTE_RGB,   test_minecraft.jpg",
            "BYTE_BGR,   test_minecraft.jpg",
            "BYTE_ARGB,  test_leafeon.png",
            "BYTE_ABGR,  test_leafeon.png",
            "INT_RGB,    test_minecraft.jpg",
            "INT_BGR,    test_minecraft.jpg",
            "INT_ARGB,   test_leafeon.png",
            "INT_ABGR,   test_leafeon.png",
    })
    public void benchmarkConversion(TestImage.Type type, String imageName) {
        double base_cps = TestImage.load(imageName, type)
                .convert(TestImage.ConvertMode.BASE) // Prime
                .assertCorrect(TestImage.DebugMode.SHOW_IMAGE_ON_FAILURE)
                .benchmarkPrime(TestImage.ConvertMode.BASE, 100)
                .benchmark(TestImage.ConvertMode.BASE, 1000);

        double simd_cps = TestImage.load(imageName, type)
                .convert(TestImage.ConvertMode.SIMD) // Prime
                .assertCorrect(TestImage.DebugMode.SHOW_IMAGE_ON_FAILURE)
                .benchmarkPrime(TestImage.ConvertMode.SIMD, 100)
                .benchmark(TestImage.ConvertMode.SIMD, 1000);

        System.err.println("[" + type + "] SIMD " + numberFormat.format(simd_cps / base_cps) + "x" +
                "\t\tbase=" + numberFormat.format(base_cps) + " ops/s" +
                "\t\tsimd=" + numberFormat.format(simd_cps) + " ops/s");
    }

    private static final NumberFormat numberFormat = createNumberFormat(1, 3);

    // Internal use
    public static NumberFormat createNumberFormat(int min_fractionDigits, int max_fractionDigits) {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.ENGLISH);
        fmt.setMinimumFractionDigits(min_fractionDigits);
        fmt.setMaximumFractionDigits(max_fractionDigits);
        fmt.setGroupingUsed(false);
        return fmt;
    }
}
