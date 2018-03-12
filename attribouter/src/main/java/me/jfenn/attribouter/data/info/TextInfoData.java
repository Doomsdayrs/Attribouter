package me.jfenn.attribouter.data.info;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import me.jfenn.attribouter.R;
import me.jfenn.attribouter.utils.ResourceUtils;

public class TextInfoData extends InfoData {

    private String text;
    private boolean isCentered;

    public TextInfoData(XmlResourceParser parser) throws XmlPullParserException {
        this(parser.getAttributeValue(null, "text"), parser.getAttributeBooleanValue(null, "centered", false));
    }

    public TextInfoData(String text, boolean isCentered) {
        super(R.layout.item_attribouter_text);
        this.text = text;
        this.isCentered = isCentered;
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public void bind(Context context, ViewHolder viewHolder) {
        TextView textView = (TextView) viewHolder.itemView;
        textView.setText(ResourceUtils.getString(context, text));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            textView.setTextAlignment(isCentered ? View.TEXT_ALIGNMENT_CENTER : View.TEXT_ALIGNMENT_GRAVITY);
    }

}
