# ADB WiFi Bağlantı Scripti
# Telefonda gösterilen IP ve portları güncelleyin

$phoneIP = "192.168.248.XXX"  # Telefonunuzun IP adresini buraya girin
$pairingPort = "XXXXX"        # Telefonda gösterilen pairing port'unu girin
$connectPort = "5555"         # Genelde 5555 olur, telefonda farklı gösteriliyorsa güncelleyin

Write-Host "ADB WiFi Pairing başlatılıyor..." -ForegroundColor Green
Write-Host "IP: $phoneIP" -ForegroundColor Yellow
Write-Host "Pairing Port: $pairingPort" -ForegroundColor Yellow
Write-Host "Güvenlik Kodu: studio-JRT!kfFSno" -ForegroundColor Cyan

# Pairing işlemi
Write-Host "`nPairing komutu:" -ForegroundColor Magenta
Write-Host "& `"C:\Android\Sdk\platform-tools\adb.exe`" pair ${phoneIP}:${pairingPort}" -ForegroundColor White

# Bağlantı işlemi  
Write-Host "`nBağlantı komutu:" -ForegroundColor Magenta
Write-Host "& `"C:\Android\Sdk\platform-tools\adb.exe`" connect ${phoneIP}:${connectPort}" -ForegroundColor White

Write-Host "`nManuel olarak şu adımları takip edin:" -ForegroundColor Green
Write-Host "1. Telefonda Kablosuz ADB hata ayıklama'yı açın"
Write-Host "2. 'Cihazı eşleştirme kodu ile eşleştir' seçin"  
Write-Host "3. Telefonda gösterilen IP ve port bilgilerini yukarıdaki komutlarda güncelleyin"
Write-Host "4. Pairing komutunu çalıştırın"
Write-Host "5. İstendiğinde güvenlik kodunu girin: studio-JRT!kfFSno"
Write-Host "6. Pairing başarılı olduktan sonra connect komutunu çalıştırın"