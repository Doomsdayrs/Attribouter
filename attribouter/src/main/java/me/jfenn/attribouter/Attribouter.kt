package me.jfenn.attribouter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.XmlRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import me.jfenn.attribouter.activities.AboutActivity
import me.jfenn.attribouter.conductor.AboutController
import me.jfenn.attribouter.fragments.AboutFragment

class Attribouter private constructor(private val context: Context) {
    private var fileRes: Int? = null
    private var gitHubToken: String? = null

    fun withFile(@XmlRes fileRes: Int): Attribouter {
        this.fileRes = fileRes
        return this
    }

    fun withGitHubToken(token: String?): Attribouter {
        gitHubToken = token
        return this
    }

    fun show() {
        val intent = Intent(context, AboutActivity::class.java)
        intent.putExtra(EXTRA_FILE_RES, fileRes)
        intent.putExtra(EXTRA_GITHUB_OAUTH_TOKEN, gitHubToken)
        context.startActivity(intent)
    }

    fun toController(): Controller {
        val bundle = bundleOf(Pair(EXTRA_GITHUB_OAUTH_TOKEN, gitHubToken))
        fileRes?.let { bundle.putInt(EXTRA_FILE_RES, it) }
        return AboutController(bundle)
    }

    fun toFragment(): Fragment {
        val args = Bundle()
        fileRes?.let { args.putInt(EXTRA_FILE_RES, it) }
        args.putString(EXTRA_GITHUB_OAUTH_TOKEN, gitHubToken)
        val fragment = AboutFragment()
        fragment.arguments = args
        return fragment
    }

    companion object {
        const val EXTRA_FILE_RES = "me.jfenn.attribouter.EXTRA_FILE_RES"
        const val EXTRA_GITHUB_OAUTH_TOKEN = "me.jfenn.attribouter.EXTRA_GITHUB_OAUTH_TOKEN"

        @JvmStatic
        fun from(context: Context): Attribouter {
            return Attribouter(context)
        }
    }

}