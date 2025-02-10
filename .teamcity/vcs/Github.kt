/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package vcs

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Github : GitVcsRoot({
    name = "Compose Hot Reload"
    url = "git@github.com:JetBrains/compose-hot-reload"
    branch = "refs/heads/master"
    branchSpec = "refs/heads/*"
    authMethod = uploadedKey {
        uploadedKey = "compose-hot-reload-deploy-id_rsa"
    }
})
