package io.github.halilozel1903.shakereport

import androidx.core.content.FileProvider

/**
 * ShakeReport's own [FileProvider], so its manifest entry never conflicts with an app's
 * `androidx.core.content.FileProvider`. Authority: `<applicationId>.shakereport.fileprovider`.
 */
internal class ShakeReportFileProvider : FileProvider()
