package vn.edu.vlu.milestone2.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ScanWorkScheduler(context: Context) {

    private val workManager by lazy { WorkManager.getInstance(context.applicationContext) }

    fun enqueue(scanId: Long) {
        val request = OneTimeWorkRequestBuilder<LabelAnalysisWorker>()
            .setInputData(workDataOf(LabelAnalysisWorker.KEY_SCAN_ID to scanId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork("$TAG-$scanId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(scanId: Long) {
        workManager.cancelUniqueWork("$TAG-$scanId")
    }

    private companion object {
        const val TAG = "label-analysis"
        const val BACKOFF_SECONDS = 30L
    }
}
