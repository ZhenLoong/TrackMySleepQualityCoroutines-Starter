/*
 * Copyright 2019, The Android Open Source Project
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

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val nights = database.getAllNights()
    val nightsString = Transformations.map(nights) {
        nights -> formatNights(nights, application.resources)
    }

    private var tonight = MutableLiveData<SleepNight>()

    init {
        initializeTonight()
    }

    private suspend fun getTonightFromDatabase(): SleepNight?{
        return withContext(Dispatchers.IO){
            var night = database.getTonight()
            if(night?.endTimeMilli != night?.startTimeMilli){
                night = null
            }
            night
        }
    }

    private fun initializeTonight(){
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    fun onStartTracking(){
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    fun onStopTracking(){
        uiScope.launch{
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)

            _navigateToSleepQuality.value = oldNight
        }
    }

    fun onClear(){
        uiScope.launch(){
            clear()
            tonight.value = null
        }
    }

    fun doneNavigating(){
        _navigateToSleepQuality.value = null
    }

    private suspend fun clear(){
        withContext(Dispatchers.IO){
            database.clear()
        }
    }

    private suspend fun update(night:SleepNight){
        withContext(Dispatchers.IO){
            database.update(night)
        }
    }

    private suspend fun insert(night: SleepNight){
        withContext(Dispatchers.IO){
            database.insert(night)
        }
    }

    override fun onCleared(){
        super.onCleared()
        viewModelJob.cancel()
    }
}

