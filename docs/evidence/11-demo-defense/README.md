# 11 — Demo Script & Technical Defense Preparation (FINAL)

## Evidence Metadata

| Field | Detail |
| :--- | :--- |
| **Scope** | Urutan demonstrasi end-to-end 20–25 menit (13 poin wajib Guideline Section 13) dan kesiapan tanya-jawab individu (12 pertanyaan bank soal Section 13.1) untuk seluruh 4 anggota |
| **Owner** | Kelompok 2 (Kevin, Zaidan, Raihan, Justin) |
| **Source / Version** | Mengacu pada `guides/Cloud_Computing_Final_Capstone_Project_Guideline.docx` (Section 12, 13, 15, 16) dan `TESTING.md` |
| **Timestamp** | Kerangka disusun 27 November 2026 (siap dilengkapi skrip & refleksi final) |
| **Method** | Pemetaan skenario live demo dari kasus bisnis -> transaksi OIDC -> persistensi DB -> k3d -> CI/GitOps -> negative auth & Trivy -> Grafana SLO -> delete pod & restore -> capacity/FinOps -> risiko & roadmap |
| **Observation** | Seluruh komponen teknis M2–M8 telah terintegrasi di monorepo dan dapat diuji melalui perintah di `TESTING.md` |
| **Interpretation** | Demo berbasis skenario live membuktikan sistem benar-benar berjalan, bukan sekadar presentasi slide |
| **Limitation** | Dokumen pendukung final berikut **belum dibuat** dan harus disiapkan sebelum pengumpulan akhir: `final-report.pdf` (12 bab), skrip pembagian pembicara final, serta `individual-reflections/` untuk 4 anggota |
| **Decision Supported** | Memastikan keempat anggota menguasai arsitektur dan kontrol wajib secara merata untuk defense individu |

## Checklist Kelengkapan Final (Guideline Section 15 & 16)
- [ ] `final-report.pdf` (12 bab sesuai Section 15.1)
- [ ] Skrip demo 20–25 menit + pembagian peran 4 anggota (13 langkah Section 13)
- [ ] `individual-reflections/` untuk Kevin, Zaidan, Raihan, dan Justin + log kontribusi tim
- [ ] Lengkapi bukti live di `06-security/` dan `08-reliability-recovery/`
