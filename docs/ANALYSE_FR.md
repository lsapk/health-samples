# Analyse de l'application health-samples

## Ce que fait le dépôt

Ce dépôt n'est pas une seule application métier prête pour la prod : c'est une **collection de samples Android / Wear OS** autour des APIs santé (Health Connect, Health Services, Recording API, Health Platform v1). Il sert principalement de base pédagogique et de POC technique.

## Modules principaux

1. **Health Connect Sample (mobile Android)**
   - Gère les permissions Health Connect.
   - Lit et écrit des sessions d'exercice (avec données associées : pas, distance, calories, fréquence cardiaque).
   - Lit/génère des sessions de sommeil.
   - Lit/écrit des mesures de poids.
   - Gère les changements différentiels (change tokens).

2. **Exercise Sample Compose (Wear OS)**
   - Démarre/arrête/pause/reprend une session sportive via `ExerciseClient`.
   - Affiche métriques live (durée, HR, calories, distance, tours).
   - Utilise un service foreground + notification persistante.
   - Gère des objectifs de séance.

3. **Recording API on mobile Sample (mobile Android)**
   - Souscrit à la collecte locale de pas.
   - Lit les données en brut et en agrégé sur plage temporelle.

4. **Health Platform v1 Sample (legacy Samsung)**
   - Démo de lecture/écriture de sessions historiques via `HealthDataClient`.
   - À considérer comme legacy par rapport à Health Connect.

## Limites actuelles (produit)

- Pas de backend, pas de synchronisation cloud, pas de multi-device.
- Pas d'authentification utilisateur applicative.
- Pas de tableau de bord longitudinal (tendances hebdo/mensuelles, corrélations sommeil/perf, etc.).
- Données d'exemple en partie générées aléatoirement (utile pour démo, pas pour usage réel).
- Pas de mécanisme de qualité/validation des données de capteurs.
- Pas de planification d'entraînement personnalisée ni moteur de recommandations.

## Améliorations fonctionnelles indispensables

1. **Compte utilisateur + synchronisation cloud** (Supabase Auth + Postgres)
2. **Historique consolidé** (séances, sommeil, poids, objectifs, événements)
3. **Dashboard santé** (KPIs: charge d'entraînement, resting HR, progression)
4. **Objectifs intelligents** (règles adaptatives selon historique)
5. **Alertes utiles** (inactivité, dérive FC, dette de sommeil)
6. **Journal de consentement** (traçabilité RGPD/consentement)
7. **Qualité de données** (flags pour trous de mesures, sources contradictoires)
8. **Exports utilisateur** (CSV/JSON + suppression compte)
9. **Mode offline robuste + replay de sync**
10. **Partage coach/médecin** (liens temporaires avec périmètre limité)

## Mise en place demandée (dans le SQL)

Le fichier `docs/supabase_schema.sql` met en place la logique demandée en restant sur le même socle relationnel :

- **Dashboard santé**: table `dashboard_daily_kpis` + vue `v_dashboard_daily_kpis_live`.
- **Objectifs intelligents**: tables `goal_rules` et `goal_recommendations`.
- **Alertes utiles**: tables `alert_rules` et `alert_events`.
- **Mode offline + replay de sync**: tables `sync_queue` et `sync_checkpoints`.
- **Journal de consentement**: enrichissement de `consent_events` (`purpose`, `lawful_basis`, `revoked_at`).
- **Qualité de données**: table `data_quality_flags`.
- **Sécurité**: RLS par utilisateur sur toutes les nouvelles tables + triggers `updated_at`.

## Proposition de schéma Supabase

Voir `docs/supabase_schema.sql`.
