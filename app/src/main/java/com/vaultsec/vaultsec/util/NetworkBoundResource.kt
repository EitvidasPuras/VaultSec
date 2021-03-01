package com.vaultsec.vaultsec.util

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: suspend (ResultType) -> Boolean = { true },
    crossinline onFetchSuccess: () -> Unit = { },
    crossinline onFetchFailed: () -> Unit = { }
) = channelFlow {
    val data = query().first()
//    Log.e("data", "$data")

    if (shouldFetch(data)) {
        val loading = launch {
            query().collect { send(Resource.Loading(it)) }
        }

        try {
            delay(300)
            saveFetchResult(fetch())
            onFetchSuccess()
            loading.cancel()
            Log.e("onFetchSuccess", "")
            query().collect { send(Resource.Success(it)) }
        } catch (e: Exception) {
            Log.e("onFetchFailed", "")
            onFetchFailed()
            loading.cancel()
            query().collect { send(Resource.Error(type = ErrorTypes.GENERAL, data = it)) }
        }
    } else {
//        loading.cancel()
        query().collect { send(Resource.Success(it)) }
    }
}