package com.example.pfe;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private LinearLayout cardsContainer;
    private ProgressBar progressBar;
    private DatabaseReference formationsRef, usersRef;
    private Map<String, List<DataSnapshot>> formationsByCategory = new HashMap<>();
    private EditText searchEditText;
    private ImageView ivFilter;
    private List<DataSnapshot> allFormations = new ArrayList<>();
    private TextView tvTotalCourses, tvActiveStudents, tvTotalFormateurs, tvViewAll;
    private MaterialButton btnClearSearch;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        initViews();
        loadData();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cardsContainer = findViewById(R.id.cards_container);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        ivFilter = findViewById(R.id.iv_filter);
        tvTotalCourses = findViewById(R.id.tv_total_courses);
        tvActiveStudents = findViewById(R.id.tv_active_students);
        tvTotalFormateurs = findViewById(R.id.tv_total_formateurs);
        tvViewAll = findViewById(R.id.tv_view_all);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        formationsRef = FirebaseDatabase.getInstance().getReference("formations");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    private void setupListeners() {
        ivFilter.setOnClickListener(v -> showFilterDialog());
        tvViewAll.setOnClickListener(v -> showAllFormations());

        searchEditText.setOnKeyListener((v, keyCode, event) -> {
            filterFormations(searchEditText.getText().toString());
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            showAllFormations();
        });
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        cardsContainer.removeAllViews();
        cardsContainer.setVisibility(View.VISIBLE);

        Log.d(TAG, "Loading data...");

        formationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                formationsByCategory.clear();
                allFormations.clear();

                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "No data found in 'formations' node");
                    showNoDataMessage();
                    progressBar.setVisibility(View.GONE);
                    updateUI(0, 0, 0);
                    return;
                }

                Log.d(TAG, "Number of formations found: " + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Processing formation: " + snapshot.getKey());

                    if (isFormationActive(snapshot)) {
                        allFormations.add(snapshot);

                        String category = getValueFromAlternateKeys(snapshot,
                                new String[]{"categorie", "category", "type"}, "Autres");

                        if (!formationsByCategory.containsKey(category)) {
                            formationsByCategory.put(category, new ArrayList<>());
                        }
                        formationsByCategory.get(category).add(snapshot);
                    }
                }

                loadUsersData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read error (formations): " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Home.this, "Erreur de connexion: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUsersData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalStudents = 0;
                int totalFormateurs = 0;

                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "No data found in 'users' node");
                    updateUI(allFormations.size(), 0, 0);
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userType = snapshot.child("type").getValue(String.class);
                    if (userType != null) {
                        if (userType.equalsIgnoreCase("etudiant")) {
                            totalStudents++;
                        } else if (userType.equalsIgnoreCase("formateur")) {
                            totalFormateurs++;
                        }
                    }
                }

                Log.d(TAG, "Total students: " + totalStudents + ", Total formateurs: " + totalFormateurs);
                updateUI(allFormations.size(), totalStudents, totalFormateurs);
                progressBar.setVisibility(View.GONE);
                displayFormationsByCategory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read error (users): " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Home.this, "Erreur de connexion: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isFormationActive(DataSnapshot snapshot) {
        String statut = snapshot.child("statut").getValue(String.class);
        if (statut == null || !statut.equalsIgnoreCase("active")) {
            return false;
        }

        try {
            Date currentDate = new Date(); // Current date: May 31, 2025, 10:03 AM CET
            String dateDebutStr = snapshot.child("dateDebut").getValue(String.class);
            String dateFinStr = snapshot.child("dateFin").getValue(String.class);

            if (dateDebutStr == null || dateFinStr == null) {
                return false;
            }

            Date startDate = DATE_FORMAT.parse(dateDebutStr);
            Date endDate = DATE_FORMAT.parse(dateFinStr);

            return startDate != null && endDate != null &&
                    currentDate.after(startDate) && currentDate.before(endDate);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing dates for formation " + snapshot.getKey() + ": " + e.getMessage());
            return false;
        }
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

    private void updateUI(int totalCourses, int totalStudents, int totalFormateurs) {
        runOnUiThread(() -> {
            tvTotalCourses.setText(String.valueOf(totalCourses));

            if (totalStudents >= 1000) {
                double studentsInK = totalStudents / 1000.0;
                tvActiveStudents.setText(totalStudents % 1000 == 0 ?
                        String.format("%.0fK", studentsInK) :
                        String.format("%.1fK", studentsInK));
            } else {
                tvActiveStudents.setText(String.valueOf(totalStudents));
            }

            if (totalFormateurs >= 1000) {
                double formateursInK = totalFormateurs / 1000.0;
                tvTotalFormateurs.setText(totalFormateurs % 1000 == 0 ?
                        String.format("%.0fK", formateursInK) :
                        String.format("%.1fK", formateursInK));
            } else {
                tvTotalFormateurs.setText(String.valueOf(totalFormateurs));
            }

            if (allFormations.isEmpty()) {
                cardsContainer.setVisibility(View.GONE);
                findViewById(R.id.empty_state).setVisibility(View.VISIBLE);
                btnClearSearch.setVisibility(View.GONE);
            } else {
                cardsContainer.setVisibility(View.VISIBLE);
                findViewById(R.id.empty_state).setVisibility(View.GONE);
                btnClearSearch.setVisibility(View.GONE);
            }
        });
    }

    private void displayFormationsByCategory() {
        cardsContainer.removeAllViews();

        if (formationsByCategory.isEmpty()) {
            showNoDataMessage();
            return;
        }

        for (Map.Entry<String, List<DataSnapshot>> entry : formationsByCategory.entrySet()) {
            String category = entry.getKey();
            List<DataSnapshot> formations = entry.getValue();

            if (formations == null || formations.isEmpty()) {
                continue;
            }

            TextView categoryTitle = new TextView(this);
            categoryTitle.setText(category);
            categoryTitle.setTextSize(22);
            categoryTitle.setTypeface(null, Typeface.BOLD);
            categoryTitle.setTextColor(getResources().getColor(R.color.iefpTextPrimary));
            categoryTitle.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(12));
            cardsContainer.addView(categoryTitle);

            for (DataSnapshot formationSnapshot : formations) {
                addFormationCard(formationSnapshot);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void addFormationCard(DataSnapshot formationSnapshot) {
        CardView card = new CardView(this);
        CardView.LayoutParams params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        card.setLayoutParams(params);
        card.setCardElevation(dpToPx(6));
        card.setRadius(dpToPx(12));
        card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        card.setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        card.setForeground(getResources().getDrawable(android.R.drawable.list_selector_background));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_background_selector);

        String titre = getValueFromAlternateKeys(formationSnapshot,
                new String[]{"titre", "intitule", "name", "nom", "title"}, "Titre inconnu");
        TextView title = new TextView(this);
        title.setText(titre);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.iefpTextPrimary));
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        layout.addView(title);

        String description = getValueFromAlternateKeys(formationSnapshot,
                new String[]{"description", "desc", "detail"}, "Aucune description");
        if (description.length() > 100) {
            description = description.substring(0, 100) + "...";
        }
        TextView desc = new TextView(this);
        desc.setText(description);
        desc.setTextSize(14);
        desc.setTextColor(getResources().getColor(R.color.iefpTextSecondary));
        desc.setPadding(0, dpToPx(8), 0, dpToPx(12));
        desc.setMaxLines(3);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        layout.addView(desc);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.HORIZONTAL);
        infoLayout.setBackgroundResource(R.drawable.info_background);
        infoLayout.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        String duree = parseIntFromSnapshot(formationSnapshot, "duree", "0");
        TextView duration = new TextView(this);
        duration.setText("Durée: " + duree + "h");
        duration.setTextSize(13);
        duration.setTextColor(getResources().getColor(R.color.iefpBlue));
        infoLayout.addView(duration);

        String prix = parsePriceFromSnapshot(formationSnapshot);
        TextView price = new TextView(this);
        price.setText(" | Prix: " + prix + " DT");
        price.setTextSize(13);
        price.setTextColor(getResources().getColor(R.color.iefpBlue));
        price.setPadding(dpToPx(8), 0, 0, 0);
        infoLayout.addView(price);

        String places = parseIntFromSnapshot(formationSnapshot, "places", "0");
        TextView placesText = new TextView(this);
        placesText.setText(" | Places: " + places);
        placesText.setTextSize(13);
        placesText.setTextColor(getResources().getColor(R.color.iefpBlue));
        placesText.setPadding(dpToPx(8), 0, 0, 0);
        infoLayout.addView(placesText);

        layout.addView(infoLayout);

        String formateurNom = formationSnapshot.child("formateurNom").getValue(String.class);
        TextView formateur = new TextView(this);
        formateur.setText("Formateur: " + (formateurNom != null ? formateurNom : "Non spécifié"));
        formateur.setTextSize(14);
        formateur.setTextColor(getResources().getColor(R.color.iefpTextSecondary));
        formateur.setPadding(0, dpToPx(12), 0, 0);
        layout.addView(formateur);

        card.addView(layout);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, FormationDetailActivity.class);
            intent.putExtra("formationId", formationSnapshot.getKey());
            startActivity(intent);
        });

        cardsContainer.addView(card);
    }

    private void filterFormations(String query) {
        cardsContainer.removeAllViews();
        query = query.toLowerCase().trim();

        boolean hasResults = false;
        for (DataSnapshot formationSnapshot : allFormations) {
            String titre = getValueFromAlternateKeys(formationSnapshot,
                    new String[]{"titre", "intitule", "name", "nom", "title"}, "");
            String description = getValueFromAlternateKeys(formationSnapshot,
                    new String[]{"description", "desc", "detail"}, "");
            String categorie = getValueFromAlternateKeys(formationSnapshot,
                    new String[]{"categorie", "category", "type"}, "");

            if (query.isEmpty() ||
                    titre.toLowerCase().contains(query) ||
                    description.toLowerCase().contains(query) ||
                    categorie.toLowerCase().contains(query)) {
                addFormationCard(formationSnapshot);
                hasResults = true;
            }
        }

        if (!hasResults) {
            cardsContainer.setVisibility(View.GONE);
            findViewById(R.id.empty_state).setVisibility(View.VISIBLE);
            btnClearSearch.setVisibility(View.VISIBLE);
            showNoResultsMessage(query);
        } else {
            cardsContainer.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_state).setVisibility(View.GONE);
            btnClearSearch.setVisibility(View.GONE);
        }
    }

    private void showNoDataMessage() {
        cardsContainer.setVisibility(View.GONE);
        findViewById(R.id.empty_state).setVisibility(View.VISIBLE);
        btnClearSearch.setVisibility(View.GONE);
    }

    private void showNoResultsMessage(String query) {
        TextView text = findViewById(R.id.tv_empty_state_message);
        if (text != null) {
            text.setText("Aucun résultat pour \"" + query + "\"");
        }
    }

    private void showFilterDialog() {
        Toast.makeText(this, "Fonctionnalité de filtrage à implémenter", Toast.LENGTH_SHORT).show();
    }

    private void showAllFormations() {
        searchEditText.setText("");
        displayFormationsByCategory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Accueil");
        menu.add(0, 1, 0, "À propos");
        menu.add(0, 2, 0, "Aide");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                return true;
            case 1:
                startActivity(new Intent(this, About.class));
                return true;
            case 2:
                startActivity(new Intent(this, Help.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}