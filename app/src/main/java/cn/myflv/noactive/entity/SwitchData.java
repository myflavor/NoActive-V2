package cn.myflv.noactive.entity;

public class SwitchData {
    private static boolean on = false;

    public static boolean isOn() {
        return on;
    }

    public static void setOn(boolean on) {
        SwitchData.on = on;
    }
}
