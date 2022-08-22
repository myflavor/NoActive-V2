package cn.myflv.noactive.entity;

import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppItem {
    private String appName;
    private String packageName;
    private Drawable appIcon;
    private PackageInfo packageInfo;
    private boolean isWhite;
    private boolean isBlack;
}
