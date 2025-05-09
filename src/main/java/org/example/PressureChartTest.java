package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;

public class PressureChartTest  extends JFrame {
    private TimeSeries series;
    private XYPlot plot;
    private boolean isLogScale = false;

    public PressureChartTest (String title, String yAxisLabel) {
        super(title);

        series = new TimeSeries("Measurements");

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Live-chart",       // Заголовок
                "Time",                       // Ось X
                yAxisLabel,                   // Ось Y
                dataset,                      // Датасет
                false, true, false
        );

        this.plot = chart.getXYPlot();
        //DateAxis axis = (DateAxis) plot.getDomainAxis();
        //axis.setAutoRange(true);
        //axis.setFixedAutoRange(60000.0);

        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
//

        JButton switchButton = new JButton("Toggle Scale");
        switchButton.addActionListener(e -> toggleYAxisScale());
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        panel.add(chartPanel);
//        panel.add(switchButton);
//
//        setContentPane(chartPanel);
//        pack();
//        setLocationRelativeTo(null);  // Центр экрана
//        setVisible(true);
//       // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Создаем панель управления
        JPanel controlPanel = new JPanel();
        //controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(switchButton);

        // Главная панель
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // Устанавливаем панель
        setContentPane(mainPanel);
        setSize(900, 600); // Явный размер окна
        setLocationRelativeTo(null); // Центрировать окно
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void addData(double value) {
        series.addOrUpdate(new Millisecond(), value);
    }

    public void toggleYAxisScale(){
        if (isLogScale){
            plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis("Pressure, mBr"));
            isLogScale = false;
        }else {
            LogarithmicAxis logAxis = new LogarithmicAxis("(Logarithmic scale) Pressure, mBr");
            plot.setRangeAxis(logAxis);
            isLogScale = true;
        }
    }
}

