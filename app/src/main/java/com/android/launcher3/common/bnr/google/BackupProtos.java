package com.android.launcher3.common.bnr.google;

import android.support.v4.view.MotionEventCompat;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;
import java.util.Arrays;

public interface BackupProtos {

    public static final class CheckedMessage extends MessageNano {
        private static volatile CheckedMessage[] _emptyArray;
        public long checksum;
        public byte[] payload;

        public static CheckedMessage[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CheckedMessage[0];
                    }
                }
            }
            return _emptyArray;
        }

        public CheckedMessage() {
            clear();
        }

        public CheckedMessage clear() {
            this.payload = WireFormatNano.EMPTY_BYTES;
            this.checksum = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeBytes(1, this.payload);
            output.writeInt64(2, this.checksum);
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            return (super.computeSerializedSize() + CodedOutputByteBufferNano.computeBytesSize(1, this.payload)) + CodedOutputByteBufferNano.computeInt64Size(2, this.checksum);
        }

        public CheckedMessage mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 10:
                        this.payload = input.readBytes();
                        continue;
                    case 16:
                        this.checksum = input.readInt64();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static CheckedMessage parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (CheckedMessage) MessageNano.mergeFrom(new CheckedMessage(), data);
        }

        public static CheckedMessage parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new CheckedMessage().mergeFrom(input);
        }
    }

    public static final class DeviceProfileData extends MessageNano {
        private static volatile DeviceProfileData[] _emptyArray;
        public int cols;
        public int colsHomeOnly;
        public int homeIndex;
        public int homeIndexHomeOnly;
        public int rows;
        public int rowsHomeOnly;

        public static DeviceProfileData[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DeviceProfileData[0];
                    }
                }
            }
            return _emptyArray;
        }

        public DeviceProfileData() {
            clear();
        }

        public DeviceProfileData clear() {
            this.rows = 0;
            this.cols = 0;
            this.homeIndex = 0;
            this.rowsHomeOnly = 0;
            this.colsHomeOnly = 0;
            this.homeIndexHomeOnly = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt32(1, this.rows);
            output.writeInt32(2, this.cols);
            output.writeInt32(3, this.homeIndex);
            output.writeInt32(4, this.rowsHomeOnly);
            output.writeInt32(5, this.colsHomeOnly);
            output.writeInt32(6, this.homeIndexHomeOnly);
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            return (((((super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.rows)) + CodedOutputByteBufferNano.computeInt32Size(2, this.cols)) + CodedOutputByteBufferNano.computeInt32Size(3, this.homeIndex)) + CodedOutputByteBufferNano.computeInt32Size(4, this.rowsHomeOnly)) + CodedOutputByteBufferNano.computeInt32Size(5, this.colsHomeOnly)) + CodedOutputByteBufferNano.computeInt32Size(6, this.homeIndexHomeOnly);
        }

        public DeviceProfileData mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        this.rows = input.readInt32();
                        continue;
                    case 16:
                        this.cols = input.readInt32();
                        continue;
                    case 24:
                        this.homeIndex = input.readInt32();
                        continue;
                    case 32:
                        this.rowsHomeOnly = input.readInt32();
                        continue;
                    case MotionEventCompat.AXIS_GENERIC_9 /*40*/:
                        this.colsHomeOnly = input.readInt32();
                        continue;
                    case 48:
                        this.homeIndexHomeOnly = input.readInt32();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static DeviceProfileData parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (DeviceProfileData) MessageNano.mergeFrom(new DeviceProfileData(), data);
        }

        public static DeviceProfileData parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new DeviceProfileData().mergeFrom(input);
        }
    }

    public static final class Favorite extends MessageNano {
        private static volatile Favorite[] _emptyArray;
        public int appWidgetId;
        public String appWidgetProvider;
        public int cellX;
        public int cellY;
        public int container;
        public byte[] icon;
        public String iconPackage;
        public String iconResource;
        public int iconType;
        public long id;
        public String intent;
        public int itemType;
        public int rank;
        public int screen;
        public int spanX;
        public int spanY;
        public String title;

        public static Favorite[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Favorite[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Favorite() {
            clear();
        }

        public Favorite clear() {
            this.id = 0;
            this.itemType = 0;
            this.title = "";
            this.container = 0;
            this.screen = 0;
            this.cellX = 0;
            this.cellY = 0;
            this.spanX = 0;
            this.spanY = 0;
            this.appWidgetId = 0;
            this.appWidgetProvider = "";
            this.intent = "";
            this.iconType = 0;
            this.iconPackage = "";
            this.iconResource = "";
            this.icon = WireFormatNano.EMPTY_BYTES;
            this.rank = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt64(1, this.id);
            output.writeInt32(2, this.itemType);
            if (!this.title.equals("")) {
                output.writeString(3, this.title);
            }
            if (this.container != 0) {
                output.writeInt32(4, this.container);
            }
            if (this.screen != 0) {
                output.writeInt32(5, this.screen);
            }
            if (this.cellX != 0) {
                output.writeInt32(6, this.cellX);
            }
            if (this.cellY != 0) {
                output.writeInt32(7, this.cellY);
            }
            if (this.spanX != 0) {
                output.writeInt32(8, this.spanX);
            }
            if (this.spanY != 0) {
                output.writeInt32(9, this.spanY);
            }
            if (this.appWidgetId != 0) {
                output.writeInt32(10, this.appWidgetId);
            }
            if (!this.appWidgetProvider.equals("")) {
                output.writeString(11, this.appWidgetProvider);
            }
            if (!this.intent.equals("")) {
                output.writeString(12, this.intent);
            }
            if (this.iconType != 0) {
                output.writeInt32(13, this.iconType);
            }
            if (!this.iconPackage.equals("")) {
                output.writeString(14, this.iconPackage);
            }
            if (!this.iconResource.equals("")) {
                output.writeString(15, this.iconResource);
            }
            if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
                output.writeBytes(16, this.icon);
            }
            if (this.rank != 0) {
                output.writeInt32(17, this.rank);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = (super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt64Size(1, this.id)) + CodedOutputByteBufferNano.computeInt32Size(2, this.itemType);
            if (!this.title.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(3, this.title);
            }
            if (this.container != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.container);
            }
            if (this.screen != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.screen);
            }
            if (this.cellX != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.cellX);
            }
            if (this.cellY != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(7, this.cellY);
            }
            if (this.spanX != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.spanX);
            }
            if (this.spanY != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(9, this.spanY);
            }
            if (this.appWidgetId != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(10, this.appWidgetId);
            }
            if (!this.appWidgetProvider.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(11, this.appWidgetProvider);
            }
            if (!this.intent.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(12, this.intent);
            }
            if (this.iconType != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(13, this.iconType);
            }
            if (!this.iconPackage.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(14, this.iconPackage);
            }
            if (!this.iconResource.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(15, this.iconResource);
            }
            if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
                size += CodedOutputByteBufferNano.computeBytesSize(16, this.icon);
            }
            if (this.rank != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(17, this.rank);
            }
            return size;
        }

        public Favorite mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        this.id = input.readInt64();
                        continue;
                    case 16:
                        this.itemType = input.readInt32();
                        continue;
                    case MotionEventCompat.AXIS_SCROLL /*26*/:
                        this.title = input.readString();
                        continue;
                    case 32:
                        this.container = input.readInt32();
                        continue;
                    case MotionEventCompat.AXIS_GENERIC_9 /*40*/:
                        this.screen = input.readInt32();
                        continue;
                    case 48:
                        this.cellX = input.readInt32();
                        continue;
                    case 56:
                        this.cellY = input.readInt32();
                        continue;
                    case 64:
                        this.spanX = input.readInt32();
                        continue;
                    case 72:
                        this.spanY = input.readInt32();
                        continue;
                    case 80:
                        this.appWidgetId = input.readInt32();
                        continue;
                    case 90:
                        this.appWidgetProvider = input.readString();
                        continue;
                    case 98:
                        this.intent = input.readString();
                        continue;
                    case 104:
                        this.iconType = input.readInt32();
                        continue;
                    case 114:
                        this.iconPackage = input.readString();
                        continue;
                    case FolderLock.REQUEST_CODE_FOLDER_LOCK /*122*/:
                        this.iconResource = input.readString();
                        continue;
                    case 130:
                        this.icon = input.readBytes();
                        continue;
                    case 136:
                        this.rank = input.readInt32();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Favorite parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Favorite) MessageNano.mergeFrom(new Favorite(), data);
        }

        public static Favorite parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Favorite().mergeFrom(input);
        }
    }

    public static final class Journal extends MessageNano {
        private static volatile Journal[] _emptyArray;
        public int appVersion;
        public int backupVersion;
        public long bytes;
        public Key[] key;
        public DeviceProfileData profile;
        public int rows;
        public long t;

        public static Journal[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Journal[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Journal() {
            clear();
        }

        public Journal clear() {
            this.appVersion = 0;
            this.t = 0;
            this.bytes = 0;
            this.rows = 0;
            this.key = Key.emptyArray();
            this.backupVersion = 1;
            this.profile = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt32(1, this.appVersion);
            output.writeInt64(2, this.t);
            if (this.bytes != 0) {
                output.writeInt64(3, this.bytes);
            }
            if (this.rows != 0) {
                output.writeInt32(4, this.rows);
            }
            if (this.key != null && this.key.length > 0) {
                for (Key element : this.key) {
                    if (element != null) {
                        output.writeMessage(5, element);
                    }
                }
            }
            if (this.backupVersion != 1) {
                output.writeInt32(6, this.backupVersion);
            }
            if (this.profile != null) {
                output.writeMessage(7, this.profile);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = (super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.appVersion)) + CodedOutputByteBufferNano.computeInt64Size(2, this.t);
            if (this.bytes != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(3, this.bytes);
            }
            if (this.rows != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.rows);
            }
            if (this.key != null && this.key.length > 0) {
                for (Key element : this.key) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(5, element);
                    }
                }
            }
            if (this.backupVersion != 1) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.backupVersion);
            }
            if (this.profile != null) {
                return size + CodedOutputByteBufferNano.computeMessageSize(7, this.profile);
            }
            return size;
        }

        public Journal mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        this.appVersion = input.readInt32();
                        continue;
                    case 16:
                        this.t = input.readInt64();
                        continue;
                    case 24:
                        this.bytes = input.readInt64();
                        continue;
                    case 32:
                        this.rows = input.readInt32();
                        continue;
                    case MotionEventCompat.AXIS_GENERIC_11 /*42*/:
                        int i;
                        int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 42);
                        if (this.key == null) {
                            i = 0;
                        } else {
                            i = this.key.length;
                        }
                        Key[] newArray = new Key[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.key, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new Key();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new Key();
                        input.readMessage(newArray[i]);
                        this.key = newArray;
                        continue;
                    case 48:
                        this.backupVersion = input.readInt32();
                        continue;
                    case 58:
                        if (this.profile == null) {
                            this.profile = new DeviceProfileData();
                        }
                        input.readMessage(this.profile);
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Journal parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Journal) MessageNano.mergeFrom(new Journal(), data);
        }

        public static Journal parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Journal().mergeFrom(input);
        }
    }

    public static final class Key extends MessageNano {
        private static volatile Key[] _emptyArray;
        public long checksum;
        public long id;
        public String name;
        public int type;

        public interface Type {
            public static final int FAVORITE = 1;
            public static final int FAVORITE_HOMEONLY = 5;
            public static final int ICON = 3;
            public static final int SCREEN = 2;
            public static final int SCREEN_HOMEONLY = 6;
            public static final int WIDGET = 4;
            public static final int WIDGET_HOMEONLY = 7;
        }

        public static Key[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Key[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Key() {
            clear();
        }

        public Key clear() {
            this.type = 1;
            this.name = "";
            this.id = 0;
            this.checksum = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt32(1, this.type);
            if (!this.name.equals("")) {
                output.writeString(2, this.name);
            }
            if (this.id != 0) {
                output.writeInt64(3, this.id);
            }
            if (this.checksum != 0) {
                output.writeInt64(4, this.checksum);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.type);
            if (!this.name.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(2, this.name);
            }
            if (this.id != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(3, this.id);
            }
            if (this.checksum != 0) {
                return size + CodedOutputByteBufferNano.computeInt64Size(4, this.checksum);
            }
            return size;
        }

        public Key mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        int value = input.readInt32();
                        switch (value) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                this.type = value;
                                break;
                            default:
                                continue;
                        }
                    case 18:
                        this.name = input.readString();
                        continue;
                    case 24:
                        this.id = input.readInt64();
                        continue;
                    case 32:
                        this.checksum = input.readInt64();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Key parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Key) MessageNano.mergeFrom(new Key(), data);
        }

        public static Key parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Key().mergeFrom(input);
        }
    }

    public static final class Resource extends MessageNano {
        private static volatile Resource[] _emptyArray;
        public byte[] data;
        public int dpi;

        public static Resource[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Resource[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Resource() {
            clear();
        }

        public Resource clear() {
            this.dpi = 0;
            this.data = WireFormatNano.EMPTY_BYTES;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt32(1, this.dpi);
            output.writeBytes(2, this.data);
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            return (super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt32Size(1, this.dpi)) + CodedOutputByteBufferNano.computeBytesSize(2, this.data);
        }

        public Resource mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        this.dpi = input.readInt32();
                        continue;
                    case 18:
                        this.data = input.readBytes();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Resource parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Resource) MessageNano.mergeFrom(new Resource(), data);
        }

        public static Resource parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Resource().mergeFrom(input);
        }
    }

    public static final class Screen extends MessageNano {
        private static volatile Screen[] _emptyArray;
        public long id;
        public int rank;

        public static Screen[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Screen[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Screen() {
            clear();
        }

        public Screen clear() {
            this.id = 0;
            this.rank = 0;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeInt64(1, this.id);
            if (this.rank != 0) {
                output.writeInt32(2, this.rank);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeInt64Size(1, this.id);
            if (this.rank != 0) {
                return size + CodedOutputByteBufferNano.computeInt32Size(2, this.rank);
            }
            return size;
        }

        public Screen mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 8:
                        this.id = input.readInt64();
                        continue;
                    case 16:
                        this.rank = input.readInt32();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Screen parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Screen) MessageNano.mergeFrom(new Screen(), data);
        }

        public static Screen parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Screen().mergeFrom(input);
        }
    }

    public static final class Widget extends MessageNano {
        private static volatile Widget[] _emptyArray;
        public boolean configure;
        public Resource icon;
        public String label;
        public int minSpanX;
        public int minSpanY;
        public Resource preview;
        public String provider;

        public static Widget[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Widget[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Widget() {
            clear();
        }

        public Widget clear() {
            this.provider = "";
            this.label = "";
            this.configure = false;
            this.icon = null;
            this.preview = null;
            this.minSpanX = 2;
            this.minSpanY = 2;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            output.writeString(1, this.provider);
            if (!this.label.equals("")) {
                output.writeString(2, this.label);
            }
            if (this.configure) {
                output.writeBool(3, this.configure);
            }
            if (this.icon != null) {
                output.writeMessage(4, this.icon);
            }
            if (this.preview != null) {
                output.writeMessage(5, this.preview);
            }
            if (this.minSpanX != 2) {
                output.writeInt32(6, this.minSpanX);
            }
            if (this.minSpanY != 2) {
                output.writeInt32(7, this.minSpanY);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize() + CodedOutputByteBufferNano.computeStringSize(1, this.provider);
            if (!this.label.equals("")) {
                size += CodedOutputByteBufferNano.computeStringSize(2, this.label);
            }
            if (this.configure) {
                size += CodedOutputByteBufferNano.computeBoolSize(3, this.configure);
            }
            if (this.icon != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(4, this.icon);
            }
            if (this.preview != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(5, this.preview);
            }
            if (this.minSpanX != 2) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.minSpanX);
            }
            if (this.minSpanY != 2) {
                return size + CodedOutputByteBufferNano.computeInt32Size(7, this.minSpanY);
            }
            return size;
        }

        public Widget mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        break;
                    case 10:
                        this.provider = input.readString();
                        continue;
                    case 18:
                        this.label = input.readString();
                        continue;
                    case 24:
                        this.configure = input.readBool();
                        continue;
                    case 34:
                        if (this.icon == null) {
                            this.icon = new Resource();
                        }
                        input.readMessage(this.icon);
                        continue;
                    case MotionEventCompat.AXIS_GENERIC_11 /*42*/:
                        if (this.preview == null) {
                            this.preview = new Resource();
                        }
                        input.readMessage(this.preview);
                        continue;
                    case 48:
                        this.minSpanX = input.readInt32();
                        continue;
                    case 56:
                        this.minSpanY = input.readInt32();
                        continue;
                    default:
                        if (!WireFormatNano.parseUnknownField(input, tag)) {
                            break;
                        }
                        continue;
                }
                return this;
            }
        }

        public static Widget parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Widget) MessageNano.mergeFrom(new Widget(), data);
        }

        public static Widget parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Widget().mergeFrom(input);
        }
    }
}
