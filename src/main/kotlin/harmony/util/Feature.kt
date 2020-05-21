package harmony.util

class Feature<O> {

    val isEnabled: Boolean
    private val _value: O?
    val value: O
        get() = _value ?: throw RuntimeException("This feature is not enabled!")

    private constructor(value: O?) {
        this.isEnabled = value != null
        this._value = value
    }

    infix fun <T> ifEnabled(callable: (value: O)->T): T? {
        return if (isEnabled)
            callable(value)
        else
            null
    }

    companion object {

        @JvmStatic
        fun <T> disable(): Feature<T> = Feature(null)

        @JvmStatic
        fun <T> enable(value: T) = Feature(value)
     }
}