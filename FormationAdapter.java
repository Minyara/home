package com.example.pfe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FormationAdapter extends RecyclerView.Adapter<FormationAdapter.FormationViewHolder> implements Filterable {

    private Context context;
    private List<Formation> formationList;
    private List<Formation> formationListFull;

    public FormationAdapter(Context context, List<Formation> formationList) {
        this.context = context;
        this.formationList = formationList;
        this.formationListFull = new ArrayList<>(formationList);
    }

    @Override
    public FormationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_formation, parent, false);
        return new FormationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FormationViewHolder holder, int position) {
        Formation f = formationList.get(position);
        holder.titre.setText(f.getIntitule());
        holder.date.setText("Début : " + f.getDateDebut());
        holder.prix.setText(f.getPrix() + " DT");
    }

    @Override
    public int getItemCount() {
        return formationList.size();
    }

    public static class FormationViewHolder extends RecyclerView.ViewHolder {
        TextView titre, date, prix;
        ImageView image;

        public FormationViewHolder(View itemView) {
            super(itemView);
            titre = itemView.findViewById(R.id.titreFormation);
            date = itemView.findViewById(R.id.dateDebutFormation);
            prix = itemView.findViewById(R.id.prixFormation);
            image = itemView.findViewById(R.id.imageFormation);
        }
    }

    @Override
    public Filter getFilter() {
        return filterFormation;
    }

    private Filter filterFormation = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Formation> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(formationListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Formation f : formationListFull) {
                    if (f.getIntitule().toLowerCase().contains(filterPattern)) {
                        filteredList.add(f);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            formationList.clear();
            formationList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}
