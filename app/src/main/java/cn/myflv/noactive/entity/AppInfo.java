package cn.myflv.noactive.entity;

import java.util.Set;

import lombok.Data;

@Data
public class AppInfo {
    public String appName;
    public boolean system;
    public boolean white;
    public boolean black;
    public boolean direct;
    public boolean top;
    public boolean socket;
    public Set<String> processSet;
    public Set<String> killProcessSet;
    public Set<String> whiteProcessSet;
}
