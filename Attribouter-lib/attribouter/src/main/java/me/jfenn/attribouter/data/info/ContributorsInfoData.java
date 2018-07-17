package me.jfenn.attribouter.data.info;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.jfenn.attribouter.R;
import me.jfenn.attribouter.adapters.InfoAdapter;
import me.jfenn.attribouter.data.github.ContributorsData;
import me.jfenn.attribouter.data.github.GitHubData;
import me.jfenn.attribouter.data.github.UserData;
import me.jfenn.attribouter.dialogs.OverflowDialog;
import me.jfenn.attribouter.dialogs.UserDialog;
import me.jfenn.attribouter.utils.ResourceUtils;
import me.jfenn.attribouter.utils.UrlClickListener;

public class ContributorsInfoData extends InfoData<ContributorsInfoData.ViewHolder> {

    @Nullable
    private String repo;
    @Nullable
    private String contributorsTitle;
    private int overflow;
    private List<ContributorInfoData> contributors;

    public ContributorsInfoData(XmlResourceParser parser) throws XmlPullParserException, IOException {
        super(R.layout.item_attribouter_contributors);
        repo = parser.getAttributeValue(null, "repo");
        contributors = new ArrayList<>();
        contributorsTitle = parser.getAttributeValue(null, "title");
        if (contributorsTitle == null)
            contributorsTitle = "@string/title_attribouter_contributors";
        boolean showDefaults = parser.getAttributeBooleanValue(null, "showDefaults", true);
        overflow = parser.getAttributeIntValue(null, "overflow", -1);
        while (parser.next() != XmlResourceParser.END_TAG || parser.getName().equals("contributor")) {
            if (parser.getEventType() == XmlResourceParser.START_TAG && parser.getName().equals("contributor")) {
                int position = parser.getAttributeIntValue(null, "position", -1);
                ContributorInfoData contributor = new ContributorInfoData(parser, position != -1 ? position : null);

                if (!contributors.contains(contributor))
                    contributors.add(contributor);
                else contributors.get(contributors.indexOf(contributor)).merge(contributor);

                if (contributor.login != null && !contributor.hasEverything())
                    addRequest(new UserData(contributor.login));
            }
        }

        if (showDefaults) {
            ContributorInfoData me = new ContributorInfoData(
                    "TheAndroidMaster",
                    "James Fenn",
                    "https://avatars1.githubusercontent.com/u/13000407",
                    "^Library Developer",
                    null,
                    "Android developer and co-founder of Double Dot Labs. Writes Java, C, and HTML. PHP confuses me.",
                    "https://jfenn.me/",
                    "dev@jfenn.me"
            );

            if (!contributors.contains(me))
                contributors.add(me);

            addRequest(new ContributorsData("TheAndroidMaster/Attribouter"));
        }

        addRequest(new ContributorsData(repo));
    }

