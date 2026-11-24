# 09 Capacity dan FinOps

Muhammad Kevin Rasendriya, NIM 2210512104. Group A02.

Bagian ini mengukur resource GearRent yang sedang berjalan, lalu menghitung
headroom dan model biaya. Hasil ada di [REPORT.md](REPORT.md), data mentah
di [results/2026-11-23](results/2026-11-23), dan bukti visual di
[09_evidence.jpg](../../cloud-capstone-A02/09-capacity-finops/09_evidence.jpg).

## Persiapan

Python 3.10+, kubectl, dan akses kubeconfig cluster diperlukan untuk pengukuran.
Metrics API harus tersedia. Rendering JPG memakai Pillow dengan font Arial
Windows. Tidak perlu memasang library Python tambahan untuk collector.

Koordinasikan waktu dengan pemilik 06 dan 08. Jangan deployment, mengganti image,
atau menghapus pod selama run. Resource dan UID pod diperiksa sebelum/sesudah.
Output folder harus baru. Skrip menolak menimpa hasil yang sudah ada.

Contoh PowerShell untuk lingkungan lokal pengujian ini.

Contoh header demo di bawah hanya berlaku untuk runtime baseline 4987995.
Main terbaru sudah memakai JWT. Untuk runtime versi main, gunakan token-file
seperti bagian setelah security dan isi commit workload yang benar.

```powershell
$kube = 'C:\Users\kevin\Documents\CloudComputing\GearRent-A02-lab\kubeconfig.yaml'
kubectl --kubeconfig $kube --context k3d-gearrent-a02 -n gearrent-latest top pods

$common = @(
    '--kubeconfig', $kube,
    '--source', '4987995c0332de16c9953ef45f23eab5e2448433',
    '--executor', 'Sesi lokal pada laptop Muhammad Kevin Rasendriya',
    '--demo-headers',
    '--sample-seconds', '15'
)

python scripts/capacity.py @common --workers 0 --seconds 60 --out run-baru/idle
python scripts/capacity.py @common --workers 2 --seconds 120 --out run-baru/load-2
python scripts/capacity.py @common --workers 8 --seconds 120 --out run-baru/load-8
```

Isi `--source` dengan commit source image yang benar. Periksa digest deployment
sebelum menjalankan ulang. Namespace, label, Deployment, URL, dan Host dapat
diganti melalui opsi skrip. Jalankan `python scripts/capacity.py --help`.

RPS yang diatur adalah batas satu request/detik/worker. Skrip memakai closed-loop,
sehingga layanan lambat menurunkan RPS aktual. Kegagalan transport dicatat sebagai
status 0. Respons 4xx/5xx tetap menjadi percobaan gagal. Semua endpoint bersifat
read-only. Distribusi 75/25 berlaku per rangkaian request, dengan pembulatan pada
akhir worker. Sampel Metrics API menyertakan timestamp dan window aslinya.

## Setelah security selesai

Ganti `--demo-headers` dengan `--token-file` yang menunjuk file token lokal.
Isi file adalah access token yang sudah diterbitkan Keycloak untuk tenant/role sah.
Skrip tidak mencetak atau menyimpan token. Simpan file token di luar repo.
Gunakan masa berlaku yang mencakup seluruh fase. Token kadaluarsa akan menghasilkan
401 dan tetap dihitung sebagai kegagalan. Isi versi source yang baru dan jalankan
ketiga fase ke folder baru.

```powershell
python scripts/capacity.py --kubeconfig $kube --source COMMIT_IMAGE_TERBARU `
    --executor 'Sesi pengujian lokal kelompok A02' --token-file C:\private\access-token.txt `
    --workers 2 --seconds 120 --out run-jwt/load-2
```

## Bangun worksheet dan JPG

Pastikan tiga folder idle, load-2, dan load-8 berada di direktori hasil yang sama.

```powershell
python scripts/capacity_report.py --results docs/capacity/results/2026-11-23
python -m unittest discover -s tests -p test_capacity.py
```

Generator menyimpan gambar ringkasan ke results/2026-11-23/baseline-summary.jpg.
File pengumpulan 09_evidence.jpg tetap berasal dari screenshot demo aktual.
Jalankan generator menggunakan tiga fase baseline. Catatan demo 60 detik pada
REPORT.md ditambahkan terpisah dan perlu dipertahankan saat menyusun laporan final.

Ubah cost-assumptions.json untuk mengganti tarif dan volume. Semua nilai tarif
default merupakan asumsi ilustratif, belum mewakili penawaran penyedia cloud.
Jangan mengubah hasil aktual untuk mengikuti target. Jika hasil tidak memenuhi
target, catat hasilnya dan keputusan perbaikannya.

## Cara menjelaskan saat presentasi

"Bagian saya memeriksa kebutuhan resource GearRent. Kami membandingkan kondisi
idle dengan dua tingkat beban baca. Selain jumlah request dan latency, kami
mengambil CPU serta RAM pod setiap 15 detik. Dari hasilnya, kami menghitung sisa
kapasitas terhadap limit dan meninjau request yang dipakai scheduler. Ukuran
resource usulan perlu diuji lagi dengan login JWT dan transaksi tulis. Model
biaya memakai tarif asumsi yang ditampilkan, sehingga angkanya bisa dihitung ulang."

Tunjukkan tabel hasil di REPORT.md dan JPG. Sebutkan commit workload, autentikasi
yang digunakan, durasi uji, RPS aktual, dan batas kesimpulannya. Pekerjaan ini
belum menguji migrasi ke CloudStack atau menerapkan usulan rightsizing.
