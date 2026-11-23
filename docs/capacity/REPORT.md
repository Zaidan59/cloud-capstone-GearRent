# Capacity dan FinOps GearRent A02

Muhammad Kevin Rasendriya, NIM 2210512104. Group A02.

## Versi dan metode

Pengujian lokal berbantuan otomasi pada laptop Muhammad Kevin Rasendriya.
RAM 16 GB. Window 23-11-2026 20:28:13 sampai 20:33:16 WIB.
Source workload `4987995c0332de16c9953ef45f23eab5e2448433`. Commit skrip pengukuran `d9545bf32d1a5adbecf1df8bf7a27d5849ead48d`.
Image aktif `registry:5000/student/capstone-app@sha256:2e71b51b16b1e89934695137bef1f692c0ee4c1c7ee1d243b4c4f549863b5ddc`. Cluster `k3d-gearrent-a02`, namespace
`gearrent-latest`, Deployment `gearrent`, satu replika.

Ini baseline aplikasi yang sudah terpasang dengan `trust-headers=true`.
Integrasi JWT dan versi terbaru dari anggota lain perlu diukur ulang.
JSON menyimpan UID pod, imageID, resource, respons per request, timestamp, dan sampel Metrics API.
Token, password, serta body transaksi tidak disimpan dalam evidence.

Workload browsing read-only menggunakan 75% GET `/api/equipments` dan 25% GET
`/api/equipments/available`. Dua fase load masing-masing berdurasi 120 detik,
dengan 2 dan 8 worker. Setiap worker dibatasi satu request/detik, closed-loop.
Respons lambat menurunkan laju aktual. Angka worker adalah concurrency client,
dan tidak boleh ditafsirkan sebagai jumlah pengguna maksimum.

Idle diukur selama 60 detik. Metrics API diambil setiap 15 detik dan memiliki
window averaging sendiri. Puncak CPU di bawah adalah maksimum sampel rata-rata
window, sehingga lonjakan singkat dapat terlewat. Memory dari Metrics API adalah
working set. Sampel idle pertama dapat dipengaruhi warm-up pilot sebelumnya.
Satu GET inventaris dijalankan sebagai preflight di luar hitungan beban.
Fase dijalankan berurutan tanpa reset pod dan tanpa cooldown khusus.
Percentile memakai nearest rank. Latency client mencakup jaringan lokal, ingress,
API, database, dan pembacaan body. Perhitungan p95 tabel hanya memakai respons 2xx.
Status non-2xx dan kegagalan transport tetap masuk denominator seluruh percobaan.

## Worksheet hasil aktual

| Fase | Window aktual s | Attempt | 2xx | RPS aktual | p95 client ms | CPU max m | RAM max MiB | Sampel |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Idle | 60.0 | 0 | 0 | 0.00 | - | 114.3 | 229.1 | 4 |
| 2 worker | 119.5 | 240 | 240 | 2.01 | 47.0 | 39.0 | 223.2 | 8 |
| 8 worker | 119.8 | 949 | 949 | 7.92 | 47.0 | 214.3 | 242.5 | 8 |

Fase 8 worker menghasilkan 100.00% respons 2xx.
Terhadap kandidat target p95 client 2 detik, hasil fase tersebut memenuhi target.
Latency client tertinggi fase tersebut mencapai 3875.0 ms,
sehingga percentile yang memenuhi target tidak menjamin semua request di bawah 2 detik.
Target ini pembanding uji lokal. Hasil belum membuktikan kapasitas produksi,
beban tulis reservasi, upload foto, throughput maksimum, atau target SLO bulanan.

## Headroom dan keputusan resource

Resource aktif per pod adalah request CPU 100m / RAM 192Mi,
limit CPU 500m / RAM 512Mi.

| Ukuran | CPU | RAM |
|---|---:|---:|
| Maksimum sampel seluruh fase | 214.31m | 242.55MiB |
| Pemakaian terhadap request | 214.31% | 126.33% |
| Sisa terhadap limit | 285.69m | 269.45MiB |
| Headroom terhadap limit | 57.14% | 52.63% |

Rumus headroom adalah `(limit - maksimum sampel) / limit * 100%`.
Request mengatur reservasi kapasitas scheduling. Penggunaan di atas request
masih diperbolehkan selama kondisi node memungkinkan dan limit belum tercapai.
Headroom pod tidak sama dengan kapasitas bebas seluruh node.

