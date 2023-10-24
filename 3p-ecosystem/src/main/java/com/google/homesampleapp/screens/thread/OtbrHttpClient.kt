package com.google.homesampleapp.screens.thread

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.threadnetwork.ThreadNetworkCredentials
import com.google.common.io.BaseEncoding
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * OtbrHttp object has some useful methods, enums and properties for getting/setting information to
 * OpenThread Border Routers using the HTTP REST API:
 * https://github.com/openthread/ot-br-posix/blob/main/src/rest/openapi.yaml
 */
object OtbrHttpClient {
  enum class Verbs {
    GET,
    POST,
    PUT,
    DELETE
  }

  /** what is generally an OK response */
  val okResponses =
      setOf(
          HttpURLConnection.HTTP_OK,
          HttpURLConnection.HTTP_CREATED,
          HttpURLConnection.HTTP_ACCEPTED,
          HttpURLConnection.HTTP_NOT_AUTHORITATIVE,
          HttpURLConnection.HTTP_NO_CONTENT,
          HttpURLConnection.HTTP_RESET,
          HttpURLConnection.HTTP_PARTIAL)

  /**
   * Creates credentials in the format used by the OTBR HTTP server. See its documentation in
   * https://github.com/openthread/ot-br-posix/blob/main/src/rest/openapi.yaml#L215
   */
  fun createJsonCredentialsObject(newCredentials: ThreadNetworkCredentials): JSONObject {
    val jsonTimestamp = JSONObject()
    jsonTimestamp.put("Seconds", System.currentTimeMillis() / 1000)
    jsonTimestamp.put("Ticks", 0)
    jsonTimestamp.put("Authoritative", false)

    val jsonQuery = JSONObject()
    jsonQuery.put(
        "ActiveDataset", BaseEncoding.base16().encode(newCredentials.activeOperationalDataset))
    jsonQuery.put("PendingTimestamp", jsonTimestamp)
    // delay of committing the pending set into active set: 10000ms
    jsonQuery.put("Delay", 10000)

    Timber.d(jsonQuery.toString())

    return jsonQuery
  }

  /**
   * Suspend function that generically creates an HTTP request and gives back its response. This was
   * created mostly for interacting with the OTBR HTTP, but it is generic enough for general use.
   *
   * When using a GET for a credential, you may either use [acceptMimeType] == "text/plain", which
   * will return the credentials in hex/base16 TLV format, or [acceptMimeType] ==
   * "application/json", which will return the credentials in JSON format. See the OpenThread HTTP
   * Rest API for more information (link above)
   */
  suspend fun createJsonHttpRequest(
      url: URL,
      activity: FragmentActivity,
      verb: Verbs,
      postPayload: String = "",
      contentTypeMimeType: String = "application/json",
      acceptMimeType: String = "application/json",
  ): Pair<HttpURLConnection, String> {

    // Use IO Dispatcher so it doesn't block the UI thread
    return withContext(Dispatchers.IO) {
      // typecasting the connection exposes additional methods and properties
      val urlConnection = url.openConnection() as HttpURLConnection
      var inputStream: InputStream? = null
      var content = ""

      urlConnection.connectTimeout = 1000 /* ms */
      urlConnection.setRequestProperty("Accept", acceptMimeType)
      urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0")

      urlConnection.requestMethod =
          when (verb) {
            Verbs.GET -> "GET"
            Verbs.POST -> "POST"
            Verbs.DELETE -> "DELETE"
            Verbs.PUT -> "PUT"
          }

      when (verb) {
        Verbs.POST,
        Verbs.PUT -> {
          urlConnection.setRequestProperty("Content-Type", contentTypeMimeType)
          urlConnection.doOutput = true
          try {
            val outputStream = urlConnection.outputStream
            postPayload?.let { outputStream.write(postPayload.toString().toByteArray()) }
            outputStream.flush()
            outputStream.close()

            val response = urlConnection.responseCode
            Timber.d("$verb response: $response")
            inputStream = BufferedInputStream(urlConnection.inputStream)
            content = String(inputStream.readBytes(), StandardCharsets.UTF_8)
          } catch (e: Exception) {
            Timber.e("Error loading page $e")
          } finally {
            inputStream?.let { inputStream!!.close() }
          }
        }
        Verbs.GET,
        Verbs.DELETE -> {
          try {
            inputStream = BufferedInputStream(urlConnection.inputStream)
            content = String(inputStream.readBytes(), StandardCharsets.UTF_8)
            val response = urlConnection.responseCode
            Timber.d("$verb response: $response")
          } catch (e: Exception) {
            Timber.e("Error loading page $e")
          } finally {
            inputStream?.let { inputStream.close() }
          }
        }
        else -> {
          Timber.e("HTTP verb not supported")
        }
      }

      Pair(urlConnection, content)
    }
  }
}
