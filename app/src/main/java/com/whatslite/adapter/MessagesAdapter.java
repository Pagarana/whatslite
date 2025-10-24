package com.whatslite.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.whatslite.R;
import com.whatslite.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tekrarlı mesajı engellemek için Firebase pushKey ile upsert yapar.
 * addOrUpdateWithKey(key, message) KULLAN!
 *
 * item_message_incoming.xml ve item_message_outgoing.xml içinde
 * en azından tvText ve tvTime ID'leri olmalı.
 */
public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface MessageActionListener { void onMessageLongClick(Message m, View anchor); }

    private static final int VT_OUT = 1;
    private static final int VT_IN  = 2;

    private final List<Message> data = new ArrayList<>();
    private final List<String> keys  = new ArrayList<>(); // Firebase push key paralel dizisi
    private final String myUid;
    private final MessageActionListener listener;
    private final SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessagesAdapter(String myUid, MessageActionListener l) {
        this.myUid = myUid;
        this.listener = l;
    }

    /** Aynı key tekrar edilirse günceller, yoksa ekler. */
    public void addOrUpdateWithKey(@NonNull String key, @NonNull Message m) {
        int idx = keys.indexOf(key);
        if (idx >= 0) {
            data.set(idx, m);
            notifyItemChanged(idx);
        } else {
            keys.add(key);
            data.add(m);
            notifyItemInserted(data.size() - 1);
        }
    }

    @Override public int getItemViewType(int position) {
        Message m = data.get(position);
        return (m.senderId != null && m.senderId.equals(myUid)) ? VT_OUT : VT_IN;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (vt == VT_OUT) {
            View v = inf.inflate(R.layout.item_message_outgoing, parent, false);
            return new OutVH(v);
        } else {
            View v = inf.inflate(R.layout.item_message_incoming, parent, false);
            return new InVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        Message m = data.get(pos);
        String time = tf.format(new Date(m.timestamp == 0L ? System.currentTimeMillis() : m.timestamp));

        if (h instanceof OutVH) {
            OutVH vh = (OutVH) h;
            vh.tvText.setText(m.text);
            vh.tvTime.setText(time);
            vh.itemView.setOnLongClickListener(v -> { if (listener!=null) listener.onMessageLongClick(m, v); return true; });
        } else {
            InVH vh = (InVH) h;
            vh.tvText.setText(m.text);
            vh.tvTime.setText(time);
            vh.itemView.setOnLongClickListener(v -> { if (listener!=null) listener.onMessageLongClick(m, v); return true; });
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class OutVH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        OutVH(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tvText);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
    static class InVH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        InVH(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tvText);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
