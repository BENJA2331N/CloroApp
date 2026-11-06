package com.example.cloroapp.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cloroapp.CloroApplication
import com.example.cloroapp.ui.viewmodel.AlertaDetalleViewModel
import com.example.cloroapp.ui.viewmodel.AlertasViewModel
import com.example.cloroapp.ui.viewmodel.PanelViewModel
import com.example.cloroapp.ui.viewmodel.PuntoDetalleViewModel
import com.example.cloroapp.ui.viewmodel.PuntosViewModel
import com.example.cloroapp.ui.viewmodel.RegistroViewModel
import com.example.cloroapp.ui.viewmodel.SesionViewModel

/** Fábrica única de ViewModels: les entrega el repositorio y las preferencias de la aplicación. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SesionViewModel(cloroApplication().contenedor) }
        initializer { PuntosViewModel(cloroApplication().contenedor.repositorio) }
        initializer {
            val contenedor = cloroApplication().contenedor
            PuntoDetalleViewModel(contenedor.repositorio, contenedor.preferencias)
        }
        initializer {
            val contenedor = cloroApplication().contenedor
            RegistroViewModel(contenedor.repositorio, contenedor.preferencias)
        }
        initializer { AlertasViewModel(cloroApplication().contenedor.repositorio) }
        initializer { AlertaDetalleViewModel(cloroApplication().contenedor.repositorio) }
        initializer {
            val contenedor = cloroApplication().contenedor
            PanelViewModel(contenedor.repositorio, contenedor.preferencias)
        }
    }
}

fun CreationExtras.cloroApplication(): CloroApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CloroApplication
