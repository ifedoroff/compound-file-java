package com.ifedorov.cfbf;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import org.apache.commons.lang3.ArrayUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class Utils {
    public static final byte[] DISECT_MARK = Utils.toBytesLE(0xfffffffc,4 );
    public static final int DISECT_MARK_INT = Utils.toInt(Utils.toBytesLE(0xfffffffc,4 ));
    public static final byte[] FATSECT_MARK = Utils.toBytesLE(0xfffffffd, 4);
    public static final int FATSECT_MARK_INT = Utils.toInt(Utils.toBytesLE(0xfffffffd, 4));
    public static final byte[] ENDOFCHAIN_MARK = Utils.toBytesLE(0xfffffffe, 4);
    public static final int ENDOFCHAIN_MARK_INT = Utils.toInt(Utils.toBytesLE(0xfffffffe, 4));
    public static final byte[] FREESECT_MARK_OR_NOSTREAM = Utils.toBytesLE(0xffffffff, 4);
    public static final int FREESECT_MARK_OR_NOSTREAM_INT = Utils.toInt(Utils.toBytesLE(0xffffffff, 4));
    public static final byte[] MAX_POSSIBLE_POSITION = Utils.toBytesLE(0xfffffffa, 4);
    public static final long DIFF_BETWEEN_EPOCHS_1970_1601 = 11644473599996l;

    public static byte[] toBytesLE(long l, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static byte[] toBytesBE(long l, int length) {
        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long toLongLE(byte[] b) {
        long result = 0;
        for (int i = b.length - 1; i >= 0; i--) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static long toLongBE(byte[] b) {
        long result = 0;
        for (int i = 0; i < b.length; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static UUID uuidFromByteLE(byte[] sourceBytes) {
        byte[] mostSignificant = new byte[8];
        mostSignificant[0] = sourceBytes[3];
        mostSignificant[1] = sourceBytes[2];
        mostSignificant[2] = sourceBytes[1];
        mostSignificant[3] = sourceBytes[0];
        mostSignificant[4] = sourceBytes[5];
        mostSignificant[5] = sourceBytes[4];
        mostSignificant[6] = sourceBytes[7];
        mostSignificant[7] = sourceBytes[6];
        byte[] leastSignificant = new byte[8];
        System.arraycopy(sourceBytes, 8, leastSignificant, 0, 8);
        return new UUID(Utils.toLongBE(mostSignificant), Utils.toLongBE(leastSignificant));
    }

    public static byte[] uuidToBytesLE(UUID uuid) {
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        byte[] mostSignificant = toBytesBE(mostSignificantBits, 8);
        byte[] leastSignificant = toBytesBE(leastSignificantBits, 8);
        ArrayUtils.swap(mostSignificant, 0, 3);
        ArrayUtils.swap(mostSignificant, 1, 2);
        ArrayUtils.swap(mostSignificant, 4, 5);
        ArrayUtils.swap(mostSignificant, 6, 7);
        return ArrayUtils.addAll(mostSignificant, leastSignificant);
    }

    public static int toInt(byte[] bytes) {
        if(bytes.length == 2) {
            return (((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF));
        } else if(bytes.length == 4) {
            return (bytes[0] & 0xFF) |
                    ((bytes[1] & 0xFF) << 8) |
                    ((bytes[2] & 0xFF) << 16) |
                    ((bytes[3] & 0xFF) << 24);
        } else {
            throw new IllegalArgumentException("Cannot convert bytes to int: " + Arrays.toString(bytes));
        }
    }

    public static double toDoubleLE(byte[] bytes) {
        int exponent = (((bytes[7] & 0b01111111) << 4) | (bytes[6] >> 4)) - 1023;
        double fraction = 0;
        for (int i = 1; i <= 52; i++) {
            int bitPosition = (52 - i) % 8;
            int bytePosition = bytes[(52 - i) / 8];
            fraction += (((bytePosition & 0xFF) >> bitPosition) & 0b00000001) * Math.pow(2, - i);
        }
        return Math.pow(2, exponent) * (1 + fraction);
    }

    public static boolean toBoolean(byte bit) {
        return (bit & 1) == 1;
    }

    public static Date toTime(byte[] bytes) {
        return new Date(Utils.toLongLE(bytes) / 10000 - DIFF_BETWEEN_EPOCHS_1970_1601);
    }

    public static byte[] initializedWith(int size, byte value) {
        byte[] data = new byte[size];
        Arrays.fill(data, value);
        return data;
    }

    public static byte[] initializedWith(int size, byte[] value) {
        Verify.verify(size % value.length == 0);
        byte[] data = new byte[size];
        fill(data, value);
        return data;
    }

    public static byte[] initializedWith(int size, int value) {
        byte[] data = new byte[size];
        Arrays.fill(data, (byte)value);
        return data;
    }

    public static boolean isEndOfChain(Integer value) {
        return value.equals(ENDOFCHAIN_MARK_INT);
    }

    public static boolean isEndOfChain(byte[] value) {
        return Arrays.equals(ENDOFCHAIN_MARK, value);
    }

    public static boolean isFreeSectOrNoStream(byte[] value) {
        return Arrays.equals(FREESECT_MARK_OR_NOSTREAM, value);
    }

    public static boolean isFreeSectOrNoStream(Integer value) {
        return value.equals(FREESECT_MARK_OR_NOSTREAM_INT);
    }

    public static String toHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static String toHex(byte[] bytes) {
        return Joiner.on("").join(FluentIterable.from(ArrayUtils.toObject(bytes)).transform(Utils::toHex).toList().reverse());
    }

    public static byte[] hexToByteLE(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        ArrayUtils.reverse(data);
        return data;
    }

    public static String toUTF16String(byte[] bytes)  {
        StringBuilder builder = new StringBuilder(bytes.length/2);
        for (int i = 0; i < bytes.length; i+=2) {
            builder.append(new String(new byte[]{bytes[i+1], bytes[i]}, StandardCharsets.UTF_16BE));
        }
        return builder.toString();
    }

    public static byte[] toUTF16Bytes(String string) {
        byte[] bytes = new byte[string.length() * 2];
        for (int i = 0; i < string.length(); i++) {
            byte[] charBytes = new byte[0];
            charBytes = string.substring(i, i + 1).getBytes(StandardCharsets.UTF_16BE);
            if(charBytes.length != 2) {
                throw new IllegalStateException("Each character in UTF-16 encoding should be presented with 2 bytes");
            }
            bytes[2*i+1] = charBytes[0];
            bytes[2*i] = charBytes[1];
        }
        return bytes;
    }

    public static byte[] addTrailingZeros(byte[] original, int maximumLength) {
        byte[] result = new byte[maximumLength];
        System.arraycopy(original, 0, result, 0, original.length);
        for (int i = original.length; i < maximumLength; i++) {
            result[i] = 0;
        }
        return result;
    }

    public static String toUTF8WithNoTrailingZeros(byte[] bytes) {
        return new String(Utils.toUTF16String(removeTrailingZeros(bytes)).getBytes(), StandardCharsets.UTF_8);
    }

    public static byte[] removeTrailingZeros(byte[] bytes) {
        int resultingLength = bytes.length;
        for (int i = bytes.length - 1; i > 0; i-=2) {
            if(bytes[i] == 0 && bytes[i-1] == 0) {
                resultingLength = i-1;
            } else {
                break;
            }
        }
        return ArrayUtils.subarray(bytes, 0, resultingLength);
    }

    public static void fill(byte[] target, byte[] filler) {
        Verify.verify(target.length % filler.length == 0);
        int step = filler.length;
        for (int i = 0; i < target.length; i+=step) {
            System.arraycopy(filler, 0, target, i, step);
        }
    }

    public static byte[] copy(byte[] bytes) {
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        return copy;
    }

}
