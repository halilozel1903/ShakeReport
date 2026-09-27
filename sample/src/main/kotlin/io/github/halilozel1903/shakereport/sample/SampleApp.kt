package io.github.halilozel1903.shakereport.sample

import android.app.Application
import io.github.halilozel1903.shakereport.ShakeReport
import io.github.halilozel1903.shakereport.ShakeReportConfig
import io.github.halilozel1903.shakereport.core.ShakeConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // In a real app: if (BuildConfig.DEBUG || BuildConfig.FLAVOR == "beta") { … }
        ShakeReport.install(
            application = this,
            config = ShakeReportConfig(
                // Easier to trigger on emulators (Extended controls > Virtual sensors > Device pose).
                shake = ShakeConfig.Sensitive,
                sender = DemoSlackSender(),
                emailRecipients = listOf("qa@example.com"),
                subjectPrefix = "[Beta]",
                customData = { mapOf("User" to "demo-user-42", "Flavor" to "beta") },
            ),
        )
    }
}
