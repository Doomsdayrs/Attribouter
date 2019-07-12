package me.jfenn.attribouter.wedges

import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import me.jfenn.attribouter.R
import me.jfenn.attribouter.adapters.WedgeAdapter
import me.jfenn.attribouter.data.github.RepositoryData
import me.jfenn.attribouter.utils.ResourceUtils
import me.jfenn.attribouter.wedges.link.GitHubLinkWedge
import me.jfenn.attribouter.wedges.link.LinkWedge
import me.jfenn.attribouter.wedges.link.PlayStoreLinkWedge
import me.jfenn.attribouter.wedges.link.WebsiteLinkWedge
import java.util.*

class AppWedge: Wedge<AppWedge.ViewHolder>(R.layout.item_attribouter_app_info) {

    private val icon: String? by attr("icon")
    private var description: String? by attr("description")
    private val repo: String? by attr("repo")
    private val gitHubUrl: String? by attr("gitHubUrl")
    private val websiteUrl: String? by attr("websiteUrl")
    private val playStoreUrl: String? by attr("playStoreUrl")

    override fun onCreate() {
        (gitHubUrl ?: repo?.let { "https://github.com/$it" })?.let {
            addChild(GitHubLinkWedge(it, 0).create())
        }

        websiteUrl?.let {
            addChild(WebsiteLinkWedge(it, 0).create())
        }

        playStoreUrl?.let {
            addChild(PlayStoreLinkWedge(it, 0).create())
        }

        repo?.let { getProvider()?.getRepository(it)?.subscribe { data -> onRepository(data) } }
    }

    fun onRepository(repo: RepositoryData) {
        repo.description?.let { repoDescription ->
            if ((description == null || !description!!.startsWith("^")))
                description = repoDescription
        }

        repo.html_url?.let { repoUrl ->
            addChild(GitHubLinkWedge(repoUrl, 0, true).create())
        }

        repo.homepage?.let { repoHomepage ->
            addChild(
                    if (repoHomepage.startsWith("https://play.google.com/"))
                        PlayStoreLinkWedge(repoHomepage, 0).create()
                    else WebsiteLinkWedge(repoHomepage, 0).create()
            )
        }
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    override fun bind(context: Context, viewHolder: ViewHolder) {
        val info = context.applicationInfo
        ResourceUtils.setImage(context, icon, info.icon, viewHolder.appIconView)
        viewHolder.nameTextView?.setText(info.labelRes)

        viewHolder.versionTextView?.apply {
            try {
                val packageInfo = context.packageManager.getPackageInfo(info.packageName, 0)
                text = String.format(context.getString(R.string.title_attribouter_version), packageInfo.versionName)
                visibility = View.VISIBLE
            } catch (e: PackageManager.NameNotFoundException) {
                visibility = View.GONE
            }
        }

        viewHolder.descriptionTextView?.apply {
            ResourceUtils.getString(context, description)?.let {
                visibility = View.VISIBLE
                text = it
            } ?: run {
                visibility = View.GONE
            }
        }

        viewHolder.links?.apply {
            val children = getChildren(LinkWedge::class.java)
            if (children.size > 0) {
                val links = children.filter { link -> !link.isHidden }
                Collections.sort(links, LinkWedge.Comparator(context))

                val flexbox = FlexboxLayoutManager(context)
                flexbox.flexDirection = FlexDirection.ROW
                flexbox.justifyContent = JustifyContent.CENTER
                layoutManager = flexbox
                adapter = WedgeAdapter(links)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    class ViewHolder(v: View) : Wedge.ViewHolder(v) {

        var appIconView: ImageView? = v.findViewById(R.id.appIcon)
        var nameTextView: TextView? = v.findViewById(R.id.appName)
        var versionTextView: TextView? = v.findViewById(R.id.appVersion)
        var descriptionTextView: TextView? = v.findViewById(R.id.description)
        var links: RecyclerView? = v.findViewById(R.id.appLinks)

    }
}