package com.example.frontend.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.model.Message;
import com.example.frontend.model.MessagePJ;
import com.example.frontend.utils.ColorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.frontend.utils.UIUtils;
import com.google.android.flexbox.FlexboxLayout;


public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private List<MessageGroup> groupedItems = new ArrayList<>();
    private long currentUserId;

    private OnAttachmentDownloadListener downloadListener;
    public MessageAdapter(long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnAttachmentDownloadListener(OnAttachmentDownloadListener listener) {
        this.downloadListener = listener;
    }

    // Classe interne pour gérer header ou message
    public static class MessageGroup {
        public enum Type { HEADER, MESSAGE }
        public Type type;
        public String headerDate; // pour HEADER
        public Message message;   // pour MESSAGE

        public MessageGroup(String headerDate) {
            this.type = Type.HEADER;
            this.headerDate = headerDate;
        }

        public MessageGroup(Message message) {
            this.type = Type.MESSAGE;
            this.message = message;
        }
    }

    // Met à jour la liste avec regroupement par date
    public void setMessages(List<Message> messages) {
        groupedItems.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE);
        String lastDate = null;

        for (Message m : messages) {
            // Récupérer la date sans l'heure pour grouper
            Date messageDate = m.getDate(); // Tu dois avoir un getter Date dans Message
            String dateStr = sdf.format(messageDate);

            if (!dateStr.equals(lastDate)) {
                // Ajouter un header
                groupedItems.add(new MessageGroup(dateStr));
                lastDate = dateStr;
            }
            // Ajouter le message
            groupedItems.add(new MessageGroup(m));
        }
        notifyDataSetChanged();
    }

    // Ajout d’un message en fin (gestion grouping simplifiée, on pourrait améliorer)
    public void addMessage(Message message) {
        // Récupérer la date formatée
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE);
        String messageDate = sdf.format(message.getDate());

        // Si dernière date en header différente, ajouter header
        if (groupedItems.isEmpty()) {
            groupedItems.add(new MessageGroup(messageDate));
        } else {
            // Chercher dernier header
            String lastHeaderDate = null;
            for (int i = groupedItems.size() - 1; i >= 0; i--) {
                MessageGroup mg = groupedItems.get(i);
                if (mg.type == MessageGroup.Type.HEADER) {
                    lastHeaderDate = mg.headerDate;
                    break;
                }
            }
            if (!messageDate.equals(lastHeaderDate)) {
                groupedItems.add(new MessageGroup(messageDate));
            }
        }
        groupedItems.add(new MessageGroup(message));
        notifyItemInserted(groupedItems.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        MessageGroup item = groupedItems.get(position);
        if (item.type == MessageGroup.Type.HEADER) {
            return VIEW_TYPE_HEADER;
        } else {
            // Message, on différencie selon userId
            return item.message.getUserId() == currentUserId ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
        }
    }

    @Override
    public int getItemCount() {
        return groupedItems.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_ME) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
            return new MessageViewHolder(view, downloadListener);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_other, parent, false);
            return new MessageViewHolder(view, downloadListener);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageGroup item = groupedItems.get(position);
        if (item.type == MessageGroup.Type.HEADER) {
            ((DateHeaderViewHolder) holder).bind(item.headerDate);
        } else {
            ((MessageViewHolder) holder).bind(item.message);
        }
    }

    // ViewHolder pour le header date
    public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.header_date_text);
        }

        public void bind(String dateStr) {
            dateText.setText(dateStr);
        }
    }

    // ViewHolder pour les messages (me et other)
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, heureText, initalText;
        FlexboxLayout pjContainer;

        OnAttachmentDownloadListener downloadListener;

        public MessageViewHolder(@NonNull View itemView, OnAttachmentDownloadListener listener) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            pjContainer = itemView.findViewById(R.id.pj_container);
            heureText = itemView.findViewById(R.id.message_time);
            initalText = itemView.findViewById(R.id.message_initials);
            this.downloadListener = listener;
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            heureText.setText(message.getHour());
            pjContainer.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (MessagePJ pj : message.getPieces_jointe()) {
                View attachmentView = inflater.inflate(R.layout.item_attachment, pjContainer, false);
                TextView filenameView = attachmentView.findViewById(R.id.attachment_filename);
                filenameView.setText(pj.getName());
                filenameView.setOnClickListener(v -> {
                    if (downloadListener != null) {
                        downloadListener.onDownloadRequested(pj);
                    }
                });
                pjContainer.addView(attachmentView);
            }

            initalText.setText(message.getUser().getInitials());
            int color = ColorUtils.getColorForUserId(message.getUserId());
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            initalText.setBackground(drawable);
        }
    }

    public interface OnAttachmentDownloadListener {
        void onDownloadRequested(MessagePJ pj);
    }
}
