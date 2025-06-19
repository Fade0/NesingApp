package com.fade.nesingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fade.nesingapp.databinding.FragmentDashboardBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Button

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val pingGoogleButton: Button = binding.buttonPingGoogle
        val pingAmazonButton: Button = binding.buttonPingAmazon
        val pingMicrosoftButton: Button = binding.buttonPingMicrosoft
        val pingFacebookButton: Button = binding.buttonPingFacebook
        val pingAllegroButton: Button = binding.buttonPingAllegro
        val pingWpButton: Button = binding.buttonPingWp
        val pingCustomButton: Button = binding.buttonPingCustom
        val customUrlEditText = binding.edittextCustomUrl

        val pingResultTextView: TextView = binding.textPingResult

        fun setupPingButton(button: Button, url: String) {
            button.setOnClickListener {
                pingResultTextView.text = ""
                lifecycleScope.launch {
                    val times = pingWebsiteXTimes(url, 4)
                    pingResultTextView.text = times.joinToString(separator = "\n")
                }
            }
        }

        setupPingButton(pingGoogleButton, "https://google.com")
        setupPingButton(pingAmazonButton, "https://amazon.com")
        setupPingButton(pingMicrosoftButton, "https://microsoft.com")
        setupPingButton(pingFacebookButton, "https://facebook.com")
        setupPingButton(pingAllegroButton, "https://allegro.pl")
        setupPingButton(pingWpButton, "https://wp.pl")

        pingCustomButton.setOnClickListener {
            val url = customUrlEditText.text.toString()
            if (url.isBlank()) {
                pingResultTextView.text = "Please enter a URL."
                return@setOnClickListener
            }
            pingResultTextView.text = ""
            lifecycleScope.launch {
                val times = pingWebsiteXTimes(url, 4)
                pingResultTextView.text = times.joinToString(separator = "\n")
            }
        }

        // Port scanning UI elements
        val portscanGoogleButton: Button = binding.buttonPortscanGoogle
        val portscanAmazonButton: Button = binding.buttonPortscanAmazon
        val portscanMicrosoftButton: Button = binding.buttonPortscanMicrosoft
        val portscanFacebookButton: Button = binding.buttonPortscanFacebook
        val portscanAllegroButton: Button = binding.buttonPortscanAllegro
        val portscanWpButton: Button = binding.buttonPortscanWp
        val portscanCustomButton: Button = binding.buttonPortscanCustom
        val portscanCustomUrlEditText = binding.edittextPortscanCustomUrl
        val portscanCustomNameEditText = binding.edittextPortscanCustomName
        val portscanResultTextView: TextView = binding.textPortscanResult

        fun setupPortscanButton(button: Button, url: String) {
            button.setOnClickListener {
                portscanResultTextView.text = ""
                lifecycleScope.launch {
                    val openPorts = portScanHost(url, 100, 120)
                    android.util.Log.d("PortScan", "Open ports for $url: $openPorts")
                    portscanResultTextView.text = if (openPorts.isEmpty()) {
                        "No open ports found."
                    } else {
                        "Open ports:\n" + openPorts.joinToString(separator = "\n")
                    }
                }
            }
        }

        setupPortscanButton(portscanGoogleButton, "google.com")
        setupPortscanButton(portscanAmazonButton, "amazon.com")
        setupPortscanButton(portscanMicrosoftButton, "microsoft.com")
        setupPortscanButton(portscanFacebookButton, "facebook.com")
        setupPortscanButton(portscanAllegroButton, "allegro.pl")
        setupPortscanButton(portscanWpButton, "wp.pl")

        portscanCustomButton.setOnClickListener {
            val url = portscanCustomUrlEditText.text.toString()
            val name = portscanCustomNameEditText.text.toString()
            if (url.isBlank()) {
                portscanResultTextView.text = "Please enter a URL or IP."
                return@setOnClickListener
            }
            portscanResultTextView.text = ""
            lifecycleScope.launch {
                val openPorts = portScanHost(url, 1, 1024)
                android.util.Log.d("PortScan", "Open ports for $url ($name): $openPorts")
                val displayName = if (name.isBlank()) url else name
                portscanResultTextView.text = if (openPorts.isEmpty()) {
                    "$displayName: No open ports found."
                } else {
                    "$displayName: Open ports:\n" + openPorts.joinToString(separator = "\n")
                }
            }
        }

        // Add hint log for port scan custom name field
        android.util.Log.d("PortScan", "Hint: Enter a friendly name for the target in the custom name field. This is optional and used for labeling scan results.")

        return root
    }


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


    private suspend fun pingWebsiteXTimes(urlString: String, count: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val times = mutableListOf<String>()
            for (i in 1..count) {
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
                        val timeTaken = endTime - startTime
                        times.add("${timeTaken}ms")
                    } else {
                        times.add("timeout")
                    }
                } catch (e: Exception) {
                    times.add("timeout")
                }
            }
            times
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun portScanHost(host: String, startPort: Int, endPort: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val openPorts = mutableListOf<String>()
            try {
                val address = java.net.InetAddress.getByName(host)
                for (port in startPort..endPort) {
                    try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress(address, port), 100)
                        socket.close()
                        openPorts.add(port.toString())
                    } catch (_: Exception) {

                    }
                }
            } catch (e: Exception) {
                openPorts.add("Port scan failed: ${e.localizedMessage}")
            }
            openPorts
        }
    }
}