    @Override
    public void onInit(GitHubData data) {
        if (data instanceof ContributorsData) {
            if (((ContributorsData) data).contributors != null) {
                for (ContributorsData.ContributorData contributor : ((ContributorsData) data).contributors) {
                    if (contributor.login == null)
                        continue;

                    ContributorInfoData mergeContributor = new ContributorInfoData(
                            contributor.login,
                            null,
                            contributor.avatar_url,
                            repo != null && repo.startsWith(contributor.login) ? "Owner" : "Contributor",
                            null,
                            null,
                            null,
                            null
                    );

                    ContributorInfoData contributorInfo = mergeContributor;

                    if (contributors.contains(mergeContributor)) {
                        contributorInfo = contributors.get(contributors.indexOf(mergeContributor));
                        contributorInfo.merge(mergeContributor);
                    } else contributors.add(contributorInfo);

                    if (!contributorInfo.hasEverything())
                        addRequest(new UserData(contributor.login));
                }
            }
        } else if (data instanceof UserData) {
            UserData user = (UserData) data;
            ContributorInfoData contributor = new ContributorInfoData(
                    user.login,
                    user.name,
                    user.avatar_url,
                    repo != null && repo.startsWith(user.login) ? "Owner" : "Contributor",
                    null,
                    user.bio,
                    user.blog,
                    user.email
            );

            if (!contributors.contains(contributor))
                contributors.add(0, contributor);
            else contributors.get(contributors.indexOf(contributor)).merge(contributor);
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
            viewHolder.topThreeView.setVisibility(View.GONE);

            viewHolder.overflow.setVisibility(View.VISIBLE);
            viewHolder.overflow.setText(String.format(context.getString(R.string.title_attribouter_view_overflow), contributorsTitle));

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new OverflowDialog(v.getContext(), contributorsTitle, new ArrayList<InfoData>(contributors)).show();
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

        if (contributorsTitle != null)
            viewHolder.titleView.setText(ResourceUtils.getString(context, contributorsTitle));

        ContributorInfoData first = null, second = null, third = null;
        List<InfoData> remainingContributors = new ArrayList<>();
        int hiddenContributors = 0;
        for (ContributorInfoData contributor : contributors) {
            if (contributor.isHidden) {
                hiddenContributors++;
                continue;
            }

            if (contributor.position != null) {
                if (first == null && contributor.position == 1) {
                    first = contributor;
                    continue;
                } else if (second == null && contributor.position == 2) {
                    second = contributor;
                    continue;
                } else if (third == null && contributor.position == 3) {
                    third = contributor;
                    continue;
                }
            }

            if (remainingContributors.size() < overflow || overflow == -1)
                remainingContributors.add(contributor);
        }

        if (first != null && second != null && third != null) {
            viewHolder.topThreeView.setVisibility(View.VISIBLE);

            viewHolder.firstNameView.setText(ResourceUtils.getString(context, first.getName()));
            ResourceUtils.setImage(context, first.avatarUrl, viewHolder.firstImageView);
            if (first.task != null) {
                viewHolder.firstTaskView.setVisibility(View.VISIBLE);
                viewHolder.firstTaskView.setText(ResourceUtils.getString(context, first.task));
            } else viewHolder.firstTaskView.setVisibility(View.GONE);

            viewHolder.firstView.setTag(first);
            if (ResourceUtils.getString(context, first.bio) != null) {
                viewHolder.firstView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new UserDialog(view.getContext(), (ContributorInfoData) view.getTag())
                                .show();
                    }
                });
            } else if (ResourceUtils.getString(context, first.blog) != null) {
                viewHolder.firstView.setOnClickListener(new UrlClickListener(ResourceUtils.getString(context, first.blog)));
            } else if (first.login != null) {
                viewHolder.firstView.setOnClickListener(new UrlClickListener("https://github.com/" + first.login));
            } else viewHolder.firstView.setOnClickListener(null);

            viewHolder.secondNameView.setText(ResourceUtils.getString(context, second.getName()));
            ResourceUtils.setImage(context, second.avatarUrl, viewHolder.secondImageView);
            if (second.task != null) {
                viewHolder.secondTaskView.setVisibility(View.VISIBLE);
                viewHolder.secondTaskView.setText(ResourceUtils.getString(context, second.task));
            } else viewHolder.secondTaskView.setVisibility(View.GONE);

