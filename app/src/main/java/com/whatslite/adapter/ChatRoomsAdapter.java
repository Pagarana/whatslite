package com.whatslite.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.whatslite.R;
import com.whatslite.model.ChatRoom;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomsAdapter extends RecyclerView.Adapter<ChatRoomsAdapter.ChatRoomViewHolder> {
    
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private OnChatRoomClickListener clickListener;
    private String currentUserNickname;
    
    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }
    
    public ChatRoomsAdapter(OnChatRoomClickListener clickListener) {
        this.clickListener = clickListener;
    }
    
    public void updateChatRooms(List<ChatRoom> newChatRooms, String currentUserNickname) {
        this.chatRooms = new ArrayList<>(newChatRooms);
        this.currentUserNickname = currentUserNickname;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom);
    }
    
    @Override
    public int getItemCount() {
        return chatRooms.size();
    }
    
    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNickname;
        private TextView tvLastMessage;
        private TextView tvTime;
        private TextView tvAvatar;
        private View unreadIndicator;
        
        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onChatRoomClick(chatRooms.get(position));
                }
            });
        }
        
        public void bind(ChatRoom chatRoom) {
            String otherUserNickname = chatRoom.getOtherParticipant(currentUserNickname);
            tvNickname.setText(otherUserNickname);
            
            // Show first letter in avatar
            String firstLetter = otherUserNickname.length() > 0 ? 
                otherUserNickname.substring(0, 1).toUpperCase() : "?";
            tvAvatar.setText(firstLetter);
            
            if (chatRoom.lastMessage != null && !chatRoom.lastMessage.isEmpty()) {
                tvLastMessage.setText(chatRoom.lastMessage);
                tvLastMessage.setVisibility(View.VISIBLE);
            } else {
                tvLastMessage.setText("No messages yet");
                tvLastMessage.setVisibility(View.VISIBLE);
            }
            
            // Format time
            if (chatRoom.lastMessageTime > 0) {
                CharSequence timeText = DateUtils.getRelativeTimeSpanString(
                    chatRoom.lastMessageTime,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                );
                tvTime.setText(timeText);
            } else {
                tvTime.setText("");
            }
            
            // For now, don't show unread indicator - we can implement this later
            unreadIndicator.setVisibility(View.GONE);
        }
    }
}
