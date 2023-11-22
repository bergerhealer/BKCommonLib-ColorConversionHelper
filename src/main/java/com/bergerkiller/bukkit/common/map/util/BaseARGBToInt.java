package com.bergerkiller.bukkit.common.map.util;

class BaseARGBToInt implements RGBColorToIntConversion {
    @Override
    public boolean hasTransparency() {
        return true;
    }

    @Override
    public int singleBytesInputLength() {
        return 4;
    }

    @Override
    public int singleBytesToInt(byte[] input, int inputOffset) {
        return ((input[inputOffset] & 0xFF) << 24) |
               (input[inputOffset + 1] & 0xFF) |
               ((input[inputOffset + 2] & 0xFF) << 8) |
               ((input[inputOffset + 3] & 0xFF) << 16);
    }

    @Override
    public int singleIntToInt(int input) {
        return ((input >> 16) & 0xFF) | (input & 0xFF00FF00) | ((input & 0xFF) << 16);
    }
}
