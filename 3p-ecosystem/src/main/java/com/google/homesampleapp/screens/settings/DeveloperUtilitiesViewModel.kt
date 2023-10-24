/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.homesampleapp.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.homesampleapp.data.DevicesRepository
import com.google.homesampleapp.data.DevicesStateRepository
import com.google.homesampleapp.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class DeveloperUtilitiesViewModel
@Inject
constructor(
    private val devicesRepository: DevicesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val devicesStateRepository: DevicesStateRepository,
) : ViewModel() {

  fun printRepositories() {
    viewModelScope.launch {
      val divider = "-".repeat(20)
      val userPreferences = userPreferencesRepository.getData()
      Timber.d(
          "UserPreferences Repository\n${divider} [UserPreferences Repository] ${divider}\n${userPreferences}\n${divider} End of [UserPreferences Repository] $divider")
      val devices = devicesRepository.getAllDevices()
      Timber.d(
          "Devices Repository\n${divider} [Devices Repository] ${divider}\n${devices}\n${divider} End of [Devices Repository] $divider")
      val devicesState = devicesStateRepository.getAllDevicesState()
      Timber.d(
          "DevicesState Repository\n${divider} [DevicesState Repository] ${divider}\n${devicesState}\n${divider} End of [DevicesState Repository] $divider")
    }
  }
}
