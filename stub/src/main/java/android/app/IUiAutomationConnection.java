package android.app;

import android.os.IBinder;

public interface IUiAutomationConnection extends android.os.IInterface {
    abstract class Stub extends android.os.Binder implements IUiAutomationConnection {
        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
