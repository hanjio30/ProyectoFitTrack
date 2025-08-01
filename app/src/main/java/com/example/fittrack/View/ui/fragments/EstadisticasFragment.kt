package com.example.fittrack.View.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.fittrack.databinding.FragmentEstadisticasBinding
import android.annotation.SuppressLint as SuppressLint1

class EstadisticasFragment : Fragment() {

    private var _binding: FragmentEstadisticasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstadisticasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStatsCards()
        setupWebViewCharts()
    }

    private fun setupStatsCards() {
        // Configurar los datos de las tarjetas de estadísticas
        binding.tvPasos.text = "960"
        binding.tvCalorias.text = "1800"
    }

    @SuppressLint1("SetJavaScriptEnabled")
    private fun setupWebViewCharts() {
        // Configurar WebView para los gráficos
        val webView = binding.webViewCharts
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // HTML con los gráficos usando Chart.js
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    body { 
                        margin: 0; 
                        padding: 20px; 
                        background-color: #f5f5f5;
                        font-family: Arial, sans-serif;
                    }
                    .chart-container { 
                        background: white; 
                        padding: 20px; 
                        margin: 20px 0; 
                        border-radius: 16px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    .legend-container {
                        display: flex;
                        justify-content: center;
                        gap: 20px;
                        margin-bottom: 20px;
                        flex-wrap: wrap;
                    }
                    .legend-item {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        font-size: 12px;
                        color: #666;
                    }
                    .legend-color {
                        width: 12px;
                        height: 12px;
                        border-radius: 50%;
                    }
                    .chart-title {
                        text-align: center;
                        margin-bottom: 20px;
                        color: #333;
                        font-size: 16px;
                        font-weight: bold;
                    }
                    .credit {
                        text-align: left;
                        font-size: 12px;
                        color: #999;
                        margin-top: 10px;
                    }
                </style>
            </head>
            <body>
                <!-- Leyenda para el gráfico de líneas -->
                <div class="legend-container">
                    <div class="legend-item">
                        <div class="legend-color" style="background-color: #A8D5BA;"></div>
                        <span>Caminata Suave</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background-color: #7FB3D3;"></div>
                        <span>Caminata Rápida</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background-color: #F4A460;"></div>
                        <span>Trote</span>
                    </div>
                </div>
                
                <!-- Gráfico de líneas -->
                <div class="chart-container">
                    <canvas id="lineChart" style="max-height: 300px;"></canvas>
                </div>
                
                <div class="credit">Created with Chart.js</div>
                
                <!-- Gráfico circular -->
                <div class="chart-container">
                    <div class="chart-title">Distribución de Actividades</div>
                    <canvas id="pieChart" style="max-height: 300px;"></canvas>
                </div>

                <script>
                    // Gráfico de líneas
                    const lineCtx = document.getElementById('lineChart').getContext('2d');
                    const lineChart = new Chart(lineCtx, {
                        type: 'line',
                        data: {
                            labels: ['2002', '2003', '2004', '2005', '2006', '2007'],
                            datasets: [{
                                label: 'Caminata Suave',
                                data: [100, 150, 200, 250, 300, 350],
                                borderColor: '#A8D5BA',
                                backgroundColor: 'rgba(168, 213, 186, 0.3)',
                                fill: true,
                                tension: 0.4
                            }, {
                                label: 'Caminata Rápida',
                                data: [200, 250, 300, 350, 400, 450],
                                borderColor: '#7FB3D3',
                                backgroundColor: 'rgba(127, 179, 211, 0.3)',
                                fill: true,
                                tension: 0.4
                            }, {
                                label: 'Trote',
                                data: [300, 350, 400, 450, 500, 550],
                                borderColor: '#F4A460',
                                backgroundColor: 'rgba(244, 164, 96, 0.3)',
                                fill: true,
                                tension: 0.4
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false
                                }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true,
                                    grid: {
                                        color: 'rgba(0,0,0,0.1)'
                                    }
                                },
                                x: {
                                    grid: {
                                        display: false
                                    }
                                }
                            }
                        }
                    });

                    // Gráfico circular
                    const pieCtx = document.getElementById('pieChart').getContext('2d');
                    const pieChart = new Chart(pieCtx, {
                        type: 'doughnut',
                        data: {
                            labels: ['Caminata Suave', 'Caminata Rápida', 'Trote'],
                            datasets: [{
                                data: [45, 35, 20],
                                backgroundColor: [
                                    '#4A90A4',
                                    '#F4A460',
                                    '#708090'
                                ],
                                borderWidth: 2,
                                borderColor: '#fff'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    position: 'bottom',
                                    labels: {
                                        padding: 20,
                                        usePointStyle: true,
                                        font: {
                                            size: 12
                                        }
                                    }
                                }
                            }
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadData(htmlContent, "text/html", "utf-8")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}