/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val isCI = System.getenv("CI") != null ||  System.getenv("TEAMCITY_VERSION") != null
