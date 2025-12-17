# 💡 Progetto ISW2 – A.A. 2024/2025

**Corso:** Ingegneria del Software 2 [ISW2]  
**Studente:** Matteo Basili  
**Professore:** Davide Falessi  

---

[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=MatteoBasili_isw2-progetto-project_management-2024_25)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=MatteoBasili_isw2-progetto-project_management-2024_25&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=MatteoBasili_isw2-progetto-project_management-2024_25)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=MatteoBasili_isw2-progetto-project_management-2024_25&metric=bugs)](https://sonarcloud.io/summary/new_code?id=MatteoBasili_isw2-progetto-project_management-2024_25)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=MatteoBasili_isw2-progetto-project_management-2024_25&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=MatteoBasili_isw2-progetto-project_management-2024_25)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=MatteoBasili_isw2-progetto-project_management-2024_25&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=MatteoBasili_isw2-progetto-project_management-2024_25)

---

## 📝 Descrizione

Il progetto ha l’obiettivo di analizzare la fattibilità dell’adozione di **modelli di Machine Learning per la predizione di classi buggy**, al fine di supportare e migliorare la fase di testing di grandi applicazioni open-source.

Lo studio si propone di valutare empiricamente se l’impiego di tecniche di **feature selection**, **bilanciamento dei dati** e **classificazione cost-sensitive** consenta di aumentare l’accuratezza dei modelli predittivi. L’idea alla base del lavoro è che l’identificazione preventiva delle classi con maggiore probabilità di contenere difetti possa aiutare i team di testing a prioritizzare le attività di verifica e a ottimizzare l’uso delle risorse disponibili.

**(DA AGGIORNARE ALLA FINE)** L’analisi è condotta su due progetti open-source, **Apache BookKeeper** e **Apache Storm**, utilizzando dati storici estratti da **GitHub** e **Jira**. L’identificazione delle classi buggy avviene tramite tecniche di SZZ-linking, mentre il labeling dei dati è effettuato mediante la strategia **Proportion (any)**. La valutazione dei modelli segue un approccio di **walk-forward validation**, in modo da simulare uno scenario realistico di utilizzo nel tempo.

I modelli di Machine Learning presi in considerazione sono:

- Random Forest
- Naive Bayes
- IBK

Per ciascun classificatore vengono confrontate diverse configurazioni che combinano **(DA AGGIORNARE ALLA FINE)**:

- feature selection (nessuna selezione vs Best First),
- tecniche di bilanciamento dei dati (no sampling, over-sampling, under-sampling e SMOTE),
- approcci cost-sensitive (sensitive threshold e sensitive learning).

Gli esperimenti sono implementati utilizzando le API di **WEKA**, al fine di garantire automazione e riproducibilità dei risultati.
Il codice del progetto rispetta i quality gate di **SonarCloud**, risultando privo di code smells.

---

## 📁 Struttura del Repository

- `MLforSE/` → Codice sorgente
- `Report/` → Presentazione  
