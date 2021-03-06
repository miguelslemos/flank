package ftl.args.yml

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import ftl.args.ArgsHelper.getDefaultProjectId
import ftl.config.FtlConstants.useMock
import ftl.gc.GcToolResults
import ftl.util.Utils.assertNotEmpty

/**
 * Common Gcloud parameters shared between iOS and Android
 *
 * https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
 * https://cloud.google.com/sdk/gcloud/reference/alpha/firebase/test/ios/run
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class GcloudYmlParams(
        // TODO: make resultsBucket immutable
        @field:JsonProperty("results-bucket")
        var resultsBucket: String = "",

        @field:JsonProperty("record-video")
        val recordVideo: Boolean = true,

        val timeout: String = "15m",

        val async: Boolean = false,

        val project: String = getDefaultProjectId()
) {
    companion object : IYmlKeys {
        override val keys = listOf("results-bucket", "record-video", "timeout", "async", "project")
    }

    init {
        assertNotEmpty(project, "project is not set. Define GOOGLE_CLOUD_PROJECT or set project in flank.yml\n" +
                " See https://github.com/GoogleCloudPlatform/google-cloud-java#specifying-a-project-id")

        if (useMock) {
            resultsBucket = "mockBucket"
        } else {
            if (resultsBucket.isEmpty()) resultsBucket = GcToolResults.getDefaultBucket(project) ?: ""
            assertNotEmpty(resultsBucket, "results-bucket is not set")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class GcloudYml(
        val gcloud: GcloudYmlParams
) {
    companion object : IYmlMap {
        override val map = mapOf("gcloud" to GcloudYmlParams.keys)
    }
}
