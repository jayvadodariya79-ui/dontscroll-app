package com.jayv.dontscroll

import android.app.Application
import com.jayv.dontscroll.data.Prefs
import com.jayv.dontscroll.data.Repo
import com.jayv.dontscroll.notify.Notifier

class DontScrollApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Repo.init(this)
        Notifier.createChannels(this)
        Notifier.scheduleSummary(this)
    }
}