Usulan pengujian berikutnya adalah request memory **320Mi**,
dihitung dari maksimum working set x 1,25 lalu dibulatkan ke kelipatan 64Mi.
Pertahankan request CPU 100m dan limits 500m/512Mi untuk uji ulang.
Usulan memory ini masih berada di bawah limit aktif.
Usulan belum diterapkan ke manifest agar baseline dan hasil tetap konsisten.
Mengubah request pod tidak langsung mengubah tagihan VM berukuran tetap.
Penghematan membutuhkan perubahan offering, jumlah VM, atau konsolidasi yang diuji.
Keputusan akhir menunggu startup peak, uji write/upload, JWT, soak test lebih panjang,
CPU throttling, dan pemakaian database. Sampling ini tidak mengukur throttling.

## Model biaya ilustratif

Tarif berikut merupakan angka asumsi tugas, bukan harga vendor atau tagihan aktual.
Bandingkan dengan tarif resmi atau biaya internal lab sebelum dipakai untuk anggaran.
Model satu VM 2 vCPU, RAM 4 GiB,
disk 20 GiB, 720 jam/bulan.
Tarif CPU Rp1,000/vCPU-jam, RAM Rp100/GiB-jam,
storage Rp500/GiB-bulan.

`biaya = jam x (vCPU x tarif_CPU + GiB_RAM x tarif_RAM) + GiB_disk x tarif_disk + biaya_tetap`

Total model adalah **Rp1,738,000/bulan**.
Dengan asumsi 100,000 request sukses/bulan,
unit-cost adalah **Rp17,380/1.000 request sukses**.
Ini alokasi biaya infrastruktur bulanan terhadap volume asumsi, bukan tagihan per request.
Volume bulanan belum divalidasi dan tidak diekstrapolasi sebagai kapasitas maksimum.

| Asumsi request sukses/bulan | Model Rp per 1.000 request |
|---:|---:|
| 10.000 | 173,800 |
| 100.000 | 17,380 |
| 1.000.000 | 1,738 |

Tabel sensitivitas mengasumsikan VM yang sama, sehingga kecukupan kapasitas pada
setiap volume masih perlu diuji. Model tidak memasukkan Pajak, Egress, Backup, Operasional manusia, High availability, Registry, CI, dan observability terpisah.
Model VM 2 vCPU/4GiB merupakan kandidat penempatan k3s/aplikasi/database untuk lab,
bukan ukuran yang sudah dibuktikan di CloudStack. Pengukuran pod aplikasi tidak
mencakup semua komponen VM, DB, control plane, atau tooling delivery.

## Roadmap CloudStack

1. Inventaris kebutuhan seluruh node, PostgreSQL, disk, dan retensi. Satu node saat ini
   berbagi layanan lab sehingga metrik pod saja belum cukup memilih ukuran VM.
2. Pilih compute offering untuk CPU/RAM dan disk offering untuk penyimpanan persisten.
   Kandidat VM awal pada model biaya perlu dibuktikan dengan pengukuran seluruh stack.
3. Buat Linux VM, pasang k3s, deploy manifest aplikasi dengan DNS database dan Secret
   untuk lingkungan tersebut. Ganti IP Docker lokal, alamat registry, dan akses Ingress.
4. Simpan PostgreSQL pada data disk persisten. Uji backup/restore bersama pemilik 08.
   Penyimpanan foto perlu dipindahkan ke volume persisten atau object storage yang diuji.
5. Tetapkan firewall dan akses privat database, HTTPS, identitas OIDC, serta registry.
   Pisahkan tooling CI/observability pada perhitungan biaya bila ikut dimigrasikan.
6. Ulangi pengukuran beban dan pemulihan di VM. Sesuaikan offering dan budget berdasarkan
   hasil tersebut. CloudStack simulator tidak membuktikan performa VM nyata.

Pemetaan ini adalah rencana konseptual. Migrasi CloudStack belum dijalankan.
Dokumentasi compute/disk offerings menjelaskan pemisahan CPU/RAM dan disk.
[Apache CloudStack service offerings](https://docs.cloudstack.apache.org/en/latest/adminguide/service_offerings.html)

## Batas dan tindak lanjut

Hasil berlaku untuk dua endpoint baca, data demo kecil, satu pod, satu laptop,
dan kondisi lokal saat uji. Kesehatan cluster serta deployment dicek sebelum/sesudah.
Baseline tidak menyimpulkan concurrency maksimum atau efisiensi produksi.
Jangan menjalankan uji hapus pod/deployment bersamaan dengan pengukuran 09.
Ulangi fase yang sama dengan token valid setelah 06 selesai, dan catat commit/digest baru.
Jika JWT kedaluwarsa, request 401 tetap dihitung sebagai gagal dan harus dijelaskan.

Detail request dan metrik tersimpan di `results/2026-11-23/<fase>/results.json`.
JPG merupakan ringkasan visual dari data tersebut. Konfigurasi dasar Kubernetes
dan kode aplikasi tidak diubah oleh pekerjaan 09 ini.
