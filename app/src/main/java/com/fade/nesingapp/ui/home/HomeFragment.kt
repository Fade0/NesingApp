package com.fade.nesingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fade.nesingapp.databinding.FragmentHomeBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val apiKey = "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"

    // Master prompt loaded from assets
    private lateinit var masterPrompt: String

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Load master prompt from assets
        masterPrompt = readMasterPromptFromAssets()

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        // --- New logic for sending text to API and displaying response ---
        binding.sendButton.setOnClickListener {
            val userInput = binding.inputEditText.text.toString()
            if (userInput.isBlank()) {
                binding.responseTextView.text = "Please enter some text."
                return@setOnClickListener
            }
            binding.responseTextView.text = "Sending..."
            // Use coroutine to perform network request
            // (Requires androidx.lifecycle:lifecycle-runtime-ktx in build.gradle)
            viewLifecycleOwner.lifecycleScope.launch {
                val response = sendTextToApi(masterPrompt, userInput)
                binding.responseTextView.text = response
            }
        }
        return root
    }

    // Helper function to send text to OpenAI API
    private suspend fun sendTextToApi(masterPrompt: String, userInput: String): String {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.openai.com/v1/chat/completions")
                val conn = url.openConnection() as java.net.HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.doOutput = true


                android.util.Log.d("OpenAI", "apiKey: " + apiKey.take(4) + "..." + apiKey.takeLast(4))
                android.util.Log.d("OpenAI", "Authorization header: Bearer " + apiKey.take(4) + "..." + apiKey.takeLast(4))

                val messagesJson = """
                    [
                        {"role": "system", "content": "${masterPrompt.replace("\"", "\\\"")}"},
                        {"role": "user", "content": "${userInput.replace("\"", "\\\"")}"}
                    ]
                """.trimIndent()
                val jsonBody = """
                    {
                        "model": "gpt-3.5-turbo",
                        "messages": $messagesJson
                    }
                """.trimIndent()
                conn.outputStream.use { it.write(jsonBody.toByteArray()) }
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                // Extract the assistant's reply from the response JSON
                // (Very basic extraction, for production use a proper JSON parser)
                val contentRegex = """"content"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
                val match = contentRegex.find(response)
                match?.groups?.get(1)?.value?.replace("\\n", "\n") ?: "No response from OpenAI."
            } catch (e: Exception) {
                // Try to get error stream from the connection if available
                val errorMsg = StringBuilder()
                errorMsg.append("Error: ${e.localizedMessage}\n")
                if (e is java.io.IOException) {
                    try {
                        val url = java.net.URL("https://api.openai.com/v1/chat/completions")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        errorMsg.append("HTTP response code: ${conn.responseCode}\n")
                        val errorStream = conn.errorStream
                        if (errorStream != null) {
                            val errorBody = errorStream.bufferedReader().readText()
                            errorMsg.append("Error body: $errorBody\n")
                        }
                        conn.disconnect()
                    } catch (_: Exception) {}
                }
                errorMsg.toString()
            }
        }
    }

    // Helper to read master prompt from assets/master_prompt.txt
    private fun readMasterPromptFromAssets(): String {
        return try {
            requireContext().assets.open("master_prompt.txt").bufferedReader().use { it.readText() }.trim()
        } catch (e: Exception) {
            "You are a helpful assistant. Answer concisely and clearly."
        }
    }

    // Function to "ping" a website by sending a simple HTTP GET request
    // Returns a string with the result and response time
    private suspend fun pingWebsite(urlString: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val responseCode = conn.responseCode
                val endTime = System.currentTimeMillis()
                conn.disconnect()
                if (responseCode in 200..399) {
                    "Ping successful! Response code: $responseCode, time: ${endTime - startTime} ms"
                } else {
                    "Ping failed. Response code: $responseCode, time: ${endTime - startTime} ms"
                }
            } catch (e: Exception) {
                "Ping failed: ${e.localizedMessage}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
