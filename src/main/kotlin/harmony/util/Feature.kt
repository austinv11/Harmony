package harmony.util

/**
 * This is a utility representing an optional, toggleable feature of Harmony.
 */
class Feature<O> {

    /**
     * Whether this feature is enabled.
     */
    val isEnabled: Boolean

    private val _value: O?

    /**
     * Gets the feature info if available.
     */
    val value: O
        get() = _value ?: throw RuntimeException("This feature is not enabled!")

    private constructor(value: O?) {
        this.isEnabled = value != null
        this._value = value
    }

    /**
     * DSL to run code if the feature is enabled.
     *
     * @param callable The code to invoke when enabled.
     * @return Any value that is passed from the callable.
     */
    infix fun <T> ifEnabled(callable: (value: O)->T): T? {
        return if (isEnabled)
            callable(value)
        else
            null
    }

    companion object {

        /**
         * Disables a feature.
         *
         * @return A disabled feature.
         */
        @JvmStatic
        fun <T> disable(): Feature<T> = Feature(null)

        /**
         * Enables a feature with a given value.
         *
         * @param value A non-null value.
         * @return The enabled feature.
         */
        @JvmStatic
        fun <T> enable(value: T) = Feature(value)
     }
}