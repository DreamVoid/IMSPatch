package android.app;

import android.os.IBinder;

public interface IInstrumentationWatcher extends android.os.IInterface {
    abstract class Stub extends android.os.Binder implements IInstrumentationWatcher {
        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
