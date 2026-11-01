package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.PersistableBundle;

public interface ICarrierConfigLoader extends android.os.IInterface {
    PersistableBundle getConfigForSubId(int subId, String callingPackage);

    void overrideConfig(int subId, PersistableBundle overrides, boolean persistent);

    abstract class Stub extends Binder implements ICarrierConfigLoader {
        public static ICarrierConfigLoader asInterface(IBinder binder) {
            throw new RuntimeException("STUB");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}