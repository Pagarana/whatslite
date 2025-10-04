# Firebase Debug Checklist

## Firebase Console Kontrol:

### 1. Database URL'si:
- Console'da Database Overview'da URL gösterilmeli
- Örnek: `https://whatslite-4377b-default-rtdb.firebaseio.com/`

### 2. Data sekmesinde kontrol:
```
whatslite-4377b-default-rtdb/
├── users/ (bu klasör var mı?)
├── messages/
└── chatRooms/
```

## Uygulama Debug:

### LogCat'te aranacak loglar:
- `FirebaseManager`: Firebase bağlantı durumu
- `Firebase connection: true/false`
- `User joined successfully`
- `User list data changed`

### Test Adımları:
1. İlk telefonda app açılır
2. Nickname gir (örn: "test1")
3. LogCat'te "✅ User joined successfully" log'u olmalı
4. Firebase Console Data sekmesinde users/test1 görülmeli
5. İkinci telefonda app açılır
6. Farklı nickname gir (örn: "test2")
7. İlk telefonda test2 kullanıcısı görülmeli

## Olası Sorunlar:

1. **İnternet bağlantısı**
2. **Firebase Rules yanlış**
3. **Database henüz oluşturulmamış**
4. **SHA-1 fingerprint eksik**
5. **google-services.json yanlış**