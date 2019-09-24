package com.ifedorov.cfbf.stream;

import com.google.common.collect.Lists;
import com.ifedorov.cfbf.*;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegularStreamRWTest {

    @Mock FAT fat;
    @Mock CompoundFile compoundFile;

    @Test
    void testRead() {

        when(fat.buildChain(1)).thenReturn(Lists.newArrayList(1,2,3));
        when(compoundFile.sector(1))
                .thenReturn(Sector.from(DataView.from(Utils.initializedWith(512, 1)), 1));
        when(compoundFile.sector(2))
                .thenReturn(Sector.from(DataView.from(Utils.initializedWith(512, 2)), 2));
        when(compoundFile.sector(3))
                .thenReturn(Sector.from(DataView.from(Utils.initializedWith(512, 3)), 3));
        RegularStreamRW regularStreamRW = new RegularStreamRW(fat, compoundFile);
        byte[] result = regularStreamRW.read(1, 1300);
        assertEquals(1300, result.length);
        assertArrayEquals(Utils.initializedWith(512, 1), ArrayUtils.subarray(result, 0, 512));
        assertArrayEquals(Utils.initializedWith(512, 2), ArrayUtils.subarray(result, 512, 1024));
        assertArrayEquals(Utils.initializedWith(276, 3), ArrayUtils.subarray(result, 1024, 1300));
        verify(fat, times(1)).buildChain(1);
        verify(compoundFile, times(1)).sector(1);
        verify(compoundFile, times(1)).sector(2);
        verify(compoundFile, times(1)).sector(3);
    }
}