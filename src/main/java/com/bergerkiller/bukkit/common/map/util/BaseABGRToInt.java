package com.bergerkiller.bukkit.common.map.util;

class BaseABGRToInt implements RGBColorToIntConversion {
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
               ((input[inputOffset + 1] & 0xFF) << 16) |
               ((input[inputOffset + 2] & 0xFF) << 8) |
               (input[inputOffset + 3] & 0xFF);
    }

    @Override
    public int singleIntToInt(int input) {
        return input;
    }
}
