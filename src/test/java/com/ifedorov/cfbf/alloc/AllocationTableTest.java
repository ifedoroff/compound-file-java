package com.ifedorov.cfbf.alloc;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.ifedorov.cfbf.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ifedorov.cfbf.Header.HEADER_LENGTH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationTableTest {

    @Mock Sectors sectors;
    @Mock
    Header header;

    @BeforeEach
    void init() {
        lenient().when(header.getSectorShift()).thenReturn(HEADER_LENGTH);
    }

    @Test
    void testBuildChain() {
        byte[] firstSector = new byte[16];
        System.arraycopy(Utils.toBytesLE(1, 4), 0, firstSector, 0, 4);
        System.arraycopy(Utils.toBytesLE(2, 4), 0, firstSector, 4, 4);
        System.arraycopy(Utils.toBytesLE(5, 4), 0, firstSector, 8, 4);
        System.arraycopy(Utils.toBytesLE(4, 4), 0, firstSector, 12, 4);
        byte[] secondSector = new byte[16];
        System.arraycopy(Utils.toBytesLE(6, 4), 0, secondSector, 0, 4);
        System.arraycopy(Utils.ENDOFCHAIN_MARK, 0, secondSector, 4, 4);
        System.arraycopy(Utils.ENDOFCHAIN_MARK, 0, secondSector, 8, 4);
        System.arraycopy(Utils.ENDOFCHAIN_MARK, 0, secondSector, 12, 4);
        when(sectors.sector(0)).thenReturn(Sector.from(new DataView.SimpleDataView(firstSector), 0));
        when(sectors.sector(1)).thenReturn(Sector.from(new DataView.SimpleDataView(secondSector), 1));
        AllocationTable allocationTable = new AllocationTable(
                sectors,
                Lists.newArrayList(0, 1),
                16);
        assertEquals(4, allocationTable.buildChain(0).size());
        assertTrue(Iterables.elementsEqual(Lists.newArrayList(0,1,2,5), allocationTable.buildChain(0)));
        assertEquals(3, allocationTable.buildChain(3).size());
        assertTrue(Iterables.elementsEqual(Lists.newArrayList(3,4,6), allocationTable.buildChain(3)));
    }

    @Test
    void testRegisterSectorsContinuouslyAfterAllocationOfEachNewSector() {
        Sectors sectors = new Sectors(DataView.empty(), header);
        AllocationTable allocationTable = new AllocationTable(sectors, Lists.newArrayList(), header.getSectorShift());
        Integer previousSectorPosition = null;
        Integer firstSectorPosition = null;
        for (int i = 0; i < 129; i++) {
            int sectorPosition = sectors.allocate().getPosition();
            if(firstSectorPosition == null) {
                firstSectorPosition = sectorPosition;
            }
            allocationTable.registerSector(sectorPosition, previousSectorPosition);
            previousSectorPosition = sectorPosition;
        }
//        verify(fatToDIFATFacade, times(2)).registerFatSectorInDIFAT(anyInt());
        List<Integer> chain = allocationTable.buildChain(firstSectorPosition);
        assertEquals(129, chain.size());
    }

    @Test
    void testShouldReturnEmptyListIfFirstSectorPositionIsEndOfChain() {
        Sectors sectors = new Sectors(DataView.empty(), header);
        AllocationTable allocationTable = new AllocationTable(sectors, Lists.newArrayList(), header.getSectorShift());
        assertEquals(0, allocationTable.buildChain(Utils.ENDOFCHAIN_MARK_INT).size());
    }
}