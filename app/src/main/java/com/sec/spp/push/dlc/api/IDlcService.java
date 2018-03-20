package com.sec.spp.push.dlc.api;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDlcService extends IInterface {

    /* renamed from: com.sec.spp.push.dlc.api.IDlcService$a */
    public static abstract class AbstractDlcService extends Binder implements IDlcService {
        /* renamed from: a */
        static final int f239a = 1;
        /* renamed from: b */
        static final int f240b = 2;
        /* renamed from: c */
        static final int f241c = 3;
        /* renamed from: d */
        static final int f242d = 4;
        /* renamed from: e */
        private static final String serviceTag = "com.sec.spp.push.dlc.api.IDlcService";

        /* renamed from: com.sec.spp.push.dlc.api.IDlcService$a$a */
        private static class DlcService implements IDlcService {
            /* renamed from: a */
            private IBinder binder;

            DlcService(IBinder iBinder) {
                this.binder = iBinder;
            }

            /* renamed from: a */
            public String ServiceTag() {
                return AbstractDlcService.serviceTag;
            }

            public IBinder asBinder() {
                return this.binder;
            }

            public int requestSend(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7) {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                int readInt = 0;
                try {
                    obtain.writeInterfaceToken(AbstractDlcService.serviceTag);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeLong(j);
                    obtain.writeString(str3);
                    obtain.writeString(str4);
                    obtain.writeString(str5);
                    obtain.writeString(str6);
                    obtain.writeString(str7);
                    this.binder.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    readInt = obtain2.readInt();

                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
                return readInt;
            }

            public int requestSendAggregation(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7, int i, long j2, long j3, long j4, long j5, long j6) {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                int readInt = 0;
                try {
                    obtain.writeInterfaceToken(AbstractDlcService.serviceTag);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeLong(j);
                    obtain.writeString(str3);
                    obtain.writeString(str4);
                    obtain.writeString(str5);
                    obtain.writeString(str6);
                    obtain.writeString(str7);
                    obtain.writeInt(i);
                    obtain.writeLong(j2);
                    obtain.writeLong(j3);
                    obtain.writeLong(j4);
                    obtain.writeLong(j5);
                    obtain.writeLong(j6);
                    this.binder.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    readInt = obtain2.readInt();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
                return readInt;
            }

            public int requestSendSummary(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7, String str8, long j2, long j3, long j4, long j5, long j6) {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                int readInt = 0;
                try {
                    obtain.writeInterfaceToken(AbstractDlcService.serviceTag);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeLong(j);
                    obtain.writeString(str3);
                    obtain.writeString(str4);
                    obtain.writeString(str5);
                    obtain.writeString(str6);
                    obtain.writeString(str7);
                    obtain.writeString(str8);
                    obtain.writeLong(j2);
                    obtain.writeLong(j3);
                    obtain.writeLong(j4);
                    obtain.writeLong(j5);
                    obtain.writeLong(j6);
                    this.binder.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    readInt = obtain2.readInt();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
                return readInt;
            }

            public int requestSendUrgent(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7) {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                int readInt = 0;
                try {
                    obtain.writeInterfaceToken(AbstractDlcService.serviceTag);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeLong(j);
                    obtain.writeString(str3);
                    obtain.writeString(str4);
                    obtain.writeString(str5);
                    obtain.writeString(str6);
                    obtain.writeString(str7);
                    this.binder.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    readInt = obtain2.readInt();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
                return readInt;
            }
        }

        public AbstractDlcService() {
            attachInterface(this, serviceTag);
        }

        /* renamed from: a */
        public static IDlcService m200a(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(serviceTag);
            return (queryLocalInterface == null || !(queryLocalInterface instanceof IDlcService)) ? new DlcService(iBinder) : (IDlcService) queryLocalInterface;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            int requestSend;
            switch (i) {
                case 1:
                    parcel.enforceInterface(serviceTag);
                    requestSend = requestSend(parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(requestSend);
                    return true;
                case 2:
                    parcel.enforceInterface(serviceTag);
                    requestSend = requestSendUrgent(parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(requestSend);
                    return true;
                case 3:
                    parcel.enforceInterface(serviceTag);
                    requestSend = requestSendAggregation(parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(requestSend);
                    return true;
                case 4:
                    parcel.enforceInterface(serviceTag);
                    requestSend = requestSendSummary(parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(requestSend);
                    return true;
                case 1598968902:
                    parcel2.writeString(serviceTag);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    int requestSend(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7);

    int requestSendAggregation(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7, int i, long j2, long j3, long j4, long j5, long j6);

    int requestSendSummary(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7, String str8, long j2, long j3, long j4, long j5, long j6);

    int requestSendUrgent(String str, String str2, long j, String str3, String str4, String str5, String str6, String str7);
}
