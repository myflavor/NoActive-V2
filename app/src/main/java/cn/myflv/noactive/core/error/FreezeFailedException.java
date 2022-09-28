package cn.myflv.noactive.core.error;

public class FreezeFailedException extends Exception {
    public FreezeFailedException() {
        super("process died or not supported");
    }
}
