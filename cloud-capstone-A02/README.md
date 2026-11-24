# GearRent A02

Justin Joshia Banjarnahor (2410512133), Muhammad Kevin Rasendriya (2210512104),
Dhaifulloh Raihan Zayyan (2410512129), Muhammad Zaidan (2410512043).

[09_evidence.jpg](09-capacity-finops/09_evidence.jpg) adalah screenshot terminal
asli dari demo Muhammad Kevin Rasendriya pada 23 November 2026, pukul 21.04 WIB.
Source workload 4987995, satu replika Kubernetes. Uji baca inventaris dengan
8 worker, durasi konfigurasi 60 detik, menghasilkan 480/480 HTTP 200,
p95 client 78 ms, maksimum 266 ms, dan laju aktual 8,0146 request/detik.
Puncak dari empat sampel Metrics API adalah CPU 359,26m dan RAM 254,98MiB.

Screenshot juga menunjukkan resource aktif dan model biaya ilustratif.
Tarif dan volume 100.000 request sukses/bulan adalah asumsi. Request memory
320Mi merupakan usulan yang belum diterapkan. JWT, beban tulis, dan migrasi
CloudStack masih memerlukan pengujian lanjutan.

[Metode dan cara menjalankan](../docs/capacity/README.md),
[analisis serta catatan demo](../docs/capacity/REPORT.md), dan
[data run demo](../docs/capacity/results/2026-11-23/live-demo-60s/results.json).
Folder ini memuat kontribusi 09 untuk digabungkan ke paket kelompok.
