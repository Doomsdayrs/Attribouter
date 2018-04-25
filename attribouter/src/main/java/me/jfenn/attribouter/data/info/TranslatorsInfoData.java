package me.jfenn.attribouter.data.info;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.jfenn.attribouter.R;
import me.jfenn.attribouter.adapters.InfoAdapter;
import me.jfenn.attribouter.data.github.ContributorsData;
import me.jfenn.attribouter.data.github.GitHubData;
import me.jfenn.attribouter.data.github.UserData;
import me.jfenn.attribouter.dialogs.OverflowDialog;
import me.jfenn.attribouter.utils.ResourceUtils;

public class TranslatorsInfoData extends InfoData<TranslatorsInfoData.ViewHolder> {

    @Nullable
    private String translatorsTitle;
    private List<TranslatorInfoData> translators;
    private List<InfoData> sortedTranslators;
    private int overflow;

    public TranslatorsInfoData(XmlResourceParser parser) throws XmlPullParserException, IOException {
        super(R.layout.item_attribouter_translators);
        translators = new ArrayList<>();
        translatorsTitle = parser.getAttributeValue(null, "title");
        if (translatorsTitle == null)
            translatorsTitle = "@string/title_attribouter_translators";
        overflow = parser.getAttributeIntValue(null, "overflow", -1);
        while (parser.getEventType() != XmlResourceParser.END_TAG || parser.getName().equals("translator")) {
            parser.next();
            if (parser.getEventType() == XmlResourceParser.START_TAG && parser.getName().equals("translator")) {
                TranslatorInfoData translator = new TranslatorInfoData(parser);

                if (!translators.contains(translator))
                    translators.add(translator);
                else translators.get(translators.indexOf(translator)).merge(translator);

                if (translator.login != null && !translator.hasEverything())
                    addRequest(new UserData(translator.login));
            }
        }
    }

    @Override
    public void onInit(GitHubData data) {
        if (data instanceof ContributorsData) {
            if (((ContributorsData) data).contributors != null) {
                for (ContributorsData.ContributorData contributor : ((ContributorsData) data).contributors) {
                    if (contributor.login == null)
                        continue;

                    TranslatorInfoData mergeTranslator = new TranslatorInfoData(
                            contributor.login,
                            null,
                            contributor.avatar_url,
                            null,
                            null,
                            null
                    );

                    TranslatorInfoData translatorInfo = mergeTranslator;

                    if (translators.contains(mergeTranslator)) {
                        translatorInfo = translators.get(translators.indexOf(mergeTranslator));
                        translatorInfo.merge(mergeTranslator);
                    } else translators.add(translatorInfo);

                    if (!translatorInfo.hasEverything())
                        addRequest(new UserData(contributor.login));
                }
            }
        } else if (data instanceof UserData) {
            UserData user = (UserData) data;
            TranslatorInfoData translator = new TranslatorInfoData(
                    user.login,
                    user.name,
                    user.avatar_url,
                    null,
                    user.blog,
                    user.email
            );

            if (!translators.contains(translator))
                translators.add(0, translator);
            else translators.get(translators.indexOf(translator)).merge(translator);
        }
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public void bind(Context context, ViewHolder viewHolder) {
        if (overflow == 0) {
            viewHolder.titleView.setVisibility(View.GONE);
            viewHolder.recycler.setVisibility(View.GONE);
            viewHolder.expand.setVisibility(View.GONE);

            viewHolder.overflow.setVisibility(View.VISIBLE);
            viewHolder.overflow.setText(String.format(context.getString(R.string.title_attribouter_view_overflow), translatorsTitle));

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new OverflowDialog(v.getContext(), translatorsTitle, sortedTranslators).show();
                }
            });
            return;
        } else {
            viewHolder.titleView.setVisibility(View.VISIBLE);
            viewHolder.recycler.setVisibility(View.VISIBLE);
            viewHolder.expand.setVisibility(View.VISIBLE);
            viewHolder.overflow.setVisibility(View.GONE);
            viewHolder.itemView.setOnClickListener(null);
        }

        if (translatorsTitle != null)
            viewHolder.titleView.setText(ResourceUtils.getString(context, translatorsTitle));

        int remaining = overflow;
        List<InfoData> sortedList = new ArrayList<>();
        sortedTranslators = new ArrayList<>();
        for (String language : Locale.getISOLanguages()) {
            boolean isHeader = false;
            for (TranslatorInfoData translator : translators) {
                if (translator.locales == null)
                    continue;

                boolean isLocale = false;
                for (String locale : translator.locales.split(",")) {
                    if (language.equals(locale)) {
                        isLocale = true;
                        break;
                    }
                }

                if (isLocale) {
                    if (!isHeader) {
                        InfoData header = new HeaderInfoData(new Locale(language).getDisplayLanguage());
                        sortedTranslators.add(header);
                        if (remaining != 0)
                            sortedList.add(header);

                        isHeader = true;
                    }

                    sortedTranslators.add(translator);
                    if (remaining != 0) {
                        sortedList.add(translator);
                        remaining--;
                    }
                }
            }
        }


        viewHolder.recycler.setLayoutManager(new LinearLayoutManager(context));
        viewHolder.recycler.setAdapter(new InfoAdapter(sortedList));

        if (sortedTranslators.size() > sortedList.size()) {
            viewHolder.expand.setVisibility(View.VISIBLE);
            viewHolder.expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new OverflowDialog(v.getContext(), translatorsTitle, sortedTranslators).show();
                }
            });
        } else viewHolder.expand.setVisibility(View.GONE);
    }

    class ViewHolder extends InfoData.ViewHolder {

        private TextView titleView;
        private RecyclerView recycler;
        private View expand;
        private TextView overflow;

        ViewHolder(View v) {
            super(v);

            titleView = v.findViewById(R.id.contributorsTitle);
            recycler = v.findViewById(R.id.recycler);
            expand = v.findViewById(R.id.expand);
            overflow = v.findViewById(R.id.overflow);
        }
    }

}
