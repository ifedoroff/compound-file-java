package com.ifedorov.cfbf;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;

import java.util.List;

public class AllocationTable {

    private final List<Sector> sectors;
    private final int sectorSize;

    public AllocationTable(List<Sector> allocationSectors, int sectorSize) {
        this.sectors = allocationSectors;
        this.sectorSize = sectorSize;
    }

    public List<Integer> buildChain(int currentSector) {
        List<Integer> chain = Lists.newArrayList();
        chain.add(currentSector);
        while(!Utils.isEndOfChain(currentSector = getValueAt(currentSector))) {
            chain.add(currentSector);
        }
        return chain;
    }

    private int getValueAt(int position) {
        int sectorNumber = position * 4 / sectorSize;
        int shiftInsideSector = position * 4 % sectorSize;
        Verify.verify(sectorNumber <= sectors.size());
        return Utils.toInt(sectors.get(sectorNumber).subView(shiftInsideSector, shiftInsideSector + 4).getData());
    }
}