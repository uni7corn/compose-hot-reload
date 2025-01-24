@file:OptIn(ExperimentalBCVApi::class)

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi

plugins {
    org.jetbrains.kotlinx.`binary-compatibility-validator`
}

extensions.configure<ApiValidationExtension> {
    klib { enabled = true }
}
