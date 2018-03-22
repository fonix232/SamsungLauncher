package com.google.android.libraries.launcherclient;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.WindowManager.LayoutParams;

public interface ILauncherOverlay extends IInterface {

    public static abstract class Stub extends Binder implements ILauncherOverlay {
        private static final String DESCRIPTOR = "com.google.android.libraries.launcherclient.ILauncherOverlay";
        static final int TRANSACTION_closeOverlay = 6;
        static final int TRANSACTION_endScroll = 3;
        static final int TRANSACTION_getVoiceSearchLanguage = 11;
        static final int TRANSACTION_hasOverlayContent = 13;
        static final int TRANSACTION_isVoiceDetectionRunning = 12;
        static final int TRANSACTION_onPause = 7;
        static final int TRANSACTION_onResume = 8;
        static final int TRANSACTION_onScroll = 2;
        static final int TRANSACTION_openOverlay = 9;
        static final int TRANSACTION_requestVoiceDetection = 10;
        static final int TRANSACTION_startScroll = 1;
        static final int TRANSACTION_windowAttached = 4;
        static final int TRANSACTION_windowDetached = 5;

        private static class Proxy implements ILauncherOverlay {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void startScroll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onScroll(float progress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(progress);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void endScroll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void windowAttached(LayoutParams attrs, ILauncherOverlayCallback callbacks, int options) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (attrs != null) {
                        _data.writeInt(1);
                        attrs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (callbacks != null) {
                        iBinder = callbacks.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(options);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void windowDetached(boolean isChangingConfigurations) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!isChangingConfigurations) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void closeOverlay(int options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(options);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onResume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void openOverlay(int options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(options);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestVoiceDetection(boolean start) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!start) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public String getVoiceSearchLanguage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVoiceDetectionRunning() throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public boolean hasOverlayContent() throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ILauncherOverlay asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ILauncherOverlay)) {
                return new Proxy(obj);
            }
            return (ILauncherOverlay) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int _arg0 = 0;
            boolean _arg02 = false;
            boolean _result;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    startScroll();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    onScroll(data.readFloat());
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    endScroll();
                    return true;
                case 4:
                    LayoutParams _arg03;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = (LayoutParams) LayoutParams.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    windowAttached(_arg03, com.google.android.libraries.launcherclient.ILauncherOverlayCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = true;
                    }
                    windowDetached(_arg02);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    closeOverlay(data.readInt());
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    onPause();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    onResume();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    openOverlay(data.readInt());
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = true;
                    }
                    requestVoiceDetection(_arg02);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _result2 = getVoiceSearchLanguage();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    _result = isVoiceDetectionRunning();
                    reply.writeNoException();
                    if (_result) {
                        _arg0 = 1;
                    }
                    reply.writeInt(_arg0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    _result = hasOverlayContent();
                    reply.writeNoException();
                    if (_result) {
                        _arg0 = 1;
                    }
                    reply.writeInt(_arg0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void closeOverlay(int i) throws RemoteException;

    void endScroll() throws RemoteException;

    String getVoiceSearchLanguage() throws RemoteException;

    boolean hasOverlayContent() throws RemoteException;

    boolean isVoiceDetectionRunning() throws RemoteException;

    void onPause() throws RemoteException;

    void onResume() throws RemoteException;

    void onScroll(float f) throws RemoteException;

    void openOverlay(int i) throws RemoteException;

    void requestVoiceDetection(boolean z) throws RemoteException;

    void startScroll() throws RemoteException;

    void windowAttached(LayoutParams layoutParams, ILauncherOverlayCallback iLauncherOverlayCallback, int i) throws RemoteException;

    void windowDetached(boolean z) throws RemoteException;
}
