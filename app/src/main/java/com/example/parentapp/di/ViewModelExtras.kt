package com.example.parentapp.di

import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.parentapp.ParentsApp

/**
 * Resolves the app's [AppContainer] inside a `viewModelFactory { initializer { ... } }` block:
 *
 * ```
 * val Factory = viewModelFactory {
 *     initializer { ChildListViewModel(appContainer.childRepository) }
 * }
 * ```
 */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as ParentsApp).container
