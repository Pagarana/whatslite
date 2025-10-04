package com.whatslite.adapter;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.whatslite.R;
import com.whatslite.model.Message;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
    
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    
    private List<Message> messages = new ArrayList<>();
    private String currentUserNickname;
    private OnMessageTranslateListener translateListener;
    
    public interface OnMessageTranslateListener {
        void onTranslateMessage(Message message);
    }
    
    public MessagesAdapter(String currentUserNickname, OnMessageTranslateListener translateListener) {
        this.currentUserNickname = currentUserNickname;
        this.translateListener = translateListener;
    }
    
    public void updateMessages(List<Message> newMessages) {
        android.util.Log.d("MessagesAdapter", "==== MessagesAdapter.updateMessages() ====");
        android.util.Log.d("MessagesAdapter", "Previous messages count: " + this.messages.size());
        android.util.Log.d("MessagesAdapter", "New messages count: " + (newMessages != null ? newMessages.size() : "NULL"));
        
        if (newMessages == null) {
            android.util.Log.w("MessagesAdapter", "⚠️ New messages list is NULL!");
            return;
        }
        
        this.messages = new ArrayList<>(newMessages);
        
        // Debug: Her mesajı logla
        for (int i = 0; i < newMessages.size(); i++) {
            Message msg = newMessages.get(i);
            android.util.Log.d("MessagesAdapter", "  Message " + i + ": " + msg.senderNickname + " -> " + msg.originalText + " (isFromMe: " + msg.isFromMe + ")");
        }
        
        notifyDataSetChanged();
        android.util.Log.d("MessagesAdapter", "notifyDataSetChanged() called");
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isFromMe ? TYPE_SENT : TYPE_RECEIVED;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_SENT) ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public int getLastMessageId() {
        if (messages.isEmpty()) {
            return 0; // Hiç mesaj yoksa 0 döner
        }
        Message lastMessage = messages.get(messages.size() - 1);
        android.util.Log.d("MessagesAdapter", "Last message ID: " + lastMessage.id);
        return lastMessage.id;
    }
    
    public void appendMessages(List<Message> newMessages) {
        android.util.Log.d("MessagesAdapter", "==== MessagesAdapter.appendMessages() ====");
        android.util.Log.d("MessagesAdapter", "Current messages count: " + this.messages.size());
        android.util.Log.d("MessagesAdapter", "New messages to append: " + (newMessages != null ? newMessages.size() : "NULL"));
        
        if (newMessages == null || newMessages.isEmpty()) {
            android.util.Log.w("MessagesAdapter", "⚠️ No new messages to append!");
            return;
        }
        
        // Yeni mesajları mevcut listeye ekle
        this.messages.addAll(newMessages);
        
        // Debug: Eklenen mesajları logla
        for (int i = 0; i < newMessages.size(); i++) {
            Message msg = newMessages.get(i);
            android.util.Log.d("MessagesAdapter", "  Appended[" + i + "]: " + msg.senderNickname + " -> " + msg.originalText + " (ID: " + msg.id + ")");
        }
        
        notifyDataSetChanged();
        android.util.Log.d("MessagesAdapter", "notifyDataSetChanged() called for append");
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTranslatedMessage;
        private TextView tvTime;
        private TextView tvTranslateButton;
        private TextView tvSenderName;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTranslatedMessage = itemView.findViewById(R.id.tvTranslatedMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTranslateButton = itemView.findViewById(R.id.tvTranslateButton);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
        
        public void bind(Message message) {
            // Show sender name only for received messages
            if (tvSenderName != null && !message.isFromMe) {
                tvSenderName.setText(message.senderNickname);
                tvSenderName.setVisibility(View.VISIBLE);
            } else if (tvSenderName != null) {
                tvSenderName.setVisibility(View.GONE);
            }
            
            // Create combined text: original + translation in one message
            SpannableStringBuilder combinedText = new SpannableStringBuilder();
            
            // Add original text
            combinedText.append(message.originalText);
            
            // Add translation if available and different
            if (message.isTranslated && message.translatedText != null && 
                !message.translatedText.equals(message.originalText)) {
                
                combinedText.append("\n");
                
                // Add translated text with turkuaz color
                String translationText = "🔄 " + message.translatedText;
                int startPos = combinedText.length();
                combinedText.append(translationText);
                int endPos = combinedText.length();
                
                // Apply turkuaz color to translation
                ForegroundColorSpan turkuazSpan = new ForegroundColorSpan(Color.parseColor("#40E0D0"));
                combinedText.setSpan(turkuazSpan, startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            // Set the combined text
            tvMessage.setText(combinedText);
            
            // Hide separate translation views (not needed anymore)
            if (tvTranslatedMessage != null) {
                tvTranslatedMessage.setVisibility(View.GONE);
            }
            
            // Hide translate button completely - auto-translation is active
            if (tvTranslateButton != null) {
                tvTranslateButton.setVisibility(View.GONE);
            }
            
            // Format timestamp
            String timeText = DateFormat.format("HH:mm", new Date(message.timestamp)).toString();
            tvTime.setText(timeText);
        }
    }
}
