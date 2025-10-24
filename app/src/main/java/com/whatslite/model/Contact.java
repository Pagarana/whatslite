package com.whatslite.model;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room tablosu: contacts
 * - originalNickname : Kişinin gerçek (Firebase) takma adı, tercihen lowercase saklanır
 * - displayName      : Kullanıcının verdiği özel isim (nullable)
 * - language         : Kişinin dili (nullable, Firebase'den okunabilir)
 * - profileImagePath : Profil resmi yolu (nullable)
 * - isBlocked        : Engellenmiş mi?
 * - addedDate        : Eklenme zamanı (epoch millis)
 * - lastSeenDate     : Son görülme (epoch millis)
 * - pinnedRank       : Sabitleme sırası (1..20), 0 = sabit değil
 *
 * Not: ContactDao, originalNickname üzerinden işlem yapar.
 */
@Entity(tableName = "contacts")
public class Contact {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Firebase’deki gerçek nickname (tercihen normalize/lowercase) */
    public String originalNickname;

    /** Kullanıcının verdiği özel isim (opsiyonel) */
    @Nullable
    public String displayName;

    /** Kişinin dili (opsiyonel) */
    @Nullable
    public String language;

    /** Profil resmi yolu (opsiyonel) */
    @Nullable
    public String profileImagePath;

    /** Engellenmiş mi? */
    public boolean isBlocked = false;

    /** Eklenme tarihi (epoch millis) */
    public long addedDate = 0L;

    /** Son görülme (epoch millis) */
    public long lastSeenDate = 0L;

    /** 1..20 arası sabitleme sırası; 0 = sabit değil */
    public int pinnedRank = 0;

    /** Room için boş ctor */
    public Contact() {}

    @Ignore
    public Contact(String originalNickname) {
        this.originalNickname = normalizeNickname(originalNickname);
        this.addedDate = System.currentTimeMillis();
        this.lastSeenDate = this.addedDate;
    }

    @Ignore
    public Contact(String originalNickname, @Nullable String displayName, @Nullable String language) {
        this.originalNickname = normalizeNickname(originalNickname);
        this.displayName = isEmpty(displayName) ? null : displayName;
        this.language = isEmpty(language) ? null : language;
        this.isBlocked = false;
        this.addedDate = System.currentTimeMillis();
        this.lastSeenDate = this.addedDate;
        this.pinnedRank = 0;
    }

    /** UI’de isim gösterimi için yardımcı */
    public String getDisplayNameOrOriginal() {
        return isEmpty(displayName) ? originalNickname : displayName;
    }

    /** Dışarıdan güvenli set için küçük yardımcılar */
    public static String normalizeNickname(String nick) {
        if (nick == null) return "";
        String n = nick.trim();
        if (n.startsWith("@")) n = n.substring(1);
        return n.toLowerCase();
    }

    private static boolean isEmpty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }
}