            viewHolder.secondView.setTag(second);
            if (ResourceUtils.getString(context, second.bio) != null) {
                viewHolder.secondView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new UserDialog(view.getContext(), (ContributorInfoData) view.getTag())
                                .show();
                    }
                });
            } else if (ResourceUtils.getString(context, second.blog) != null) {
                viewHolder.secondView.setOnClickListener(new UrlClickListener(ResourceUtils.getString(context, second.blog)));
            } else if (second.login != null) {
                viewHolder.secondView.setOnClickListener(new UrlClickListener("https://github.com/" + second.login));
            } else viewHolder.secondView.setOnClickListener(null);

            viewHolder.thirdNameView.setText(ResourceUtils.getString(context, third.getName()));
            ResourceUtils.setImage(context, third.avatarUrl, viewHolder.thirdImageView);
            if (third.task != null) {
                viewHolder.thirdTaskView.setVisibility(View.VISIBLE);
                viewHolder.thirdTaskView.setText(ResourceUtils.getString(context, third.task));
            } else viewHolder.thirdTaskView.setVisibility(View.GONE);

            viewHolder.thirdView.setTag(third);
            if (ResourceUtils.getString(context, third.bio) != null) {
                viewHolder.thirdView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new UserDialog(view.getContext(), (ContributorInfoData) view.getTag())
                                .show();
                    }
                });
            } else if (ResourceUtils.getString(context, third.blog) != null) {
                viewHolder.thirdView.setOnClickListener(new UrlClickListener(ResourceUtils.getString(context, third.blog)));
            } else if (third.login != null) {
                viewHolder.thirdView.setOnClickListener(new UrlClickListener("https://github.com/" + third.login));
            } else viewHolder.thirdView.setOnClickListener(null);
        } else {
            viewHolder.topThreeView.setVisibility(View.GONE);

            if (third != null)
                remainingContributors.add(0, third);

            if (second != null)
                remainingContributors.add(0, second);

            if (first != null)
                remainingContributors.add(0, first);
        }

        if (remainingContributors.size() > 0) {
            viewHolder.recycler.setVisibility(View.VISIBLE);
            viewHolder.recycler.setLayoutManager(new LinearLayoutManager(context));
            viewHolder.recycler.setAdapter(new InfoAdapter(remainingContributors));
        } else viewHolder.recycler.setVisibility(View.GONE);

        if (remainingContributors.size() + (first != null && second != null && third != null ? 3 : 0) < contributors.size() - hiddenContributors) {
            viewHolder.expand.setVisibility(View.VISIBLE);
            viewHolder.expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<InfoData> overflowList = new ArrayList<>();
                    for (ContributorInfoData contributor : contributors) {
                        if (!contributor.isHidden)
                            overflowList.add(contributor);
                    }

                    new OverflowDialog(v.getContext(), contributorsTitle, overflowList).show();
                }
            });
        } else viewHolder.expand.setVisibility(View.GONE);
    }

    class ViewHolder extends InfoData.ViewHolder {

        private TextView titleView;

        private View topThreeView;
        private View firstView;
        private ImageView firstImageView;
        private TextView firstNameView;
        private TextView firstTaskView;
        private View secondView;
        private ImageView secondImageView;
        private TextView secondNameView;
        private TextView secondTaskView;
        private View thirdView;
        private ImageView thirdImageView;
        private TextView thirdNameView;
        private TextView thirdTaskView;
        private View expand;
        private TextView overflow;

        private RecyclerView recycler;

        ViewHolder(View v) {
            super(v);

            titleView = v.findViewById(R.id.contributorsTitle);
            topThreeView = v.findViewById(R.id.topThree);
            firstView = v.findViewById(R.id.first);
            firstImageView = v.findViewById(R.id.firstImage);
            firstNameView = v.findViewById(R.id.firstName);
            firstTaskView = v.findViewById(R.id.firstTask);
            secondView = v.findViewById(R.id.second);
            secondImageView = v.findViewById(R.id.secondImage);
            secondNameView = v.findViewById(R.id.secondName);
            secondTaskView = v.findViewById(R.id.secondTask);
            thirdView = v.findViewById(R.id.third);
            thirdImageView = v.findViewById(R.id.thirdImage);
            thirdNameView = v.findViewById(R.id.thirdName);
            thirdTaskView = v.findViewById(R.id.thirdTask);
            recycler = v.findViewById(R.id.recycler);
            expand = v.findViewById(R.id.expand);
            overflow = v.findViewById(R.id.overflow);
        }
    }

}
