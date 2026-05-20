package android.app;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;

public interface IActivityManager extends android.os.IInterface {
    void startDelegateShellPermissionIdentity(int uid, String[] permissions);

    void stopDelegateShellPermissionIdentity();

    boolean startInstrumentation(ComponentName className, String profileFile, int flags, Bundle arguments,
                                 IInstrumentationWatcher watcher, IUiAutomationConnection connection,
                                 int userId, String abiOverride);

    abstract class Stub extends android.os.Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder binder) {
            throw new RuntimeException("STUB");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
