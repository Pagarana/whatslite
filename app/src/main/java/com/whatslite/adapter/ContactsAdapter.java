package com.whatslite.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.whatslite.R;
import com.whatslite.model.Contact;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<Contact> contacts = new ArrayList<>();
    private OnContactLongClickListener longClickListener;
    private OnContactClickListener clickListener;

    public interface OnContactLongClickListener {
        void onContactLongClick(Contact contact);
    }

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public ContactsAdapter(OnContactLongClickListener longClickListener, OnContactClickListener clickListener) {
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
    }

    public void updateContacts(List<Contact> newContacts) {
        this.contacts = new ArrayList<>(newContacts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivContactAvatar;
        private TextView tvContactName;
        private TextView tvContactNickname;
        private TextView tvOnlineStatus;
        private View onlineIndicator;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivContactAvatar = itemView.findViewById(R.id.ivContactAvatar);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvContactNickname = itemView.findViewById(R.id.tvContactNickname);
            tvOnlineStatus = itemView.findViewById(R.id.tvOnlineStatus);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onContactClick(contacts.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onContactLongClick(contacts.get(position));
                        return true;
                    }
                }
                return false;
            });
        }

        public void bind(Contact contact) {
            // Display name or original nickname
            String displayName = contact.getDisplayNameOrOriginal();
            tvContactName.setText(displayName);
            tvContactNickname.setText("@" + contact.originalNickname);

            // Online status (temporarily stored in profileImagePath)
            boolean isOnline = "online".equals(contact.profileImagePath);
            
            if (isOnline) {
                tvOnlineStatus.setText("Online");
                tvOnlineStatus.setTextColor(itemView.getContext().getColor(R.color.accent_color));
                onlineIndicator.setVisibility(View.VISIBLE);
                onlineIndicator.setBackgroundTintList(
                    itemView.getContext().getColorStateList(R.color.accent_color)
                );
            } else {
                // Show last seen
                if (contact.lastSeenDate > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String lastSeen = sdf.format(new Date(contact.lastSeenDate));
                    tvOnlineStatus.setText("Last seen " + lastSeen);
                } else {
                    tvOnlineStatus.setText("Offline");
                }
                tvOnlineStatus.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                onlineIndicator.setVisibility(View.GONE);
            }

            // Set avatar (default person icon for now)
            ivContactAvatar.setImageResource(R.drawable.ic_person);
            if (isOnline) {
                ivContactAvatar.setBorderColor(itemView.getContext().getColor(R.color.accent_color));
            } else {
                ivContactAvatar.setBorderColor(itemView.getContext().getColor(R.color.text_secondary));
            }
        }
    }
}