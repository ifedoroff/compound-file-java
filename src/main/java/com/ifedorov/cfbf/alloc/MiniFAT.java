package com.ifedorov.cfbf.alloc;

import com.ifedorov.cfbf.Header;
import com.ifedorov.cfbf.Sector;
import com.ifedorov.cfbf.Sectors;

public class MiniFAT extends AllocationTable {

    private final Header header;
    private final FAT fat;

    public MiniFAT(Sectors sectors, Header header, FAT fat) {
        super(sectors, fat.buildChain(header.getFirstMinifatSectorLocation()), header.getSectorShift());
        this.header = header;
        this.fat = fat;
    }

    @Override
    protected Sector allocateNewSector() {
        Sector newSector = super.allocateNewSector();
        Integer previousSectorPosition = sectorChain.size() == 1 ? null : sectorChain.get(sectorChain.size() - 2);
        fat.registerSector(newSector.getPosition(), previousSectorPosition);
        header.setNumberOfMiniFatSectors(sectorChain.size());
        if(sectorChain.size() == 1) {
            header.setFirstMinifatSectorLocation(sectorChain.get(0));
        }
        return newSector;
    }
}
