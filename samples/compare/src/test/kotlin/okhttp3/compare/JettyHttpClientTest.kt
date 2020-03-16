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

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.client.HttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Jetty's HTTP client.
 */
class JettyHttpClientTest {
  @JvmField @Rule val server1 = MockWebServer()
  @JvmField @Rule val server2 = MockWebServer()

  private val client = HttpClient()

  @Before fun setUp() {
    client.start()
  }

  @After fun tearDown() {
    client.stop()
  }

  @Test fun get() {
    server1.enqueue(MockResponse()
        .setBody("hello, Jetty HTTP Client"))

    val request = client.newRequest(server1.url("/").toUri())
        .header("Accept", "text/plain")
    val response = request.send()
    assertThat(response.status).isEqualTo(200)
    assertThat(response.contentAsString).isEqualTo("hello, Jetty HTTP Client")

    val recorded = server1.takeRequest()
    assertThat(recorded.getHeader("Accept-Encoding")).isEqualTo("gzip")
    assertThat(recorded.getHeader("User-Agent")).matches("Jetty/.*")
    assertThat(recorded.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded.getHeader("Connection")).isNull()
  }

  @Test fun redirect() {
    server1.enqueue(MockResponse()
        .setResponseCode(301)
        .addHeader("Location", server2.url("/")))

    server2.enqueue(MockResponse()
        .setBody("hello, Jetty HTTP Client"))

    val request = client.newRequest(server1.url("/").toUri())
        .header("Accept", "text/plain")
        .header("Secret", "peanutbutter")
        .header("Authorization", "peanutbutter")
    val response = request.send()
    assertThat(response.status).isEqualTo(200)
    assertThat(response.contentAsString).isEqualTo("hello, Jetty HTTP Client")

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
