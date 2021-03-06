package com.ifedorov.cfbf;

import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ifedorov.cfbf.Header.*;
import static com.ifedorov.cfbf.Header.FLAG_POSITION.DIFAT_ENTRIES_FIRST_POSITION;
import static org.junit.jupiter.api.Assertions.*;

class HeaderTest {

    private static final byte[] DUMMY_HEADER = new byte[HEADER_LENGTH];
    static {
        System.arraycopy(Header.HEADER_SIGNATURE, 0, DUMMY_HEADER, FLAG_POSITION.SIGNATURE, 8);
        System.arraycopy(Header.MINOR_VERSION_3, 0, DUMMY_HEADER, FLAG_POSITION.MINOR_VERSION, 2);
        System.arraycopy(Header.MAJOR_VERSION_3, 0, DUMMY_HEADER, FLAG_POSITION.MAJOR_VERSION, 2);
        System.arraycopy(Header.BYTE_ORDER_LITTLE_ENDIAN, 0, DUMMY_HEADER, FLAG_POSITION.BYTE_ORDER, 2);
        System.arraycopy(Header.SECTOR_SHIFT_VERSION_3, 0, DUMMY_HEADER, FLAG_POSITION.SECTOR_SHIFT, 2);
        System.arraycopy(Header.MINI_SECTOR_SHIFT_VERSION_3, 0, DUMMY_HEADER, FLAG_POSITION.MINI_SECTOR_SHIFT, 2);
        System.arraycopy(Header.MINI_STREAM_CUTOFF_SIZE, 0, DUMMY_HEADER, FLAG_POSITION.MINI_STREAM_CUTOFF_SIZE_POSITION, 4);
        System.arraycopy(Utils.ENDOFCHAIN_MARK, 0, DUMMY_HEADER, FLAG_POSITION.FIRST_DIFAT_SECTOR, 4);
        Utils.initializedWith(436, (byte)-1);
    }

    public static byte[] dummyHeader() {
        byte[] result = new byte[HEADER_LENGTH];
        System.arraycopy(DUMMY_HEADER, 0, result, 0, HEADER_LENGTH);
        return result;
    }

    byte[] data;

    @BeforeEach
    void init() {
        data = new byte[HEADER_LENGTH];
        System.arraycopy(Header.HEADER_SIGNATURE, 0, data, FLAG_POSITION.SIGNATURE, 8);
        System.arraycopy(Header.MINOR_VERSION_3, 0, data, FLAG_POSITION.MINOR_VERSION, 2);
        System.arraycopy(Header.MAJOR_VERSION_3, 0, data, FLAG_POSITION.MAJOR_VERSION, 2);
        System.arraycopy(Header.BYTE_ORDER_LITTLE_ENDIAN, 0, data, FLAG_POSITION.BYTE_ORDER, 2);
        System.arraycopy(Header.SECTOR_SHIFT_VERSION_3, 0, data, FLAG_POSITION.SECTOR_SHIFT, 2);
        System.arraycopy(Header.MINI_SECTOR_SHIFT_VERSION_3, 0, data, FLAG_POSITION.MINI_SECTOR_SHIFT, 2);
        System.arraycopy(Header.MINI_STREAM_CUTOFF_SIZE, 0, data, FLAG_POSITION.MINI_STREAM_CUTOFF_SIZE_POSITION, 4);
        System.arraycopy(Utils.initializedWith(436, -1), 0, data, FLAG_POSITION.DIFAT_ENTRIES_FIRST_POSITION, 436);
    }

