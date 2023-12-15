package com.example.fitsync.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitsync.steps.FirestoreStepRepository
import com.example.fitsync.steps.StepCounterRepository
import com.example.fitsync.steps.StepCounterService
import com.example.fitsync.steps.models.WeekSteps
import com.example.fitsync.utils.getWeekId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepCounterRepository: StepCounterRepository,
    private val firestoreStepRepository: FirestoreStepRepository,
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    val stepsFlow: StateFlow<Int> = stepCounterRepository.stepsFlow

    private val _currentWeekStats = MutableStateFlow<List<DayStats>>(emptyList())
    val currentWeekStats: StateFlow<List<DayStats>> = _currentWeekStats

    // TODO get goal from user
    private val goal = 1000

    init { fetchCurrentWeekSteps() }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCurrentWeekSteps() {
        viewModelScope.launch {
            val weekId = LocalDate.now().getWeekId()
            val weekSteps = firestoreStepRepository.getOrCreateWeekData(weekId)
            _currentWeekStats.value = weekSteps.days.entries.map { (dayOfWeek, daySteps) ->
                DayStats(
                    dayOfWeek = dayOfWeek,
                    progress = daySteps.steps.toFloat() / goal
                )
            }
        }
    }

    fun toggleStepCounterService(context: Context, isServiceRunning: Boolean): Boolean {
        val serviceIntent = Intent(context, StepCounterService::class.java)
        return if (isServiceRunning) {
            // Stop the service
            context.stopService(serviceIntent)
            false
        } else {
            // Start the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            true
        }
    }
}