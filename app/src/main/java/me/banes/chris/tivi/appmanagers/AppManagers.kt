/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.banes.chris.tivi.appmanagers

import android.app.Application
import com.gabrielittner.threetenbp.LazyThreeTen
import com.squareup.leakcanary.LeakCanary
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import me.banes.chris.tivi.BuildConfig
import me.banes.chris.tivi.extensions.plusAssign
import me.banes.chris.tivi.util.AppRxSchedulers
import timber.log.Timber
import javax.inject.Inject

class AppManagers(vararg params: AppManager) : AppManager {
    private val managers = params.asList()

    override fun init(application: Application) {
        for (m in managers) {
            m.init(application)
        }
    }
}

class ThreeTenBpManager @Inject constructor(private val schedulers: AppRxSchedulers) : AppManager {
    private val disposables = CompositeDisposable()

    override fun init(application: Application) {
        // Init LazyThreeTen
        LazyThreeTen.init(application)

        // ...and cache it's timezones on a background thread
        disposables += Completable.fromCallable { LazyThreeTen.cacheZones() }
                .subscribeOn(schedulers.disk)
                .subscribe({
                    // Ignore, nothing to do here!
                }, {
                    // This should never happen so lets throw the exception
                    Timber.e(it)
                    throw it
                })
    }
}

class LeakCanaryManager @Inject constructor() : AppManager {
    override fun init(application: Application) {
        if (!LeakCanary.isInAnalyzerProcess(application)) {
            LeakCanary.install(application)
        }
    }
}

class TimberManager @Inject constructor() : AppManager {
    override fun init(application: Application) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}