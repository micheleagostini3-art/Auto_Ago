# Auto Ago

App Android per registrare i rifornimenti dell'auto.

## Funzioni
- Inserimento data/ora automatiche, KM, carburante (metano, benzina, gasolio), prezzo al litro, importo e note.
- Archivio ordinato dal più recente al più vecchio.
- Calcolo dei KM percorsi per 1 €: `(KM rifornimento corrente - KM rifornimento precedente) / importo`.
- Pulsante **Salva TXT / aggiorna archivio**: crea o aggiorna `rifornimenti.txt` nella directory privata dell'app (`filesDir`).

Aprire il progetto con Android Studio e generare l'APK con **Build > Build APK(s)**.
