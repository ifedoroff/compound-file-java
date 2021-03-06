package com.ifedorov.cfbf;

import java.io.OutputStream;

public interface Sector extends DataView{
    int getPosition();
    static Sector from(DataView view, int position) {
        return new SimpleSector(view, position);
    }

    static Sector from(DataView view, int position, byte[] filler) {
        return new SimpleSector(view, position).fill(filler);
    }

    class SimpleSector implements Sector {
        private final DataView view;
        private final int position;

        private SimpleSector(DataView view, int position) {
            this.view = view;
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        @Override
        public Sector writeAt(int position, byte[] bytes) {
            view.writeAt(position, bytes);
            return this;
        }

        @Override
        public int getSize() {
            return view.getSize();
        }

        @Override
        public byte[] getData() {
            return view.getData();
        }

        @Override
        public DataView subView(int start, int end) {
            return view.subView(start, end);
        }

        @Override
        public DataView subView(int start) {
            return view.subView(start);
        }

        @Override
        public DataView allocate(int length) {
            return view.allocate(length);
        }

        @Override
        public Sector fill(byte[] filler) {
            view.fill(filler);
            return this;
        }

        @Override
        public byte[] readAt(int position, int length) {
            return view.readAt(position, length);
        }

        @Override
        public void copyTo(OutputStream os) {
            view.copyTo(os);
        }

        public static DataView empty() {
            return DataView.empty();
        }

        public static DataView from(byte[] data) {
            return DataView.from(data);
        }
    }
}
