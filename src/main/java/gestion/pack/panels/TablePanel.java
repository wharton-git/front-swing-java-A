/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package gestion.pack.panels;

import gestion.pack.Main;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.awt.HeadlessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 *
 * @author xeon
 */
public final class TablePanel extends javax.swing.JPanel {

    private final Main mainFrame;

    /**
     * Creates new form TablePanel
     * @param mainFrame
     */
    public TablePanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        initComponents();
        fetchData();
        initSpecialDataTable();
        
        //Listener for table row modification
        dataTable.getModel().addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                DefaultTableModel model1 = (DefaultTableModel) dataTable.getModel();

                try {
                    Integer id = (Integer) model1.getValueAt(row, 0);
                    String designation = (String) model1.getValueAt(row, 1);
                    Integer quantite = (Integer) model1.getValueAt(row, 2);
                    String etat = (String) model1.getValueAt(row, 3);

                    envoyerModification(id, designation, quantite, etat);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Erreur lors de la mise à jour : " + ex.getMessage());
                }
            }
        });
        
        javax.swing.JComboBox<String> comboBox = new javax.swing.JComboBox<>(new String[] {"Bon", "Mauvais", "Abimé"});
        dataTable.getColumnModel().getColumn(3).setCellEditor(new javax.swing.DefaultCellEditor(comboBox));

    }
    
    public void fetchData() {
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            
            HttpGet request = new HttpGet("http://localhost:3000/materiels");
            String json = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> produits = mapper.readValue(json, new TypeReference<>() {});

            DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
            model.setRowCount(0); 

            for (Map<String, Object> produit : produits) {
                model.addRow(new Object[] {
                    produit.get("id"),
                    produit.get("designation"),
                    produit.get("quantite"),
                    produit.get("etat")
                });
            }
            
            fetchStats();
            
        }catch(Exception e){
            
        }
    }
    
    public void addData() {
        
        String designation = nameTF.getText();
        String quantiteStr = quantityTF.getText();
        String etat = (String) stateCB.getSelectedItem();

        if (designation.isEmpty() || quantiteStr.isEmpty() || etat == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.");
            return;
        }

        try {
            int quantite = Integer.parseInt(quantiteStr);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(Map.of(
                "designation", designation,
                "quantite", quantite,
                "etat", etat
            ));

            // Envoi de la requête POST
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost request = new HttpPost("http://localhost:3000/materiels");
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(jsonBody, java.nio.charset.StandardCharsets.UTF_8));

                int statusCode = client.execute(request, response -> response.getCode());

                if (statusCode == 201 || statusCode == 200) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Matériel ajouté avec succès !");
                    fetchData(); 
                    nameTF.setText("");
                    quantityTF.setText("");
                    stateCB.setSelectedIndex(0);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout : code " + statusCode);
                }
            }

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Quantité doit être un nombre entier.");
        } catch (HeadlessException | IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        }
    }
    
    public void deleteData() {
        
        int[] selectedRows = dataTable.getSelectedRows();

        if (selectedRows.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins une ligne à supprimer.");
            return;
        }

        int confirm = javax.swing.JOptionPane.showConfirmDialog(this, "Confirmer la suppression ?", "Suppression", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

        DefaultTableModel model = (DefaultTableModel) dataTable.getModel();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelIndex = dataTable.convertRowIndexToModel(selectedRows[i]);
                Integer id = (Integer) model.getValueAt(modelIndex, 0);

                HttpPost request = new HttpPost("http://localhost:3000/materiels/delete");
                request.setHeader("Content-Type", "application/json");

                String json = "{\"id\":" + id + "}";
                request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

                int statusCode = client.execute(request, response -> response.getCode());

                if (statusCode == 200 || statusCode == 204) {
                    model.removeRow(modelIndex);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Échec suppression ID: " + id);
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        }
    }
    
    private void envoyerModification(int id, String designation, int quantite, String etat) {
        
        String url = "http://localhost:3000/materiels/";

        String json = String.format(
            "{\"id\": %d, \"designation\": \"%s\", \"quantite\": %d, \"etat\": \"%s\"}",
            id, designation, quantite, etat
        );

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            org.apache.hc.client5.http.classic.methods.HttpPatch patch = new org.apache.hc.client5.http.classic.methods.HttpPatch(url);
            patch.setHeader("Content-Type", "application/json");
            patch.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            int status = httpClient.execute(patch, response -> response.getCode());

            if (status != 200 && status != 204) {
                javax.swing.JOptionPane.showMessageDialog(this, "Erreur de mise à jour (code: " + status + ")");
            }
        } catch (IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Erreur PATCH : " + e.getMessage());
        }
    }

    public void fetchStats() {
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:3000/materiels/stat");
            String json = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> stats = mapper.readValue(json, new TypeReference<>() {});

            // Total
            totalLBL.setText(String.valueOf(stats.get("total")));

            // États
            @SuppressWarnings("unchecked")
            Map<String, Integer> etats = (Map<String, Integer>) stats.get("etat");
            bonLBL.setText(String.valueOf(etats.getOrDefault("Bon", 0)));
            mauvaisLBL.setText(String.valueOf(etats.getOrDefault("Mauvais", 0)));
            abimeLBL.setText(String.valueOf(etats.getOrDefault("Abimé", 0)));

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Erreur lors de la récupération des statistiques : " + e.getMessage());
        }
    }   

    
    public void initSpecialDataTable() {
        
        DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
        dataTable.setModel(new DefaultTableModel(model.getDataVector(), getColumnNames()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Seule la colonne "Id" n'est pas éditable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Integer.class;
                    case 1 -> String.class;
                    case 2 -> Integer.class;
                    case 3 -> String.class;
                    default -> Object.class;
                };
            }
        });

    }
    
    private Vector<String> getColumnNames() {
        
        Vector<String> columns = new Vector<>();
        columns.add("Id");
        columns.add("Designation");
        columns.add("Qutantité");
        columns.add("État");
        return columns;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnGraph = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        dataTable = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        nameTF = new javax.swing.JTextField();
        quantityTF = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        stateCB = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        totalLBL = new javax.swing.JLabel();
        bonLBL = new javax.swing.JLabel();
        mauvaisLBL = new javax.swing.JLabel();
        abimeLBL = new javax.swing.JLabel();

        setBackground(new java.awt.Color(204, 204, 255));

        btnGraph.setText("Graph");
        btnGraph.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGraphActionPerformed(evt);
            }
        });

        dataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Id", "Designation", "Qutantité", "État"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(dataTable);

        btnAdd.setText("Ajouter");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnDelete.setText("Supprimer");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        jLabel1.setText("Designation :");

        jLabel2.setText("Quantité :");

        stateCB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Bon", "Mauvais", "Abimé" }));

        jLabel3.setText("État :");

        jLabel4.setText("Total :");

        jLabel5.setText("Bon :");

        jLabel6.setText("Mauvais :");

        jLabel7.setText("Abimé :");

        totalLBL.setText("0");

        bonLBL.setText("0");

        mauvaisLBL.setText("0");

        abimeLBL.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(62, 62, 62)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(quantityTF)
                                .addGap(93, 93, 93)
                                .addComponent(jLabel3)
                                .addGap(32, 32, 32)
                                .addComponent(stateCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(nameTF, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btnAdd)
                                .addGap(18, 18, 18)
                                .addComponent(btnGraph)))
                        .addGap(10, 10, 10))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(39, 39, 39)
                        .addComponent(totalLBL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDelete))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(31, 31, 31)
                        .addComponent(bonLBL)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel6)
                        .addGap(40, 40, 40)
                        .addComponent(mauvaisLBL)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel7)
                        .addGap(42, 42, 42)
                        .addComponent(abimeLBL)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(26, 26, 26))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(quantityTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(stateCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGraph)
                    .addComponent(btnAdd))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDelete)
                    .addComponent(jLabel4)
                    .addComponent(totalLBL))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(bonLBL)
                    .addComponent(mauvaisLBL)
                    .addComponent(abimeLBL))
                .addGap(19, 19, 19))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnGraphActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGraphActionPerformed
        mainFrame.showPanel("GraphPanel");
    }//GEN-LAST:event_btnGraphActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        addData();
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        deleteData();
    }//GEN-LAST:event_btnDeleteActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel abimeLBL;
    private javax.swing.JLabel bonLBL;
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnGraph;
    private javax.swing.JTable dataTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel mauvaisLBL;
    private javax.swing.JTextField nameTF;
    private javax.swing.JTextField quantityTF;
    private javax.swing.JComboBox<String> stateCB;
    private javax.swing.JLabel totalLBL;
    // End of variables declaration//GEN-END:variables
}
