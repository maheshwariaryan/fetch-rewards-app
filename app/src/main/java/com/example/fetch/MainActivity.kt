package com.example.fetch

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

private const val API_URL = "https://fetch-hiring.s3.amazonaws.com/hiring.json"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FetchItemsScreen(applicationContext)
                }
            }
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            return hasInternet && isValidated
        }
    } else {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected
        return isConnected
    }
    return false
}

@Composable
fun FetchItemsScreen(appContext: Context) {
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Loading...") }
    var job: Job? by remember { mutableStateOf(null) }

    fun startFetching() {
        isLoading = true
        error = null
        statusMessage = "Loading..."

        Thread {
            try {
                val uiHandler = Handler(Looper.getMainLooper())

                if (!isNetworkAvailable(appContext)) {
                    uiHandler.post {
                        isLoading = false
                        error = "No internet connection available"
                        statusMessage = "Please check your network settings"
                    }
                    return@Thread
                }

                val testConnection = URL("https://www.google.com").openConnection() as HttpURLConnection
                testConnection.connectTimeout = 5000
                testConnection.readTimeout = 5000

                try {
                    testConnection.connect()
                    testConnection.disconnect()
                } catch (e: Exception) {
                    uiHandler.post {
                        isLoading = false
                        error = "Connection error"
                        statusMessage = "Please check your network settings"
                    }
                    return@Thread
                }

                val apiConnection = URL(API_URL).openConnection() as HttpURLConnection
                apiConnection.connectTimeout = 10000
                apiConnection.readTimeout = 15000
                apiConnection.setRequestProperty("Accept", "application/json")

                try {
                    apiConnection.connect()
                    val apiResponseCode = apiConnection.responseCode

                    if (apiResponseCode == 200) {
                        val inputStream = apiConnection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val content = StringBuilder()
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            content.append(line)
                        }
                        reader.close()
                        inputStream.close()

                        val jsonData = content.toString()
                        val jsonArray = JSONArray(jsonData)
                        val parsedItems = mutableListOf<Item>()

                        for (i in 0 until jsonArray.length()) {
                            try {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val id = jsonObject.getInt("id")
                                val listId = jsonObject.getInt("listId")
                                val name = if (jsonObject.has("name") && !jsonObject.isNull("name"))
                                    jsonObject.getString("name")
                                else
                                    null

                                parsedItems.add(Item(id, listId, name))
                            } catch (e: Exception) {
                                continue
                            }
                        }

                        val filteredItems = parsedItems.filter { !it.name.isNullOrBlank() }

                        val sortedItems = filteredItems.sortedWith(
                            compareBy<Item> { it.listId }
                                .thenBy {
                                    val regex = "Item (\\d+)".toRegex()
                                    val matchResult = regex.find(it.name ?: "")
                                    val numericPart = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
                                    numericPart
                                }
                        )

                        uiHandler.post {
                            items = sortedItems
                            isLoading = false
                            statusMessage = "${sortedItems.size} items loaded"
                        }
                    } else {
                        uiHandler.post {
                            isLoading = false
                            error = "Server returned error code: $apiResponseCode"
                            statusMessage = "Error loading data"
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    uiHandler.post {
                        isLoading = false
                        error = "Connection timed out"
                        statusMessage = "Request took too long"
                    }
                } catch (e: Exception) {
                    uiHandler.post {
                        isLoading = false
                        error = "Connection error"
                        statusMessage = "Error loading data"
                    }
                } finally {
                    apiConnection.disconnect()
                }
            } catch (e: Exception) {
                val uiHandler = Handler(Looper.getMainLooper())
                uiHandler.post {
                    isLoading = false
                    error = "Unexpected error"
                    statusMessage = "Error loading data"
                }
            }
        }.start()
    }

    LaunchedEffect(true) {
        startFetching()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Loading data...", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            job?.cancel()
                            startFetching()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = statusMessage, style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { startFetching() }) {
                        Text("Retry")
                    }
                }
            }
            items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No items to display",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { startFetching() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                val groupedItems = items.groupBy { it.listId }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { startFetching() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Refresh")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        groupedItems.forEach { (listId, itemsInGroup) ->
                            item {
                                Text(
                                    text = "List ID: $listId (${itemsInGroup.size} items)",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(itemsInGroup) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = item.name ?: "")
                                        Text(text = "ID: ${item.id}")
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}