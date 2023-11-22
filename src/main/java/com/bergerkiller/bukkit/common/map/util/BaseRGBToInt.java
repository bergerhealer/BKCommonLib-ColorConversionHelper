package com.bergerkiller.bukkit.common.map.util;

class BaseRGBToInt implements RGBColorToIntConversion {
    @Override
    public boolean hasTransparency() {
        return false;
    }

    @Override
    public int singleBytesInputLength() {
        return 3;
    }

    @Override
    public int singleBytesToInt(byte[] input, int inputOffset) {
        return (input[inputOffset] & 0xFF) | ((input[inputOffset + 1] & 0xFF) << 8) | ((input[inputOffset + 2] & 0xFF) << 16);
    }

    @Override
    public int singleIntToInt(int input) {
        return ((input >> 16) & 0xFF) | (input & 0xFF00) | ((input & 0xFF) << 16);
    }
}
