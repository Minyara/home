package com.example.pfe;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FormationManagementActivity extends AppCompatActivity {

    private ListView listViewFormations;
    private ArrayList<Formation> formationList;
    private FormationAdapter formationAdapter;
    private DatabaseReference formationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formation_management);

        listViewFormations = findViewById(R.id.listViewFormations);
        formationList = new ArrayList<>();
        formationAdapter = new FormationAdapter(this, formationList);
        listViewFormations.setAdapter((ListAdapter) formationAdapter);

        // Référence vers Firebase
        formationsRef = FirebaseDatabase.getInstance().getReference("formations");

        fetchFormations();
    }

    private void fetchFormations() {
        formationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                formationList.clear();
                for (DataSnapshot formationSnap : snapshot.getChildren()) {
                    Formation formation = formationSnap.getValue(Formation.class);
                    if (formation != null) {
                        formationList.add(formation);
                    }
                }
                formationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FormationManagementActivity.this, "Erreur : " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
