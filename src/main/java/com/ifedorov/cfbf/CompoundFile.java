package com.ifedorov.cfbf;

import com.ifedorov.cfbf.alloc.DIFAT;
import com.ifedorov.cfbf.alloc.FAT;
import com.ifedorov.cfbf.alloc.FATtoDIFATFacade;
import com.ifedorov.cfbf.alloc.MiniFAT;
import com.ifedorov.cfbf.stream.StreamHolder;
import com.ifedorov.cfbf.stream.MiniStreamRW;
import com.ifedorov.cfbf.stream.RegularStreamRW;
import com.ifedorov.cfbf.stream.StreamRW;

import java.io.*;
import java.util.function.Consumer;

public class CompoundFile {

    private final Header header;
    private final Sectors sectors;
    private final DirectoryEntryChain directoryEntryChain;
    private DataView dataView;

    public CompoundFile() {
       this(empty());
        directoryEntryChain.createRootStorage();
    }

    public CompoundFile(DataView dataView) {
        this.dataView = dataView;
        this.header = new Header(dataView.subView(0, Header.HEADER_LENGTH));
        this.sectors = new Sectors(dataView, header);
        FATtoDIFATFacade faTtoDIFATFacade = new FATtoDIFATFacade();
        DIFAT difat = new DIFAT(sectors, header, faTtoDIFATFacade);
        faTtoDIFATFacade.setDifat(difat);
        FAT fat = new FAT(sectors, header, faTtoDIFATFacade);
        faTtoDIFATFacade.setFat(fat);
        MiniFAT miniFat = new MiniFAT(sectors, header, fat);
        MiniStreamRW miniStreamRW = new MiniStreamRW(miniFat, fat, getMiniStreamFirstSectorLocation(), getMiniStreamLength(), sectors, header);
        StreamRW listenableMiniStream = new StreamRW() {
            @Override
            public byte[] read(int startingSector, int length) {
                return miniStreamRW.read(startingSector, length);
            }

            @Override
            public byte[] read(int startingSector, int from, int to) {
                return miniStreamRW.read(startingSector, from, to);
            }

            @Override
            public int write(byte[] data) {
                int firstSectorLocation = miniStreamRW.write(data);
                setMiniStreamFirstSectorLocation(miniStreamRW.getMiniStreamFirstSectorPosition());
                setMiniStreamLength(miniStreamRW.getMiniStreamLength());
                return firstSectorLocation;
            }

            @Override
            public void writeAt(int startingSector, int position, byte[] data) {
                miniStreamRW.writeAt(startingSector, position, data);
            }

            @Override
            public int append(int startingSector, int currentSize, byte[] data) {
                int firstSectorLocation = miniStreamRW.append(startingSector, currentSize, data);
                setMiniStreamFirstSectorLocation(miniStreamRW.getMiniStreamFirstSectorPosition());
                setMiniStreamLength(miniStreamRW.getMiniStreamLength());
                return firstSectorLocation;
            }

            @Override
            public void copyTo(int startingLocation, OutputStream os) {
                miniStreamRW.copyTo(startingLocation, os);
            }
        };
        StreamHolder streamReader = new StreamHolder(
                new RegularStreamRW(fat, sectors, header),
                listenableMiniStream,
                header.getMiniStreamCutoffSize()
        );

        directoryEntryChain = new DirectoryEntryChain(sectors, fat, header, streamReader);
    }

    private static DataView empty() {
        DataView dataView = DataView.empty();
        Header header = Header.empty(dataView.allocate(Header.HEADER_LENGTH));
        return dataView;
    }

    private int getMiniStreamFirstSectorLocation() {
        if(Utils.ENDOFCHAIN_MARK_INT == header.getFirstDirectorySectorLocation()) {
            return Utils.ENDOFCHAIN_MARK_INT;
        } else {
            return Utils.toInt(sectors.sector(header.getFirstDirectorySectorLocation()).subView(DirectoryEntry.FLAG_POSITION.STARTING_SECTOR_LOCATION, DirectoryEntry.FLAG_POSITION.STARTING_SECTOR_LOCATION + 4).getData());
        }
    }

    private int getMiniStreamLength() {
        if(Utils.ENDOFCHAIN_MARK_INT == header.getFirstDirectorySectorLocation()) {
            return 0;
        } else {
            return Utils.toInt(sectors.sector(header.getFirstDirectorySectorLocation()).subView(DirectoryEntry.FLAG_POSITION.STREAM_SIZE, DirectoryEntry.FLAG_POSITION.STREAM_SIZE + 4).getData());
        }
    }

    private void setMiniStreamFirstSectorLocation(int position) {
        sectors.sector(header.getFirstDirectorySectorLocation())
                .subView(DirectoryEntry.FLAG_POSITION.STARTING_SECTOR_LOCATION, DirectoryEntry.FLAG_POSITION.STARTING_SECTOR_LOCATION + 4)
                .writeAt(0, position >= 0 ? Utils.toBytesLE(position, 4) : Utils.ENDOFCHAIN_MARK);
    }

    private void setMiniStreamLength(int size) {
        sectors.sector(header.getFirstDirectorySectorLocation())
                .subView(DirectoryEntry.FLAG_POSITION.STREAM_SIZE, DirectoryEntry.FLAG_POSITION.STREAM_SIZE + 4)
                .writeAt(0, Utils.toBytesLE(size, 4));
    }

    public RootStorageDirectoryEntry getRootStorage() {
        return directoryEntryChain.getRootStorage();
    }


    public void saveTo(File outputFile) {
        try(FileOutputStream os = new FileOutputStream(outputFile)) {
            os.write(dataView.getData());
        } catch (IOException e) {
            throw new RuntimeException("Unable to save msg to file: ",e );
        }
    }

    public void saveTo(OutputStream outputStream) {
        try {
            outputStream.write(dataView.getData());
        } catch (IOException e) {
            throw new RuntimeException("Unable to save msg to file: ",e );
        }
    }

    public CompoundFile copy() {
        CompoundFile copy = new CompoundFile();
        RootStorageDirectoryEntry rootStorage = getRootStorage();
        RootStorageDirectoryEntry rootStorageCopy = copy.getRootStorage();
        rootStorage.eachChild(copyConsumer(rootStorageCopy));
        return copy;
    }

    private Consumer<DirectoryEntry> copyConsumer(StorageDirectoryEntry parent) {
        return new Consumer<DirectoryEntry>() {
            @Override
            public void accept(DirectoryEntry directoryEntry) {
                if(directoryEntry instanceof StorageDirectoryEntry) {
                    StorageDirectoryEntry copy = parent.addStorage(directoryEntry.getDirectoryEntryName());
                    ((StorageDirectoryEntry) directoryEntry).eachChild(copyConsumer(copy));
                } else {
                    parent.addStream(directoryEntry.getDirectoryEntryName(), ((StreamDirectoryEntry)directoryEntry).getStreamData());
                }
            }
        };
    }

}
