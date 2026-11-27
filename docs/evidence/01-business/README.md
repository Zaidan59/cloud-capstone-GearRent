# 01 — Business Case & Cloud Rationale (M0)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Masalah bisnis, pemangku kepentingan, ruang lingkup MVP, dan alasan adopsi cloud untuk sistem **GearRent** (*Photo & Video Equipment Rental & Maintenance*) |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | [Kelompok 2_Capstone_104_043_129_133.pdf](../../proposal/Kelompok%202_Capstone_104_043_129_133.pdf), [README.md](../../../README.md) |
| **Timestamp** | Disusun Oktober 2026 (M0 Proposal), disinkronkan 27 November 2026 |
| **Method** | Pemetaan proses bisnis rental multimedia (reservasi -> alokasi unit -> serah-terima -> pengembalian -> inspeksi & maintenance), identifikasi 4 peran stakeholder, dan analisis kecocokan karakteristik cloud (*cloud suitability*) |
| **Observation** | Sistem mengatasi risiko bentrok jadwal sewa antar-petugas, pencatatan aksesori/kondisi sebelum-sesudah sewa yang terpisah, dan pelacakan kerusakan unit pada lebih dari satu usaha rental (`tenant-a`: Jakarta Camera Rental, `tenant-b`: Bandung Visual Gear). Domain dibatasi pada 6 entitas inti (`Tenant`, `EquipmentUnit`, `Reservation`, `ReservationItem`, `InspectionRecord`, `MaintenanceRecord`) dan mengecualikan pembayaran online, kurir, denda otomatis, dan prediksi kerusakan |
| **Interpretation** | Kasus membutuhkan database terpusat dengan kontrol transaksi konkuren, isolasi multi-tenant, penyimpanan lampiran foto terpisah dari runtime container, serta kemampuan deploy ulang dan backup/restore yang terukur—bukan sekadar hosting statis |
| **Limitation** | Data unit dan pelanggan awal memakai data contoh (*seed/demo*); lingkungan awal berjalan di kluster lokal satu laptop sehingga belum membuktikan ketahanan lintas host/region atau skema bisnis produksi penuh |
| **Decision Supported** | Menetapkan ruang lingkup MVP GearRent (6 entitas, 3 peran teknis `user`/`operator`/`admin`, 2 tenant contoh) dan menolak over-engineering fitur di luar inti alur rental-perawatan |

## Rujukan Dokumen Terkait
- Proposal lengkap: [Kelompok 2_Capstone_104_043_129_133.pdf](../../proposal/Kelompok%202_Capstone_104_043_129_133.pdf)
- Data seed awal di kode: [SeedDataInitializer.java](../../../app/src/main/java/id/ac/upnvj/gearrent/service/SeedDataInitializer.java)
