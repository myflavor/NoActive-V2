package cn.myflv.noactive.provide;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import cn.myflv.noactive.FreezerInterface;
import cn.myflv.noactive.utils.BaseFreezeUtils;

public class FreezerService extends Service {
    public FreezerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FreezeBinder();
    }

    static class FreezeBinder extends FreezerInterface.Stub {

        @Override
        public boolean thawV2(int pid, int uid) throws RemoteException {
            return BaseFreezeUtils.thawPid(true, pid, uid);
        }

        @Override
        public boolean freezeV2(int pid, int uid) throws RemoteException {
            return BaseFreezeUtils.freezePid(true, pid, uid);
        }

        @Override
        public boolean thawV1(int pid) throws RemoteException {
            return BaseFreezeUtils.thawPid(true, pid);
        }

        @Override
        public boolean freezeV1(int pid) throws RemoteException {
            return BaseFreezeUtils.freezePid(true, pid);
        }

        @Override
        public boolean kill(int sig, int pid) throws RemoteException {
            return BaseFreezeUtils.kill(true, sig, pid);
        }

    }
}