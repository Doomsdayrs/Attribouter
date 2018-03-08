package me.jfenn.attribouter.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.jfenn.attribouter.R;

public class AppInfoData extends InfoData<AppInfoData.ViewHolder> {

    public AppInfoData() {
        super(R.layout.item_attribouter_app_info);
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public void bind(Context context, ViewHolder viewHolder) {
        ApplicationInfo info = context.getApplicationInfo();
        viewHolder.appIconView.setImageResource(info.icon);
        viewHolder.nameTextView.setText(info.labelRes);

    }

    static class ViewHolder extends InfoData.ViewHolder {

        ImageView appIconView;
        TextView nameTextView;
        TextView versionTextView;
        View websiteButton;
        View gitHubButton;
        View playStoreButton;

        private ViewHolder(View v) {
            super(v);
            appIconView = v.findViewById(R.id.appIcon);
            nameTextView = v.findViewById(R.id.appName);
            versionTextView = v.findViewById(R.id.appVersion);
            websiteButton = v.findViewById(R.id.appLinkWebsite);
            gitHubButton = v.findViewById(R.id.appLinkGitHub);
            playStoreButton = v.findViewById(R.id.appLinkPlayStore);
        }

    }
}
