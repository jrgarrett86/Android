/*
 * Copyright (c) 2018 DuckDuckGo
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

package com.duckduckgo.app.httpsupgrade.api

import com.duckduckgo.app.global.store.BinaryDataStore
import com.duckduckgo.app.httpsupgrade.BloomFilter
import com.duckduckgo.app.httpsupgrade.db.HttpsBloomFilterSpecDao
import com.duckduckgo.app.httpsupgrade.model.HttpsBloomFilterSpec.Companion.HTTPS_BINARY_FILE
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

interface HttpsBloomFilterFactory {
    fun create(): BloomFilter?
}

class OptionalWrapper<out T>(val value: T?)

class HttpsBloomFilterFactoryImpl @Inject constructor(private val dao: HttpsBloomFilterSpecDao, private val binaryDataStore: BinaryDataStore) :
    HttpsBloomFilterFactory {

    override fun create(): BloomFilter? {

        val specificationWrapper = Observable.just(dao)
            .observeOn(Schedulers.io())
            .map {
                OptionalWrapper(dao.get())
            }
            .blockingSingle()

        val specification = specificationWrapper.value
        val dataPath = binaryDataStore.dataFilePath(HTTPS_BINARY_FILE)

        if (dataPath == null || specification == null) {
            Timber.d("Https update data not found")
            return null
        }

        Timber.d("Found https data at $dataPath, building filter")
        return BloomFilter(dataPath, specification.totalEntries)

    }
}