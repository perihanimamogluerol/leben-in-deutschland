package com.nextdaymobilestudio.lebenindeutschland

import android.app.Application
import android.content.Context
import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.FirebaseContentSynchronizerFactory
import com.nextdaymobilestudio.lebenindeutschland.core.database.LocalContentDatabase
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.RoomLocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.OnboardingPreferencesRepository
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.createOnboardingPreferencesRepository

class LebenInDeutschlandApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}

class AppContainer(
    context: Context,
) {
    val onboardingPreferencesRepository: OnboardingPreferencesRepository =
        context.createOnboardingPreferencesRepository()

    private val localContentDatabase: LocalContentDatabase =
        LocalContentDatabase.create(context)

    val localContentRepository: LocalContentRepository =
        RoomLocalContentRepository(localContentDatabase)

    val contentSynchronizerProvider: () -> ContentSynchronizer = {
        FirebaseContentSynchronizerFactory.create(
            context = context,
            localContentRepository = localContentRepository,
        )
    }
}
