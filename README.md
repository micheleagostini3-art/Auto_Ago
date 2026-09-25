# Auto Ago

App Android per registrare i rifornimenti del proprio veicolo.

## Funzioni principali
- inserimento dei dati del rifornimento in una schermata ordinata e moderna;
- data e ora automatiche;
- KM, carburante, prezzo al litro, importo pagato e note;
- tabella archivio in ordine decrescente:
  - più recente in alto;
  - colonna aggiuntiva per il calcolo del valore `KM / €`;
- pulsante per salvare l'archivio in un file TXT;
- possibilità di scegliere la cartella di destinazione con il selettore file di Android.

## Dove si salva il file TXT
Il file viene creato in memoria privata dell'app (`filesDir`) e aggiornato ad ogni salvataggio.
Per salvarlo in una cartella visibile come Download:
1. aprire la pagina di archivio;
2. premere `ESPORTA TXT`;
3. scegliere `Download` o un'altra cartella.

## Formula del calcolo KM / €
Il valore mostrato in archivio è calcolato così:
- `(KM attuale - KM precedente) / importo pagato`

Questa formula dà i chilometri percorsi per 1 euro.

## Come creare l'APK
Aprire il repository con Android Studio oppure usare il workflow GitHub Actions presente in `.github/workflows/build-apk.yml` e scaricare l'APK generato.
