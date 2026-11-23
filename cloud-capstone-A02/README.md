# GearRent A02

Justin Joshia Banjarnahor (2410512133), Muhammad Kevin Rasendriya (2210512104),
Dhaifulloh Raihan Zayyan (2410512129), Muhammad Zaidan (2410512043).

[09_evidence.jpg](09-capacity-finops/09_evidence.jpg) memuat hasil baseline
Capacity/FinOps pada 23 November 2026, 20.28-20.33 WIB. Source workload 4987995,
satu replika GearRent di k3d. Fase idle 60 detik, lalu konfigurasi beban baca
2 dan 8 worker masing-masing 120 detik dengan batas satu request/detik/worker.
Seluruh 240 dan 949 request berhasil, p95 client 47 ms pada kedua fase.
Latency maksimum fase 8 worker mencapai 3.875 ms.

Puncak sampel CPU 214,31m dan RAM 242,55MiB. Usulan request memory 320Mi
belum diterapkan. Hasil memakai header demo. Pengujian JWT, transaksi tulis,
dan migrasi CloudStack masih perlu dilanjutkan. Nilai biaya pada gambar memakai
asumsi tarif ilustratif dan volume 100.000 request sukses/bulan.

[Metode dan cara menjalankan](../docs/capacity/README.md),
[worksheet dan analisis](../docs/capacity/REPORT.md), serta
[data mentah](../docs/capacity/results/2026-11-23) tersedia untuk pemeriksaan.
Folder pada branch ini memuat kontribusi 09 untuk digabungkan ke paket kelompok.
