package cn.myflv.noactive;

interface FreezerInterface {

    boolean thawV2(int pid, int uid);

    boolean freezeV2(int pid, int uid);

    boolean thawV1(int pid);

    boolean freezeV1(int pid);

    boolean kill(int sig,int pid);

}