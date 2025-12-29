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

Lo studio valuta empiricamente se l’impiego di tecniche di **feature selection**, **balancing** e **classificazione cost-sensitive** consenta di migliorare l’accuratezza dei modelli. L’identificazione preventiva delle classi ad alto rischio permette ai team di testing di prioritizzare le attività di verifica, ottimizzando le risorse disponibili.

L’analisi è condotta su due progetti open-source, **Apache BookKeeper** e **Apache Storm**, mediante dati storici estratti da **GitHub** e **Jira**, e il labeling delle classi è ottenuto attraverso la strategia **Proportion (Increment)**. La valutazione segue un approccio di **walk-forward validation**, al fine di simulare un contesto d’uso realistico nel tempo.

I risultati prodotti dai modelli vengono generati nel formato **ACUME**, per consentire l’analisi quantitativa tramite il relativo tool.  
Successivamente, gli output sono stati visualizzati mediante boxplot tramite un apposito **Visualizzatore di Risultati**, sviluppato separatamente e reso disponibile su un repository dedicato.

I modelli di Machine Learning considerati sono:

- **Random Forest**
- **Naive Bayes**
- **IBK**

Per ciascun classificatore sono state confrontate configurazioni che combinano:

- feature selection (**nessuna selezione** vs **Best-First**)
- balancing (**no sampling** vs **SMOTE**)
- approcci cost-sensitive (**no cost-sensitive** vs **cost-sensitive**, con configurazione *CFN = 10 × CFP*)

Gli esperimenti sono stati implementati tramite le API di **WEKA**, garantendo automazione e riproducibilità dei risultati.  
Il codice è conforme ai quality gate di **SonarCloud**, risultando privo di bug e code smells.

---

## 📁 Struttura del Repository

- `MLforSE/` → Codice sorgente
- `Report/` → Presentazione  

---

## 🔗 Riferimenti

- [ACUME](https://github.com/jonidacarka/ACUME.git)  
- [Visualizzatore dei risultati](https://github.com/MatteoBasili/isw2-visualizzatore-risultati-progetto-pm-2024_25)  
