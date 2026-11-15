package com.adl.nocturna.text

import platform.Foundation.NSLocale

actual val testLocale: PlatformLocale = NSLocale(localeIdentifier = "en_GB")

actual val spanishTestLocale: PlatformLocale = NSLocale(localeIdentifier = "es_MX")
