package com.ifedorov.cfbf;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class DataViewTest {

    @Test
    void testOverflow() {
        assertThrows(IndexOutOfBoundsException.class, () -> DataView.empty().writeAt(0, new byte[1]));
        assertThrows(IndexOutOfBoundsException.class, () -> DataView.empty().allocate(10).writeAt(5, new byte[6]));
    }

    @Test
    void testSize() {
        DataView dataView = DataView.empty();
        assertEquals(100, dataView.allocate(100).getSize());
        assertEquals(100, dataView.allocate(100).getSize());
        assertEquals(200, dataView.getSize());
    }

    @Test
    void testSubView() {
        DataView dataView = DataView.from(new byte[100]);
        byte[] firstPart = new byte[50];
        Arrays.fill(firstPart, (byte)1);
        dataView.writeAt(0, firstPart);
        byte[] secondPart = new byte[50];
        Arrays.fill(secondPart, (byte)2);
        dataView.writeAt(50, secondPart);
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(0, 101));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(0, 50).subView(1, 51));
        assertArrayEquals(firstPart, dataView.subView(0, 50).getData());
        assertArrayEquals(secondPart, dataView.subView(50, 100).getData());
        assertThrows(UnsupportedOperationException.class, () -> dataView.subView(0, 10).allocate(10));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(101));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(0, 10).subView(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataView.subView(0, 10).subView(11));
        assertEquals(10, dataView.subView(50, 100).subView(0, 10).getSize());
    }

}