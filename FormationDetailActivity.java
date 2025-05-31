package com.example.pfe;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormationDetailActivity extends AppCompatActivity {

    private static final String TAG = "FormationDetailActivity";
    private DatabaseReference formationRef;
    private TextView tvTitle, tvDescription, tvDuration, tvStartDate, tvFormateur, tvEmail, tvMateriel, tvModalite, tvPlaces, tvPrice, tvFileLink, tvEnrollLink;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formation_detail);

        // Initialiser la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser les vues
        tvTitle = findViewById(R.id.tv_title);
        tvDescription = findViewById(R.id.tv_description);
        tvDuration = findViewById(R.id.tv_duration);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvFormateur = findViewById(R.id.tv_formateur);
        tvEmail = findViewById(R.id.tv_email);
        tvMateriel = findViewById(R.id.tv_materiel);
        tvModalite = findViewById(R.id.tv_modalite);
        tvPlaces = findViewById(R.id.tv_places);
        tvPrice = findViewById(R.id.tv_price);
        tvFileLink = findViewById(R.id.tv_file_link);
        tvEnrollLink = findViewById(R.id.tv_enroll_link);

        // Underline the enroll link text
        tvEnrollLink.setPaintFlags(tvEnrollLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Récupérer l'ID de la formation et de la catégorie
        String formationId = getIntent().getStringExtra("formationId");
        String categoryId = getIntent().getStringExtra("categoryId");
        if (formationId == null || categoryId == null) {
            Toast.makeText(this, "Erreur : ID de formation ou catégorie manquant", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialiser la référence Firebase
        formationRef = FirebaseDatabase.getInstance().getReference("categories").child(categoryId).child("formations").child(formationId);

        // Charger les détails de la formation
        loadFormationDetails();

        // Gérer le clic sur le TextView d'inscription
        tvEnrollLink.setOnClickListener(v -> {
            String enrollmentUrl = "https://votre-site-web.com/inscription?formation=" + formationId;
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(enrollmentUrl));
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(this, "Aucune application pour ouvrir le lien", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening enrollment URL: " + e.getMessage());
                Toast.makeText(this, "Erreur lors de l'ouverture du lien", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFormationDetails() {
        formationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(FormationDetailActivity.this, "Formation introuvable", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // Récupérer les données
                String titre = getValueFromAlternateKeys(snapshot, new String[]{"titre", "intitule", "name", "nom", "title"}, "Sans titre");
                String description = getValueFromAlternateKeys(snapshot, new String[]{"description", "desc", "detail"}, "Aucune description");
                String duree = parseIntFromSnapshot(snapshot, "duree", "N/A");
                String dateDebut = snapshot.child("dateDebut").getValue(String.class);
                String formateur = snapshot.child("formateurNom").getValue(String.class);
                String email = snapshot.child("formateurEmail").getValue(String.class);
                String materiel = snapshot.child("materiel").getValue(String.class);
                String modalite = snapshot.child("modalite").getValue(String.class);
                String places = parseIntFromSnapshot(snapshot, "places", "N/A");
                String prix = parsePriceFromSnapshot(snapshot);

                // Mettre à jour l'UI
                tvTitle.setText(titre);
                tvDescription.setText(description);
                tvDuration.setText("Durée: " + duree + " heures");
                tvStartDate.setText("Date de début: " + (dateDebut != null ? formatDate(dateDebut) : "Non spécifiée"));
                tvFormateur.setText("Formateur: " + (formateur != null ? formateur : "Non spécifié"));
                tvEmail.setText("Email formateur: " + (email != null ? email : "N/A"));
                tvMateriel.setText("Matériel: " + (materiel != null ? materiel : "N/A"));
                tvModalite.setText("Modalité: " + (modalite != null ? (modalite.equals("en_ligne") ? "En ligne" : "Présentiel") : "N/A"));
                tvPlaces.setText("Places disponibles: " + places);
                tvPrice.setText("Prix: " + prix + " DT");

                // Gérer le fichier
                String fichierUrl = snapshot.child("fichierUrl").getValue(String.class);
                if (fichierUrl == null) {
                    fichierUrl = snapshot.child("imageUrl").getValue(String.class);
                }
                final String finalFichierUrl = fichierUrl;
                if (finalFichierUrl != null && !finalFichierUrl.isEmpty()) {
                    tvFileLink.setVisibility(View.VISIBLE);
                    tvFileLink.setOnClickListener(v -> {
                        try {
                            if (finalFichierUrl.startsWith("http://") || finalFichierUrl.startsWith("https://")) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalFichierUrl));
                                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(browserIntent);
                                } else {
                                    Toast.makeText(FormationDetailActivity.this, "Aucune application pour ouvrir le fichier", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                FirebaseStorage.getInstance().getReference(finalFichierUrl).getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                                            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                                startActivity(browserIntent);
                                            } else {
                                                Toast.makeText(FormationDetailActivity.this, "Aucune application pour ouvrir le fichier", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error fetching download URL: " + e.getMessage());
                                            Toast.makeText(FormationDetailActivity.this, "Erreur lors du téléchargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening file: " + e.getMessage());
                            Toast.makeText(FormationDetailActivity.this, "Erreur lors de l'ouverture du fichier", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    tvFileLink.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(FormationDetailActivity.this, "Erreur: " + error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private String getValueFromAlternateKeys(DataSnapshot snapshot, String[] keys, String defaultValue) {
        for (String key : keys) {
            if (snapshot.hasChild(key)) {
                String value = snapshot.child(key).getValue(String.class);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
        }
        return defaultValue;
    }

    private String parseIntFromSnapshot(DataSnapshot snapshot, String key, String defaultValue) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            if (value instanceof Long) {
                return String.valueOf(((Long) value).intValue());
            } else if (value instanceof Integer) {
                return String.valueOf((Integer) value);
            } else if (value instanceof String) {
                try {
                    return String.valueOf(Integer.parseInt((String) value));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing int: " + value);
                }
            }
        }
        return defaultValue;
    }

    private String parsePriceFromSnapshot(DataSnapshot snapshot) {
        if (snapshot.hasChild("prix")) {
            Object value = snapshot.child("prix").getValue();
            if (value instanceof Double) {
                return String.format("%.2f", value);
            } else if (value instanceof Long) {
                return String.format("%.2f", ((Long) value).doubleValue());
            } else if (value instanceof String) {
                try {
                    return String.format("%.2f", Double.parseDouble((String) value));
                } catch (NumberFormatException e) {
                    return "0.00";
                }
            }
        }
        return "0.00";
    }

    private String formatDate(String dateString) {
        if (dateString == null) return "Non spécifiée";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage());
            return dateString;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
