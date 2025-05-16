/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package gestion.pack.panels;

import gestion.pack.Main;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 *
 * @author xeon
 */
public class GraphPanel extends javax.swing.JPanel {

    private final Main mainFrame;

    /**
     * Creates new form GraphPanel
     * @param mainFrame
     */
    public GraphPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        initComponents();
        afficherHistogramme();
    }
    
    private void afficherHistogramme() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // Récupération des statistiques depuis l’API
            HttpGet request = new HttpGet("http://localhost:3000/materiels/stat");
            String json = client.execute(request, response -> EntityUtils.toString(response.getEntity()));

            // Parsing JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, Map.class);
            Map<String, Integer> etats = (Map<String, Integer>) map.get("etat");

            // Création du dataset pour le camembert
            org.jfree.data.general.DefaultPieDataset<String> dataset = new org.jfree.data.general.DefaultPieDataset<>();
            dataset.setValue("Bon", etats.getOrDefault("Bon", 0));
            dataset.setValue("Mauvais", etats.getOrDefault("Mauvais", 0));
            dataset.setValue("Abimé", etats.getOrDefault("Abimé", 0));

            // Création du camembert
            JFreeChart pieChart = ChartFactory.createPieChart(
                "Répartition des états de matériels",  // Titre
                dataset,                               // Données
                true,                                   // Inclure légende
                true,
                false
            );

            // Mise à jour du chartPanel
            chartPanel.removeAll();
            chartPanel.setLayout(new BorderLayout());
            chartPanel.add(new ChartPanel(pieChart), BorderLayout.CENTER);
            chartPanel.revalidate();
            chartPanel.repaint();

        } catch (IOException e) {
            e.printStackTrace(); // Pour faciliter le débogage
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnTable = new javax.swing.JButton();
        chartPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(204, 204, 255));

        btnTable.setText("Show Table");
        btnTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTableActionPerformed(evt);
            }
        });

        chartPanel.setBackground(new java.awt.Color(153, 153, 255));

        javax.swing.GroupLayout chartPanelLayout = new javax.swing.GroupLayout(chartPanel);
        chartPanel.setLayout(chartPanelLayout);
        chartPanelLayout.setHorizontalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 622, Short.MAX_VALUE)
        );
        chartPanelLayout.setVerticalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 319, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(chartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnTable))
                .addGap(38, 38, 38))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addComponent(btnTable)
                .addGap(18, 18, 18)
                .addComponent(chartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(57, 57, 57))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTableActionPerformed
        mainFrame.showPanel("TablePanel");
    }//GEN-LAST:event_btnTableActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnTable;
    private javax.swing.JPanel chartPanel;
    // End of variables declaration//GEN-END:variables
}
