package ftl.run

import com.google.api.services.testing.model.TestMatrix
import ftl.args.AndroidArgs
import ftl.config.FtlConstants
import ftl.gc.GcAndroidMatrix
import ftl.gc.GcAndroidTestMatrix
import ftl.gc.GcStorage
import ftl.json.MatrixMap
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

object AndroidTestRunner {

    suspend fun runTests(androidArgs: AndroidArgs): MatrixMap {
        val (stopwatch, runGcsPath) = GenericTestRunner.beforeRunTests()

        // GcAndroidMatrix => GcAndroidTestMatrix
        // GcAndroidTestMatrix.execute() 3x retry => matrix id (string)
        val androidDeviceList = GcAndroidMatrix.build(androidArgs.devices)

        val apks = resolveApks(androidArgs, runGcsPath)
        val jobs = arrayListOf<Deferred<TestMatrix>>()
        val runCount = androidArgs.testRuns
        val repeatShard = androidArgs.testShardChunks.size
        val testsPerVm = androidArgs.testShardChunks.first().size
        val testsTotal = androidArgs.testShardChunks.sumBy { it.size }

        println("  Running ${runCount}x using $repeatShard VMs per run. ${runCount * repeatShard} total VMs")
        println("  $testsPerVm tests per VM. $testsTotal total tests per run")
        repeat(runCount) {
            repeat(repeatShard) { testShardsIndex ->
                jobs += async {
                    GcAndroidTestMatrix.build(
                            appApkGcsPath = apks.first,
                            testApkGcsPath = apks.second,
                            runGcsPath = runGcsPath,
                            androidDeviceList = androidDeviceList,
                            testShardsIndex = testShardsIndex,
                            config = androidArgs).execute()
                }
            }
        }

        return GenericTestRunner.afterRunTests(jobs, runGcsPath, stopwatch, androidArgs)
    }

    /**
     * Upload APKs if the path given is local
     *
     * @return Pair(gcs uri for app apk, gcs uri for test apk)
     */
    private suspend fun resolveApks(config: AndroidArgs, runGcsPath: String): Pair<String, String> {
        val gcsBucket = config.resultsBucket

        val appApkGcsPath = async {
            if (!config.appApk.startsWith(FtlConstants.GCS_PREFIX)) {
                GcStorage.uploadAppApk(config, gcsBucket, runGcsPath)
            } else {
                config.appApk
            }
        }

        val testApkGcsPath = async {
            // Skip download case for testApk - YamlConfig downloads it when it does validation
            if (!config.testApk.startsWith(FtlConstants.GCS_PREFIX)) {
                GcStorage.uploadTestApk(config, gcsBucket, runGcsPath)
            } else {
                config.testApk
            }
        }

        return Pair(appApkGcsPath.await(), testApkGcsPath.await())
    }
}
