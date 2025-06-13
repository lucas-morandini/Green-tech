package com.example.frontend.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.Messagerie;
import com.example.frontend.R;
import com.example.frontend.model.Group;

import org.w3c.dom.Text;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;

    // Constructeur de l'adaptateur
    public GroupAdapter(List<Group> messageList) {
        this.groupList = messageList;
    }

    // Crée la vue pour chaque élément de la liste
    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }
    public void updateList(List<Group> newList) {
        groupList = newList;
        notifyDataSetChanged();
    }
    // Lie les données à la vue de chaque item
    @Override
    public void onBindViewHolder(GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.groupName.setText(group.getName());
        String nbMessage = group.getMessages().size() + " messages";
        if(group.getMessages().isEmpty()) nbMessage = "Aucun message";
        holder.nbMessage.setText(nbMessage);
        String identificationCount = group.getIdentifications().size()+" identifications";
        holder.identificationCount.setText(identificationCount);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), Messagerie.class);
            intent.putExtra("groupId", group.getId());
            v.getContext().startActivity(intent);
        });
    }

    // Retourne le nombre d'éléments dans la liste
    @Override
    public int getItemCount() {
        return groupList.size();
    }

    // Classe pour stocker les références aux vues
    public static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView groupName;
        TextView nbMessage;

        TextView identificationCount;
        public GroupViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_name);
            nbMessage = itemView.findViewById(R.id.nb_messages);
            identificationCount = itemView.findViewById(R.id.identification_count);
        }
    }
}