    @Test
    void testEnforceSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> new Header(new DataView.SimpleDataView(new byte[513])));
    }

    @Test
    void testSupportsOnlyMajorVersion3() {
        System.arraycopy(Utils.toBytesLE(0x0004, 2), 0, data, FLAG_POSITION.MAJOR_VERSION, 2);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testSupportsOnlyLittleEndianByteOrder() {
        System.arraycopy(Utils.toBytesLE(0xfffd, 2), 0, data, FLAG_POSITION.BYTE_ORDER, 2);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testSupportsOnly512BytesSectorShift() {
        System.arraycopy(Utils.toBytesLE(0x0090, 2), 0, data, FLAG_POSITION.SECTOR_SHIFT, 2);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testSupportsOnly64BytesMiniSectorShift() {
        System.arraycopy(Utils.toBytesLE(0x0007, 2), 0, data, FLAG_POSITION.MINI_SECTOR_SHIFT, 2);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testEnforcesReservedBytes() {
        System.arraycopy(Utils.toBytesLE(0xddddddddddddl, 6), 0, data, 34, 6);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testEnforcesDirectoryServicesNumber() {
        System.arraycopy(Utils.toBytesLE(0x11111111, 4), 0, data, 40, 4);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testSupportsOnly4096BytesMiniStreamCutoffSize() {
        System.arraycopy(Utils.toBytesLE(0x00002000, 4), 0, data, FLAG_POSITION.MINI_STREAM_CUTOFF_SIZE_POSITION, 4);
        assertThrows(VerifyException.class, () -> new Header(DataView.from(data)));
    }

    @Test
    void testSectorsUtilityInformation() {
        System.arraycopy(Utils.toBytesLE(0x00000001, 4), 0, data, FLAG_POSITION.NUMBER_OF_FAT_SECTORS, 4);
        System.arraycopy(Utils.toBytesLE(0x00000002, 4), 0, data, FLAG_POSITION.FIRST_DIRECTORY_SECTOR, 4);
        System.arraycopy(Utils.toBytesLE(0x00000003, 4), 0, data, FLAG_POSITION.FIRST_MINIFAT_SECTOR, 4);
        System.arraycopy(Utils.toBytesLE(0x00000004, 4), 0, data, FLAG_POSITION.NUMBER_OF_MINIFAT_SECTORS, 4);
        System.arraycopy(Utils.toBytesLE(0x00000005, 4), 0, data, FLAG_POSITION.FIRST_DIFAT_SECTOR, 4);
        System.arraycopy(Utils.toBytesLE(0x00000006, 4), 0, data, FLAG_POSITION.NUMBER_OF_DIFAT_SECTORS, 4);
        Header header = new Header(new DataView.SimpleDataView(data));
        assertEquals(1, header.getNumberOfFatSectors());
        assertEquals(2, header.getFirstDirectorySectorLocation());
        assertEquals(3, header.getFirstMinifatSectorLocation());
        assertEquals(4, header.getNumberOfMiniFatSectors());
        assertEquals(5, header.getFirstDifatSectorLocation());
        assertEquals(6, header.getNumberOfDifatSectors());
        header.setNumberOfFatSectors(6);
        header.setFirstDirectorySectorLocation(5);
        header.setFirstMinifatSectorLocation(4);
        header.setNumberOfMiniFatSectors(3);
        header.setFirstDifatSectorLocation(2);
        header.setNumberOfDifatSectors(1);
        assertEquals(6, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.NUMBER_OF_FAT_SECTORS, FLAG_POSITION.NUMBER_OF_FAT_SECTORS + 4)));
        assertEquals(5, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.FIRST_DIRECTORY_SECTOR, FLAG_POSITION.FIRST_DIRECTORY_SECTOR + 4)));
        assertEquals(4, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.FIRST_MINIFAT_SECTOR, FLAG_POSITION.FIRST_MINIFAT_SECTOR + 4)));
        assertEquals(3, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.NUMBER_OF_MINIFAT_SECTORS, FLAG_POSITION.NUMBER_OF_MINIFAT_SECTORS + 4)));
        assertEquals(2, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.FIRST_DIFAT_SECTOR, FLAG_POSITION.FIRST_DIFAT_SECTOR +4)));
        assertEquals(1, Utils.toInt(ArrayUtils.subarray(data, FLAG_POSITION.NUMBER_OF_DIFAT_SECTORS, FLAG_POSITION.NUMBER_OF_DIFAT_SECTORS + 4)));

        //sector shifts
        assertEquals(Math.pow(2, Utils.toInt(SECTOR_SHIFT_VERSION_3)), header.getSectorShift());
        assertEquals(Math.pow(2, Utils.toInt(MINI_SECTOR_SHIFT_VERSION_3)), header.getMiniSectorShift());
        assertEquals(Utils.toInt(MINI_STREAM_CUTOFF_SIZE), header.getMiniStreamCutoffSize());
    }

    @Test
    void testGetHeaderDifatEntries() {
        System.arraycopy(Utils.toBytesLE(0, 4), 0, data, 76, 4);
        System.arraycopy(Utils.toBytesLE(1, 4), 0, data, 80, 4);
        System.arraycopy(Utils.toBytesLE(2, 4), 0, data, 84, 4);
        List<Integer> difatEntries = new Header(DataView.from(data).subView(0, HEADER_LENGTH)).getDifatEntries();
        assertTrue(Iterables.elementsEqual(Lists.newArrayList(0,1,2), difatEntries));
    }

    @Test
    void testRegisterFatSector() {
        Header header = new Header(new DataView.SimpleDataView(data));
        header.registerFatSector(100);
        assertEquals(1, header.getDifatEntries().size());
        assertEquals(100, header.getDifatEntries().get(0));
        assertEquals(100, Utils.toInt(ArrayUtils.subarray(data, DIFAT_ENTRIES_FIRST_POSITION, DIFAT_ENTRIES_FIRST_POSITION + 4)));
    }

    @Test
    void testRegisterFatSectorOutOfRange() {
        for (int i = DIFAT_ENTRIES_FIRST_POSITION; i < HEADER_LENGTH; i+=4) {
            System.arraycopy(Utils.toBytesLE(i, 4), 0, data, i, 4);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> new Header(new DataView.SimpleDataView(data)).registerFatSector(1));
    }

    @Test
    void testNewHeader() {
        DataView dataView = new DataView.SimpleDataView(new byte[512]);
        Header header = Header.empty(dataView);
        Header headerNotEmpty = new Header(dataView);
        assertArrayEquals(HEADER_SIGNATURE, dataView.subView(FLAG_POSITION.SIGNATURE, FLAG_POSITION.SIGNATURE + 8).getData());
        assertArrayEquals(new byte[16], dataView.subView(FLAG_POSITION.CLSID, FLAG_POSITION.CLSID + 16).getData());
        assertArrayEquals(Utils.ENDOFCHAIN_MARK, dataView.subView(FLAG_POSITION.FIRST_DIFAT_SECTOR, FLAG_POSITION.FIRST_DIFAT_SECTOR + 4).getData());
        assertArrayEquals(Utils.toBytesLE(0, 4), dataView.subView(FLAG_POSITION.NUMBER_OF_FAT_SECTORS, FLAG_POSITION.NUMBER_OF_FAT_SECTORS + 4).getData());
        assertArrayEquals(Utils.ENDOFCHAIN_MARK, dataView.subView(FLAG_POSITION.FIRST_MINIFAT_SECTOR, FLAG_POSITION.FIRST_MINIFAT_SECTOR + 4).getData());
        assertArrayEquals(Utils.toBytesLE(0, 4), dataView.subView(FLAG_POSITION.NUMBER_OF_MINIFAT_SECTORS, FLAG_POSITION.NUMBER_OF_MINIFAT_SECTORS + 4).getData());
        assertArrayEquals(Utils.toBytesLE(0, 4), dataView.subView(FLAG_POSITION.NUMBER_OF_DIFAT_SECTORS, FLAG_POSITION.NUMBER_OF_DIFAT_SECTORS + 4).getData());
        assertArrayEquals(Utils.ENDOFCHAIN_MARK, dataView.subView(FLAG_POSITION.FIRST_DIRECTORY_SECTOR, FLAG_POSITION.FIRST_DIRECTORY_SECTOR + 4).getData());
    }
}