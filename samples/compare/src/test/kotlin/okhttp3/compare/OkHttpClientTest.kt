/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.compare

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * OkHttp client.
 */
class OkHttpClientTest {
  @JvmField @Rule val server1 = MockWebServer()
  @JvmField @Rule val server2 = MockWebServer()

  private val client = OkHttpClient()

  @Test fun get() {
    server1.enqueue(MockResponse()
        .setBody("hello, Jetty HTTP Client"))

    val request = Request.Builder()
        .url(server1.url("/"))
        .header("Accept", "text/plain")
        .build()
    val response = client.newCall(request).execute()
    assertThat(response.code).isEqualTo(200)
    assertThat(response.body!!.string()).isEqualTo("hello, Jetty HTTP Client")

    val recorded = server1.takeRequest()
    assertThat(recorded.getHeader("Connection")).isEqualTo("Keep-Alive")
    assertThat(recorded.getHeader("Accept-Encoding")).isEqualTo("gzip")
    assertThat(recorded.getHeader("User-Agent")).matches("okhttp/.*")
    assertThat(recorded.getHeader("Accept")).isEqualTo("text/plain")
  }

  @Test fun redirect() {
    server1.enqueue(MockResponse()
        .setResponseCode(301)
        .addHeader("Location", server2.url("/")))

    server2.enqueue(MockResponse()
        .setBody("hello, Jetty HTTP Client"))

    val request = Request.Builder()
        .url(server1.url("/"))
        .header("Accept", "text/plain")
        .header("Secret", "peanutbutter")
        .header("Authorization", "peanutbutter")
        .build()
    val response = client.newCall(request).execute()
    assertThat(response.code).isEqualTo(200)
    assertThat(response.body!!.string()).isEqualTo("hello, Jetty HTTP Client")

    val recorded1 = server1.takeRequest()
    assertThat(recorded1.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded1.getHeader("Secret")).isEqualTo("peanutbutter")
    assertThat(recorded1.getHeader("Authorization")).isEqualTo("peanutbutter")

    val recorded2 = server2.takeRequest()
    assertThat(recorded2.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded2.getHeader("Secret")).isEqualTo("peanutbutter")
    assertThat(recorded2.getHeader("Authorization")).isNull()
  }
}
