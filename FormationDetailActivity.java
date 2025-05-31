package com.example.pfe;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class FormationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formation_detail);

        TextView tvTitre = findViewById(R.id.tv_titre);
        TextView tvDescription = findViewById(R.id.tv_description);
        TextView tvFormateur = findViewById(R.id.tv_formateur);
        TextView tvDates = findViewById(R.id.tv_dates);
        TextView tvDuree = findViewById(R.id.tv_duree);
        TextView tvStatut = findViewById(R.id.tv_statut);
        TextView tvModules = findViewById(R.id.tv_modules);

        // Récupération des données simples
        String titre = getIntent().getStringExtra("titre");
        String description = getIntent().getStringExtra("description");
        String formateurNom = getIntent().getStringExtra("formateurNom");
        String dateDebut = getIntent().getStringExtra("dateDebut");
        String dateFin = getIntent().getStringExtra("dateFin");
        int duree = getIntent().getIntExtra("duree", 0);
        String statut = getIntent().getStringExtra("statut");

        // Récupération des modules
        ArrayList<String> moduleTitres = getIntent().getStringArrayListExtra("moduleTitres");
        ArrayList<Integer> moduleDurees = getIntent().getIntegerArrayListExtra("moduleDurees");

        // Affichage
        tvTitre.setText(titre);
        tvDescription.setText("Description : " + description);
        tvFormateur.setText("Formateur : " + formateurNom);
        tvDates.setText("Du " + dateDebut + " au " + dateFin);
        tvDuree.setText("Durée totale : " + duree + " heures");
        tvStatut.setText("Statut : " + statut);

        // Construction de l'affichage des modules
        StringBuilder modulesBuilder = new StringBuilder("Modules :\n");
        if (moduleTitres != null && moduleDurees != null) {
            for (int i = 0; i < moduleTitres.size(); i++) {
                modulesBuilder.append("- ")
                        .append(moduleTitres.get(i))
                        .append(" (")
                        .append(moduleDurees.get(i))
                        .append("h)\n");
            }
        }
        tvModules.setText(modulesBuilder.toString());
    }
}
