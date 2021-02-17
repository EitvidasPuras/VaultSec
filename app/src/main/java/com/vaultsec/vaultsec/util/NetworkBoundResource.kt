package com.vaultsec.vaultsec.util

import android.util.Log
import com.vaultsec.vaultsec.repository.ShouldFetch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    Log.e("data", "$data")


    if (shouldFetch(data)) {
        val loading = launch {
            query().collect { send(Holder.Loading(it)) }
        }

        try {
            delay(500)
            saveFetchResult(fetch())
            onFetchSuccess()
            loading.cancel()
            Log.e("onFetchSuccess", "")
            query().collect { send(Holder.Success(it)) }
        } catch (e: Exception) {
            onFetchFailed()
            Log.e("onFetchFailed", "")
            loading.cancel()
            query().collect { send(Holder.Error(e, it)) }
        }
    } else {
//        loading.cancel()
        query().collect { send(Holder.Success(it)) }
    }
}