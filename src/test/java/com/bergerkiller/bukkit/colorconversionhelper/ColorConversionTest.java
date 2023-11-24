package com.bergerkiller.bukkit.colorconversionhelper;

import com.bergerkiller.bukkit.colorconversionhelper.util.TestImage;
import com.bergerkiller.bukkit.common.map.util.RGBColorToIntConversion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ColorConversionTest {

    @Test
    public void warnIfSIMDUnavailable() {
        if (!RGBColorToIntConversion.RGB.isUsingSIMD()) {
            System.err.println("SIMD is not available! If the CPU should support it, something is wrong!");
            RGBColorToIntConversion.SIMDLoader.getError().printStackTrace();
        }
    }

    @ParameterizedTest
    @CsvSource({
            // Base operations
            "BASE,  BYTE_RGB,   test_minecraft.jpg",
            "BASE,  BYTE_BGR,   test_minecraft.jpg",
            "BASE,  BYTE_ARGB,  test_leafeon.png",
            "BASE,  BYTE_ABGR,  test_leafeon.png",
            "BASE,  INT_RGB,    test_minecraft.jpg",
            "BASE,  INT_BGR,    test_minecraft.jpg",
            "BASE,  INT_ARGB,   test_leafeon.png",
            "BASE,  INT_ABGR,   test_leafeon.png",

            // SIMD optimizations (if supported on testing machine)
            "SIMD,  BYTE_RGB,   test_minecraft.jpg",
            "SIMD,  BYTE_BGR,   test_minecraft.jpg",
            "SIMD,  BYTE_ARGB,  test_leafeon.png",
            "SIMD,  BYTE_ABGR,  test_leafeon.png",
            "SIMD,  INT_RGB,    test_minecraft.jpg",
            "SIMD,  INT_BGR,    test_minecraft.jpg",
            "SIMD,  INT_ARGB,   test_leafeon.png",
            "SIMD,  INT_ABGR,   test_leafeon.png",
    })
    public void testColorConversion(TestImage.ConvertMode convertMode, TestImage.Type type, String imageName) {
        TestImage.load(imageName, type)
                .convert(convertMode)
                .assertCorrect(TestImage.DebugMode.SHOW_IMAGE_ON_FAILURE);
    }
}
