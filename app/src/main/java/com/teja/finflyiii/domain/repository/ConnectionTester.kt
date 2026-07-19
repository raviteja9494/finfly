/* Domain gateway contract for validating a Firefly server before persisting it. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result

/**
 * Tests whether [serverUrl] accepts [bearerToken].
 * Returns success for an authenticated Firefly response and a safe error otherwise.
 */
interface ConnectionTester {
    suspend fun test(serverUrl: String, bearerToken: String): Result<Unit>
}
