package com.ifedorov.cfbf;

import com.google.common.base.Verify;
import com.ifedorov.cfbf.stream.StreamReader;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class DirectoryEntry {

    public static final int ENTRY_LENGTH = 128;
    private DataView view;
    private ObjectType objectType;
    private ColorFlag colorFlag;
    private CompoundFile compoundFile;
    private DirectoryEntryChain directoryEntryChain;
    private final StreamReader streamReader;

    public interface FLAG_POSITION {
        int DIRECTORY_ENTRY_NAME = 0;
        int DIRECTORY_ENTRY_NAME_LENGTH = 64;
        int OBJECT_TYPE = 66;
        int COLOR_FLAG = 67;
        int LEFT_SIBLING = 68;
        int RIGHT_SIBLING = 72;
        int CHILD = 76;
        int CLSID = 80;
        int STATE_BITS = 96;
        int CREATION_TIME = 100;
        int MODIFY_TIME = 108;
        int STARTING_SECTOR_LOCATION = 116;
        int STREAM_SIZE = 120;
    }

    public DirectoryEntry(DirectoryEntryChain directoryEntryChain, DataView view, StreamReader streamReader) {
        this.directoryEntryChain = directoryEntryChain;
        this.streamReader = streamReader;
        Verify.verify(view.getSize() == ENTRY_LENGTH);
        int nameLength = Utils.toInt(view.subView(FLAG_POSITION.DIRECTORY_ENTRY_NAME_LENGTH, FLAG_POSITION.DIRECTORY_ENTRY_NAME_LENGTH + 2).getData());
        Verify.verify(nameLength >= 0 && nameLength <= 64);
        objectType = ObjectType.fromCode(view.subView(FLAG_POSITION.OBJECT_TYPE, FLAG_POSITION.OBJECT_TYPE +1).getData()[0]);
        colorFlag = ColorFlag.fromCode(view.subView(FLAG_POSITION.COLOR_FLAG, FLAG_POSITION.COLOR_FLAG +1).getData()[0]);
        this.view = view;
    }

    public String getDirectoryEntryName() {
        return Utils.toUTF8WithNoTrailingZeros(view.subView(FLAG_POSITION.DIRECTORY_ENTRY_NAME, FLAG_POSITION.DIRECTORY_ENTRY_NAME + 64).getData());
    }

    public int getDirectoryEntryNameLength() {
        return Utils.toInt(view.subView(DirectoryEntry.FLAG_POSITION.DIRECTORY_ENTRY_NAME_LENGTH, DirectoryEntry.FLAG_POSITION.DIRECTORY_ENTRY_NAME_LENGTH + 2).getData());
    }

    public Optional<DirectoryEntry> getChild() {
        int childPosition = getChildPosition();
        return Utils.isFreeSectOrNoStream(childPosition) ? Optional.empty() : Optional.ofNullable(directoryEntryChain.getEntryById(childPosition));
    }

    private int getChildPosition() {
        return Utils.toInt(view.subView(FLAG_POSITION.CHILD, FLAG_POSITION.CHILD + 4).getData());
    }

    public Optional<DirectoryEntry> getLeftSibling() {
        int leftSiblingPosition = getLeftSiblingPosition();
        return Utils.isFreeSectOrNoStream(leftSiblingPosition) ? Optional.empty() : Optional.of(directoryEntryChain.getEntryById(leftSiblingPosition));
    }

    private int getLeftSiblingPosition() {
        return Utils.toInt(view.subView(FLAG_POSITION.LEFT_SIBLING, FLAG_POSITION.LEFT_SIBLING + 4).getData());
    }

    public Optional<DirectoryEntry> getRightSibling() {
        int rightSiblingPosition = getRightSiblingPosition();
        return Utils.isFreeSectOrNoStream(rightSiblingPosition) ? Optional.empty() : Optional.ofNullable(directoryEntryChain.getEntryById(rightSiblingPosition));
    }

    private int getRightSiblingPosition() {
        return Utils.toInt(view.subView(FLAG_POSITION.RIGHT_SIBLING, FLAG_POSITION.RIGHT_SIBLING + 4).getData());
    }

    public byte[] getStreamData() {
        if(hasStreamData()) {
            return streamReader.read(getStreamStartingSector(), getStreamSize());
        } else {
            throw new UnsupportedOperationException("Stream is not supported for object of type: " + objectType);
        }
    }

    public boolean hasStreamData() {
        return objectType == ObjectType.Stream && !Utils.isEndOfChain(getStreamStartingSector());
    }

    public int getStreamStartingSector() {
        return Utils.toInt(view.subView(FLAG_POSITION.STARTING_SECTOR_LOCATION, FLAG_POSITION.STARTING_SECTOR_LOCATION + 4).getData());
    }

    public int getStreamSize() {
        return Utils.toInt(view.subView(FLAG_POSITION.STREAM_SIZE, FLAG_POSITION.STREAM_SIZE + 4).getData());
    }

    public void traverse(Consumer<DirectoryEntry> action) {
        action.accept(this);
        getLeftSibling().ifPresent((leftSibling) -> leftSibling.traverse(action));
        getRightSibling().ifPresent((rightSibling) -> rightSibling.traverse(action));
        getChild().ifPresent(child -> child.traverse(action));
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public ColorFlag getColorFlag() {
        return colorFlag;
    }

    public enum ColorFlag {
        RED(0), BLACK(1);

        private int code;

        private ColorFlag(int code) {

            this.code = code;
        }

        public static ColorFlag fromCode(int code) {
            for (ColorFlag value : ColorFlag.values()) {
                if(value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown ColorFlag: " + code);
        }

        public int code() {
            return code;
        }

    }

    public enum ObjectType {
        Storage(1), Stream(2), RootStorage(5), Unknown(0);

        private int code;

        private ObjectType(int code) {

            this.code = code;
        }

        public static ObjectType fromCode(int code) {
            for (ObjectType value : ObjectType.values()) {
                if(value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown ObjectType: " + code);
        }

        public int code() {
            return code;
        }
    }
}