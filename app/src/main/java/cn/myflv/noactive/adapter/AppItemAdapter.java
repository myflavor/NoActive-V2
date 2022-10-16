package cn.myflv.noactive.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import cn.myflv.noactive.R;
import cn.myflv.noactive.entity.AppItem;

public class AppItemAdapter extends ArrayAdapter<AppItem> {

    private final int resourceId;

    public AppItemAdapter(@NonNull Context context, int resource, List<AppItem> objects) {
        super(context, resource, objects);
        this.resourceId = resource;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AppItem appItem = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
        }
        ImageView appIconImage = convertView.findViewById(R.id.app_icon);
        TextView appNameText = convertView.findViewById(R.id.app_name);
        TextView packageNameText = convertView.findViewById(R.id.package_name);
        TextView white_app = convertView.findViewById(R.id.white_app);
        white_app.setVisibility(appItem.isWhite() ? View.VISIBLE : View.GONE);
        TextView black_app = convertView.findViewById(R.id.black_app);
        black_app.setVisibility(appItem.isBlack() ? View.VISIBLE : View.GONE);
        TextView idle_app = convertView.findViewById(R.id.battery_opt);
        idle_app.setVisibility(appItem.isIdle() ? View.VISIBLE : View.GONE);

        TextView app_background = convertView.findViewById(R.id.app_background);
        if (appItem.isTop()) {
            app_background.setText(R.string.top_app);
            app_background.setVisibility(!appItem.isWhite() ? View.VISIBLE : View.GONE);
        } else if (appItem.isDirect()) {
            app_background.setText(R.string.direct_app);
            app_background.setVisibility(!appItem.isWhite() ? View.VISIBLE : View.GONE);
        } else {
            app_background.setVisibility(View.GONE);
        }

        TextView other_config = convertView.findViewById(R.id.other_config);
        boolean otherConfig = (appItem.getWhiteProcCount() + appItem.getKillProcCount() > 0) || appItem.isSocket();
        other_config.setVisibility(otherConfig ? View.VISIBLE : View.GONE);
        /*
        TextView kill_proc = convertView.findViewById(R.id.kill_proc);
        kill_proc.setText(String.valueOf(appItem.getKillProcCount()));
        kill_proc.setVisibility((appItem.getKillProcCount() > 0) ? View.VISIBLE : View.GONE);

        TextView white_proc = convertView.findViewById(R.id.white_proc);
        white_proc.setText(String.valueOf(appItem.getWhiteProcCount()));
        white_proc.setVisibility((appItem.getWhiteProcCount() > 0) ? View.VISIBLE : View.GONE);
         */

        appIconImage.setImageDrawable(appItem.getAppIcon());
        appNameText.setText(appItem.getAppName());
        packageNameText.setText(appItem.getPackageName());
        return convertView;
    }
}
