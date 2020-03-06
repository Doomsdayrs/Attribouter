package me.jfenn.attribouter.conductor

import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.Controller
import me.jfenn.attribouter.Attribouter
import me.jfenn.attribouter.R
import me.jfenn.attribouter.adapters.InfoAdapter
import me.jfenn.attribouter.data.github.GitHubData
import me.jfenn.attribouter.wedges.*
import me.jfenn.attribouter.wedges.Wedge.OnRequestListener
import org.xmlpull.v1.XmlPullParser
import kotlin.collections.ArrayList

class AboutController(bundle: Bundle?) : Controller(bundle), GitHubData.OnInitListener, OnRequestListener {
    private var recycler: RecyclerView? = null
    private var adapter: InfoAdapter? = null
    private var infos: ArrayList<Wedge<*>> = ArrayList()
    private var requests: ArrayList<GitHubData> = ArrayList()
    private var gitHubToken: String? = null

    init {
        var fileRes = R.xml.attribouter
        if (bundle != null) {
            gitHubToken = bundle.getString(Attribouter.EXTRA_GITHUB_OAUTH_TOKEN, null)
            fileRes = bundle.getInt(Attribouter.EXTRA_FILE_RES, fileRes)
        }
        val parser = resources!!.getXml(fileRes)
        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    try {
                        val classy = Class.forName(parser.name)
                        val constructor = classy.getConstructor(XmlResourceParser::class.java)
                        infos.add(constructor.newInstance(parser) as Wedge<*>)
                        parser.next()
                        continue
                    } catch (e: Exception) {
                        when (e) {
                            is ClassNotFoundException -> Log.e("Attribouter", "Class name \"" + parser.name + "\" not found - you should probably check your configuration file for typos.")
                            is NoSuchMethodException -> Log.e("Attribouter", "Class \"" + parser.name + "\" definitely exists, but doesn't have the correct constructor. Check that you have defined one with a single argument - \'android.content.res.XmlResourceParser\'")
                            is ClassCastException -> Log.e("Attribouter", "Class \"" + parser.name + "\" has been instantiated correctly, but it must extend \'me.jfenn.attribouter.data.info.InfoData\' to be worthy of the great RecyclerView adapter.")
                        }
                        e.printStackTrace()
                    }
                    when (parser.name) {
                        "appInfo" -> infos.add(AppWedge(parser))
                        "contributors" -> infos.add(ContributorsWedge(parser))
                        "translators" -> infos.add(TranslatorsWedge(parser))
                        "licenses" -> infos.add(LicensesWedge(parser))
                        "text" -> infos.add(TextWedge(parser))
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        parser.close()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        recycler = inflater.inflate(R.layout.fragment_attribouter_about, container, false) as RecyclerView
        infos = ArrayList()
        adapter = InfoAdapter(infos)
        recycler!!.layoutManager = LinearLayoutManager(applicationContext)
        recycler!!.addItemDecoration(DividerItemDecoration(recycler!!.context, DividerItemDecoration.VERTICAL))
        recycler!!.adapter = adapter
        requests = ArrayList()

        infos.forEach { it.setOnRequestListener(this) }
        requests.forEach {
            it.addOnInitListener(this)
            it.startInit(applicationContext, gitHubToken)
        }
        return recycler as RecyclerView
    }

    override fun onInit(data: GitHubData) {
        infos.indices.forEach { i ->
            if (infos[i].hasRequest(data)) adapter!!.notifyItemChanged(i) else notifyChildren(i, infos[i].children, data)
        }
        recycler!!.smoothScrollToPosition(0)
    }

    private fun notifyChildren(index: Int, children: List<Wedge<*>>, data: GitHubData) {
        if (children.isEmpty()) return
        children.forEach {
            if (it.hasRequest(data)) {
                adapter!!.notifyItemChanged(index)
                return
            }
            notifyChildren(index, it.children, data)
        }
    }

    override fun onFailure(data: GitHubData) {}

    override fun onDestroyView(view: View) {
        recycler = null
        requests.forEach { it.interruptThread() }
    }

    override fun onRequest(info: Wedge<*>, request: GitHubData) {
        if (!requests.contains(request)) {
            requests.add(request)
            request.addOnInitListener(this)
            request.startInit(applicationContext, gitHubToken)
        } else {
            val i = requests.indexOf(request)
            val activeRequest = requests[i].merge(request)
            if (activeRequest.isInitialized) info.onInit(activeRequest)
        }
    }
}