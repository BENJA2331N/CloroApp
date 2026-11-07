package com.example.cloroapp.domain

/**
 * Validación centralizada y desacoplada de la interfaz gráfica (IE 2.1.2 e IE 2.2.1).
 * Cada método evalúa las reglas de negocio de un campo o formulario y devuelve mensajes
 * específicos por cada campo para alimentar la retroalimentación visual en Material 3.
 */
object ValidadorFormularios {

    private val REGEX_CORREO = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_CARACTERES_CLAVE = 6
    const val MIN_CARACTERES_ACCION = 10
    const val MAX_CARACTERES_ACCION = 250

    /** Devuelve un mensaje de error si el correo no es válido, o null si es correcto. */
    fun validarCorreo(correo: String): String? {
        val limpio = correo.trim()
        return when {
            limpio.isEmpty() -> "Ingresa tu correo electrónico institucional."
            !limpio.contains("@") -> "El correo debe incluir el símbolo '@'."
            !REGEX_CORREO.matches(limpio) -> "Formato de correo inválido (ej. operario1@cloroapp.cl)."
            else -> null
        }
    }

    /** Devuelve un mensaje de error si la contraseña no cumple el mínimo, o null si es válida. */
    fun validarContrasena(contrasena: String): String? = when {
        contrasena.isBlank() -> "Ingresa tu contraseña."
        contrasena.length < MIN_CARACTERES_CLAVE ->
            "La contraseña debe tener al menos $MIN_CARACTERES_CLAVE caracteres."
        else -> null
    }

    /** Devuelve un mensaje de error específico para la medición de cloro libre en ppm, o null si es válida. */
    fun validarValorPpm(valorTexto: String): String? {
        val limpio = valorTexto.trim()
        if (limpio.isEmpty()) return "Ingresa el valor medido en ppm (entre 0,00 y 10,00)."
        val numero = Formato.parsear(limpio)
            ?: return "Formato numérico inválido. Usa números y coma decimal (ej. 3,80)."
        return when {
            numero < 0.0 -> "El nivel de cloro no puede ser negativo."
            numero > 10.0 -> "El valor máximo permitido por el fotómetro es 10,00 ppm."
            else -> null
        }
    }

    /** Valida la descripción de una acción correctiva en terreno. */
    fun validarAccionCorrectiva(descripcion: String): String? {
        val limpio = descripcion.trim()
        return when {
            limpio.isEmpty() -> "Describe la acción correctiva realizada en el punto."
            limpio.length < MIN_CARACTERES_ACCION ->
                "Detalla mejor la acción realizada (mínimo $MIN_CARACTERES_ACCION caracteres, llevas ${limpio.length})."
            limpio.length > MAX_CARACTERES_ACCION ->
                "La descripción no puede superar los $MAX_CARACTERES_ACCION caracteres."
            else -> null
        }
    }

    data class ValidacionRangos(
        val errorCriticoMin: String? = null,
        val errorNormalMin: String? = null,
        val errorNormalMax: String? = null,
        val errorCriticoMax: String? = null,
        val errorGeneral: String? = null,
        val rangosValidos: RangosCloro? = null
    ) {
        val esValido: Boolean
            get() = rangosValidos != null &&
                errorCriticoMin == null &&
                errorNormalMin == null &&
                errorNormalMax == null &&
                errorCriticoMax == null &&
                errorGeneral == null
    }

    /** Valida individualmente los 4 umbrales de cloro libre y su coherencia de menor a mayor. */
    fun validarRangos(
        criticoMinTxt: String,
        normalMinTxt: String,
        normalMaxTxt: String,
        criticoMaxTxt: String
    ): ValidacionRangos {
        val cMin = Formato.parsear(criticoMinTxt)
        val nMin = Formato.parsear(normalMinTxt)
        val nMax = Formato.parsear(normalMaxTxt)
        val cMax = Formato.parsear(criticoMaxTxt)

        var errCMin = when {
            criticoMinTxt.isBlank() -> "Obligatorio"
            cMin == null -> "Número inválido"
            cMin <= 0.0 || cMin >= 10.0 -> "Debe estar entre 0,10 y 9,90 ppm"
            else -> null
        }
        var errNMin = when {
            normalMinTxt.isBlank() -> "Obligatorio"
            nMin == null -> "Número inválido"
            nMin <= 0.0 || nMin >= 10.0 -> "Debe estar entre 0,10 y 9,90 ppm"
            else -> null
        }
        var errNMax = when {
            normalMaxTxt.isBlank() -> "Obligatorio"
            nMax == null -> "Número inválido"
            nMax <= 0.0 || nMax >= 10.0 -> "Debe estar entre 0,10 y 9,90 ppm"
            else -> null
        }
        var errCMax = when {
            criticoMaxTxt.isBlank() -> "Obligatorio"
            cMax == null -> "Número inválido"
            cMax <= 0.0 || cMax > 10.0 -> "Debe estar entre 0,10 y 10,00 ppm"
            else -> null
        }

        if (cMin != null && nMin != null && cMin >= nMin && errNMin == null) {
            errNMin = "Debe ser mayor que el crítico mínimo (${Formato.decimal(cMin)})"
        }
        if (nMin != null && nMax != null && nMin >= nMax && errNMax == null) {
            errNMax = "Debe ser mayor que el normal mínimo (${Formato.decimal(nMin)})"
        }
        if (nMax != null && cMax != null && nMax >= cMax && errCMax == null) {
            errCMax = "Debe ser mayor que el normal máximo (${Formato.decimal(nMax)})"
        }

        val hayErroresCampo = listOf(errCMin, errNMin, errNMax, errCMax).any { it != null }
        if (hayErroresCampo || cMin == null || nMin == null || nMax == null || cMax == null) {
            return ValidacionRangos(
                errorCriticoMin = errCMin,
                errorNormalMin = errNMin,
                errorNormalMax = errNMax,
                errorCriticoMax = errCMax,
                errorGeneral = "Corrige los campos marcados en rojo antes de guardar."
            )
        }

        val propuestos = RangosCloro(cMin, nMin, nMax, cMax)
        return if (propuestos.esValido()) {
            ValidacionRangos(rangosValidos = propuestos)
        } else {
            ValidacionRangos(errorGeneral = "Los límites deben ordenarse estrictamente de menor a mayor.")
        }
    }
}

