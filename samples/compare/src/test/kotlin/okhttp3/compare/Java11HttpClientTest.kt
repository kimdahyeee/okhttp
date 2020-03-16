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
import org.junit.Rule
import org.junit.Test
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect.NORMAL
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * Java 11's HTTP client is built-in.
 *
 * It attempts to upgrade calls to h2c, which is HTTP/2 over a plaintext connection.
 */
class Java11HttpClientTest {
  @JvmField @Rule val server1 = MockWebServer()
  @JvmField @Rule val server2 = MockWebServer()

  private val httpClient = HttpClient.newBuilder()
      .followRedirects(NORMAL)
      .build()

  @Test fun get() {
    server1.enqueue(MockResponse()
        .setBody("hello, Java HTTP Client"))

    val request = HttpRequest.newBuilder(server1.url("/").toUri())
        .header("Accept", "text/plain")
        .build()

    val response = httpClient.send(request, BodyHandlers.ofString())
    assertThat(response.statusCode()).isEqualTo(200)
    assertThat(response.body()).isEqualTo("hello, Java HTTP Client")

    val recorded = server1.takeRequest()
    assertThat(recorded.getHeader("Accept-Encoding")).isNull()
    assertThat(recorded.getHeader("Content-Length")).isEqualTo("0")
    assertThat(recorded.getHeader("User-Agent")).matches("Java-http-client/.*")
    assertThat(recorded.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded.getHeader("Connection")).isEqualTo("Upgrade, HTTP2-Settings")
    assertThat(recorded.getHeader("Upgrade")).isEqualTo("h2c")
    assertThat(recorded.getHeader("HTTP2-Settings")).isNotNull()
  }

  @Test fun redirect() {
    server1.enqueue(MockResponse()
        .setResponseCode(301)
        .addHeader("Location", server2.url("/")))

    server2.enqueue(MockResponse()
        .setBody("hello, Java HTTP Client"))

    val request = HttpRequest.newBuilder(server1.url("/").toUri())
        .header("Accept", "text/plain")
        .header("Secret", "peanutbutter")
        .header("Authorization", "peanutbutter")
        .build()

    val response = httpClient.send(request, BodyHandlers.ofString())
    assertThat(response.statusCode()).isEqualTo(200)
    assertThat(response.body()).isEqualTo("hello, Java HTTP Client")

    val recorded1 = server1.takeRequest()
    assertThat(recorded1.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded1.getHeader("Secret")).isEqualTo("peanutbutter")
    assertThat(recorded1.getHeader("Authorization")).isEqualTo("peanutbutter")

    val recorded2 = server2.takeRequest()
    assertThat(recorded2.getHeader("Accept")).isEqualTo("text/plain")
    assertThat(recorded2.getHeader("Secret")).isEqualTo("peanutbutter")
    assertThat(recorded2.getHeader("Authorization")).isEqualTo("peanutbutter") // Not stripped!
  }
}
