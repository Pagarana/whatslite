package com.whatslite.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.whatslite.R;
import com.whatslite.model.User;
import java.util.ArrayList;
import java.util.List;

public class OnlineUsersAdapter extends RecyclerView.Adapter<OnlineUsersAdapter.UserViewHolder> {
    
    private List<User> users = new ArrayList<>();
    private OnUserClickListener clickListener;
    
    public interface OnUserClickListener {
        void onUserClick(User user);
    }
    
    public OnlineUsersAdapter(OnUserClickListener clickListener) {
        this.clickListener = clickListener;
    }
    
    public void updateUsers(List<User> newUsers) {
        this.users = new ArrayList<>(newUsers);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_online_user, parent, false);
        return new UserViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNickname;
        private TextView tvLanguage;
        private View onlineIndicator;
        
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvLanguage = itemView.findViewById(R.id.tvLanguage);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onUserClick(users.get(position));
                }
            });
        }
        
        public void bind(User user) {
            // Show first letter of nickname
            String firstLetter = user.nickname.length() > 0 ? 
                user.nickname.substring(0, 1).toUpperCase() : "?";
            tvNickname.setText(firstLetter);
            tvLanguage.setText(getLanguageName(user.selectedLanguage));
            onlineIndicator.setVisibility(user.isOnline ? View.VISIBLE : View.GONE);
        }
        
        private String getLanguageName(String code) {
            switch (code) {
                case "tr": return "🇹🇷";
                case "en": return "🇺🇸";
                case "es": return "🇪🇸";
                case "fr": return "🇫🇷";
                case "de": return "🇩🇪";
                case "it": return "🇮🇹";
                case "pt": return "🇵🇹";
                case "ru": return "🇷🇺";
                case "ar": return "🇸🇦";
                case "zh": return "🇨🇳";
                default: return "🌐";
            }
        }
    }
}
