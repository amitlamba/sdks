package com.userndot.sdk.android

import android.util.Log

class Logger {

    private lateinit var debugLevel: UserNDotAPI.LogLevel

    private constructor(debugLevel: UserNDotAPI.LogLevel = UserNDotAPI.LogLevel.OFF) {
        this.debugLevel = debugLevel
    }

    companion object {

        fun getInstance(debugLevel: UserNDotAPI.LogLevel = UserNDotAPI.LogLevel.OFF): Logger {
            return Logger(debugLevel)
        }

        private fun getStaticDebugLevel(): Int {
            return UserNDotAPI.debugLevel.intValue
        }

        /**
         * Logs to Debug if the debug level is greater than 1.
         */
        fun d(message: String) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
                Log.d(Constants.USERNDOT_LOG_TAG, message)
            }
        }

        fun d(suffix: String, message: String) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
                Log.d(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
            }
        }

        fun d(suffix: String, message: String, t: Throwable) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
                Log.d(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
            }
        }

        fun d(message: String, t: Throwable) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
                Log.d(Constants.USERNDOT_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Verbose if the debug level is greater than 2.
         */
        fun v(message: String) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
                Log.v(Constants.USERNDOT_LOG_TAG, message)
            }
        }

        fun v(suffix: String, message: String) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
                Log.v(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
            }
        }

        fun v(suffix: String, message: String, t: Throwable) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
                Log.v(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
            }
        }

        fun v(message: String, t: Throwable) {
            if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
                Log.v(Constants.USERNDOT_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Info if the debug level is greater than or equal to 1.
         */
        fun i(message: String) {
            if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
                Log.i(Constants.USERNDOT_LOG_TAG, message)
            }
        }

        fun i(suffix: String, message: String) {
            if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
                Log.i(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
            }
        }

        fun i(suffix: String, message: String, t: Throwable) {
            if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
                Log.i(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
            }
        }

        fun i(message: String, t: Throwable) {
            if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
                Log.i(Constants.USERNDOT_LOG_TAG, message, t)
            }
        }
    }


    fun debug(message: String) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
            Log.d(Constants.USERNDOT_LOG_TAG, message)
        }
    }

    fun debug(suffix: String, message: String) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
            Log.d(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
        }
    }

    fun debug(suffix: String, message: String, t: Throwable) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
            Log.d(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun debug(message: String, t: Throwable) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.INFO.intValue) {
            Log.d(Constants.USERNDOT_LOG_TAG, message, t)
        }
    }


    fun verbose(message: String) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
            Log.v(Constants.USERNDOT_LOG_TAG, message)
        }
    }

    fun verbose(suffix: String, message: String) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
            Log.v(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
        }
    }

    fun verbose(suffix: String, message: String, t: Throwable) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
            Log.v(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun verbose(message: String, t: Throwable) {
        if (getStaticDebugLevel() > UserNDotAPI.LogLevel.DEBUG.intValue) {
            Log.v(Constants.USERNDOT_LOG_TAG, message, t)
        }
    }

    fun info(message: String) {
        if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
            Log.i(Constants.USERNDOT_LOG_TAG, message)
        }
    }

    fun info(suffix: String, message: String) {
        if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
            Log.i(Constants.USERNDOT_LOG_TAG + ":" + suffix, message)
        }
    }

    fun info(suffix: String, message: String, t: Throwable) {
        if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
            Log.i(Constants.USERNDOT_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun info(message: String, t: Throwable) {
        if (getStaticDebugLevel() >= UserNDotAPI.LogLevel.INFO.intValue) {
            Log.i(Constants.USERNDOT_LOG_TAG, message, t)
        }
    }
}