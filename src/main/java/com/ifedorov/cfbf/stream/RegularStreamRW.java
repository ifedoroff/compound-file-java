package com.ifedorov.cfbf.stream;

import com.ifedorov.cfbf.Header;
import com.ifedorov.cfbf.Sectors;
import com.ifedorov.cfbf.alloc.FAT;
import com.ifedorov.cfbf.Sector;
import org.apache.commons.lang3.ArrayUtils;

public class RegularStreamRW implements StreamRW {

    private final FAT fat;
    private final Sectors sectors;
    private Header header;

    public RegularStreamRW(FAT fat, Sectors sectors, Header header) {
        this.fat = fat;
        this.sectors = sectors;
        this.header = header;
    }

    @Override
    public byte[] read(int startingSector, int length) {
        byte[] result = new byte[length];
        int positionInResult = 0;
        for (Integer sectorPosition : fat.buildChain(startingSector)) {
            if(length > 0) {
                Sector sector = sectors.sector(sectorPosition);
                int bytesToRead = Math.min(sector.getSize(), length);
                System.arraycopy(sector.subView(0, bytesToRead).getData(), 0, result, positionInResult, bytesToRead);
                positionInResult += bytesToRead;
                length -= bytesToRead;
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public int write(byte[] data) {
        Integer firstSectorPosition = null;
        Integer previousSectorPosition = null;
        for (int i = 0; i < data.length; i+=header.getSectorShift()) {
            Sector sector = sectors.allocate();
            int writeBytes = Math.min(header.getSectorShift(), data.length - i);
            sector.writeAt(0, ArrayUtils.subarray(data, i, i + writeBytes));
            int sectorPosition = sector.getPosition();
            fat.registerSector(sectorPosition, previousSectorPosition);
            if(firstSectorPosition == null) {
                firstSectorPosition = sectorPosition;
            }
            previousSectorPosition = sectorPosition;
        }
        return firstSectorPosition;
    }
}
