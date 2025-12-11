package com.github.xepozz.php_dump.services

import com.github.xepozz.php_dump.stubs.token_object.TokensList

data class CustomDumpResult(
    var tokens: TokensList = TokensList(),
    var raw: String = "",
    var error: Throwable? = null,
)