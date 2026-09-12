# MosquePrayerTV

> Application Android TV pour l'affichage des horaires de prière dans les mosquées, avec compte à rebours en temps réel, hadiths automatiques et support de l'orientation d'écran.

![screenshot](./screenshots/preview.png)

## 🚀 Stack technique

![Android](https://img.shields.io/badge/Android-34-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Gradle](https://img.shields.io/badge/Gradle-8.2.2-blue)
![Min SDK](https://img.shields.io/badge/Min%20SDK-21-yellow)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34-green)

- **Langage** : Kotlin
- **Architecture** : MVVM avec ViewModel et LiveData
- **UI** : ViewBinding, RecyclerView, Material Design
- **Calcul des prières** : Bibliothèque Adhan (Umm al-Qura)
- **Compatibilité** : Android TV (Leanback) et mobile

## 📋 Prérequis

- Android Studio Hedgehog ou supérieur
- JDK 17
- Android SDK 34
- Gradle 8.2.2

## ⚙️ Installation

```bash
# 1. Cloner le repo
git clone https://github.com/ton-username/MosquePrayerTV.git
cd MosquePrayerTV

# 2. Ouvrir le projet dans Android Studio
# File > Open > sélectionner le dossier du projet

# 3. Synchroniser Gradle
# Android Studio proposera automatiquement de synchroniser Gradle
# Sinon : File > Sync Project with Gradle Files

# 4. Lancer le projet
# Connecter un appareil Android ou lancer l'émulateur
# Clic sur le bouton "Run" (triangle vert) ou Shift+F10
```

## ✨ Fonctionnalités

- **Horaires de prière précis** : Calcul basé sur la méthode Umm al-Qura avec support des décalages personnalisés
- **Compte à rebours en temps réel** : Affichage du temps restant jusqu'à la prochaine prière et l'Iqama
- **Support multi-villes** : Base de données des villes saoudiennes avec recherche
- **Hadiths automatiques** : Rotation des hadiths selon différents modes (manuel, chaque prière, horaire)
- **Calendrier Hijri** : Affichage des dates grégoriennes et hégiriennes en arabe
- **Mode simulation** : Possibilité de sélectionner une date et une heure pour tester l'affichage
- **Orientation flexible** : Support du retournement d'écran (portrait / portrait inversé)
- **Background dynamique** : Image de fond différente le vendredi selon l'heure
- **Athkar après prière** : Affichage plein écran des athkar 7 minutes après l'Iqama
- **Format 12h/24h** : Choix du format d'affichage de l'heure
- **Activation par code** : Système d'activation avec codes personnalisés
- **Iqama configurable** : Délais d'Iqama personnalisables pour chaque prière

## 🌐 Démo live

En cours de déploiement

## 👤 Auteur

**Mohamed Aziz Chaabani**  
Portfolio : https://portfolio-chaabeni-mohamed-aziz.netlify.app  
GitHub : https://github.com/medaziz701

## 📝 Structure du projet

```
app/src/main/java/com/mosque/prayer/
├── data/              # Modèles et repositories
│   ├── AppPreferences.kt       # Gestion des préférences
│   ├── CitiesRepository.kt     # Repository des villes
│   ├── City.kt                 # Modèle de ville
│   └── Prayer.kt               # Modèles de prière
├── ui/                # Activités et vues
│   ├── MainActivity.kt         # Écran principal
│   ├── SettingsActivity.kt     # Écran des paramètres
│   ├── *Dialog.kt              # Dialogues de configuration
│   └── PrayerRowAdapter.kt     # Adapter pour la liste des prières
├── utils/             # Utilitaires
│   ├── PrayerTimesCalculator.kt    # Calcul des horaires
│   ├── TimeFormatUtils.kt          # Formatage des heures
│   └── HijriCalendarUtils.kt      # Calendrier hégirien
└── viewmodel/         # ViewModels
    └── MainViewModel.kt           # ViewModel principal
```

## 📄 Licence

Ce projet est propriétaire. Tous droits réservés.
