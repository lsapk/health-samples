-- Supabase/PostgreSQL schema proposal for health-samples productionization
-- Safe to run in Supabase SQL editor.

create extension if not exists "pgcrypto";

-- =========================================================
-- 1) Core profile and reference tables
-- =========================================================

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  birth_date date,
  sex text check (sex in ('female','male','other','prefer_not_to_say')),
  height_cm numeric(5,2),
  timezone text not null default 'UTC',
  units_system text not null default 'metric' check (units_system in ('metric','imperial')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.data_sources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  source_type text not null check (source_type in (
    'health_connect','health_services','recording_api','health_platform_v1','manual','import'
  )),
  app_package text,
  device_model text,
  platform text,
  external_source_id text,
  created_at timestamptz not null default now(),
  unique (user_id, source_type, coalesce(app_package, ''))
);

-- =========================================================
-- 2) Exercise domain
-- =========================================================

create table if not exists public.exercise_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  provider_session_id text,
  activity_type text not null default 'running',
  title text,
  notes text,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  duration_seconds integer generated always as (greatest(0, extract(epoch from (ended_at - started_at))::int)) stored,
  total_steps integer,
  total_distance_m numeric(12,2),
  total_energy_kcal numeric(10,2),
  avg_heart_rate_bpm numeric(6,2),
  max_heart_rate_bpm numeric(6,2),
  min_heart_rate_bpm numeric(6,2),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (ended_at >= started_at),
  unique (user_id, provider_session_id)
);

create index if not exists idx_exercise_sessions_user_started_at
  on public.exercise_sessions (user_id, started_at desc);

create table if not exists public.exercise_samples (
  id bigserial primary key,
  session_id uuid not null references public.exercise_sessions(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  sampled_at timestamptz not null,
  heart_rate_bpm numeric(6,2),
  speed_mps numeric(8,3),
  cadence_spm numeric(8,2),
  distance_m numeric(12,2),
  calories_kcal numeric(10,2),
  lap integer,
  created_at timestamptz not null default now(),
  unique (session_id, sampled_at)
);

create index if not exists idx_exercise_samples_user_time
  on public.exercise_samples (user_id, sampled_at desc);

-- =========================================================
-- 3) Sleep domain
-- =========================================================

create table if not exists public.sleep_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  provider_session_id text,
  notes text,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  duration_seconds integer generated always as (greatest(0, extract(epoch from (ended_at - started_at))::int)) stored,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (ended_at >= started_at),
  unique (user_id, provider_session_id)
);

create index if not exists idx_sleep_sessions_user_started_at
  on public.sleep_sessions (user_id, started_at desc);

create table if not exists public.sleep_stages (
  id bigserial primary key,
  sleep_session_id uuid not null references public.sleep_sessions(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  stage text not null check (stage in ('awake','light','deep','rem','unknown')),
  started_at timestamptz not null,
  ended_at timestamptz not null,
  created_at timestamptz not null default now(),
  check (ended_at >= started_at)
);

create index if not exists idx_sleep_stages_user_time
  on public.sleep_stages (user_id, started_at desc);

-- =========================================================
-- 4) Body metrics
-- =========================================================

create table if not exists public.weight_measurements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  measured_at timestamptz not null,
  weight_kg numeric(6,2) not null check (weight_kg > 0),
  bmi numeric(6,2),
  body_fat_pct numeric(5,2),
  lean_mass_kg numeric(6,2),
  created_at timestamptz not null default now(),
  unique (user_id, measured_at)
);

create index if not exists idx_weight_user_measured_at
  on public.weight_measurements (user_id, measured_at desc);

create table if not exists public.step_hourly (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  bucket_start timestamptz not null,
  bucket_end timestamptz not null,
  steps integer not null check (steps >= 0),
  source_granularity text not null default 'hour' check (source_granularity in ('hour','day','raw')),
  created_at timestamptz not null default now(),
  check (bucket_end > bucket_start),
  unique (user_id, bucket_start, bucket_end)
);

create index if not exists idx_step_hourly_user_bucket
  on public.step_hourly (user_id, bucket_start desc);

-- =========================================================
-- 5) Goals, consent, sync
-- =========================================================

create table if not exists public.user_goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  goal_type text not null check (goal_type in ('daily_steps','weekly_distance','weekly_workouts','sleep_duration','weight_target')),
  target_value numeric(12,2) not null,
  unit text not null,
  start_date date not null,
  end_date date,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (end_date is null or end_date >= start_date)
);

create index if not exists idx_user_goals_user_active
  on public.user_goals (user_id, is_active);

-- Smart goals: generation rules and recommendations.
create table if not exists public.goal_rules (
  id uuid primary key default gen_random_uuid(),
  goal_type text not null,
  rule_name text not null,
  config jsonb not null default '{}'::jsonb,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.goal_recommendations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  goal_type text not null,
  recommended_target numeric(12,2) not null,
  unit text not null,
  based_on_window_days integer not null default 28 check (based_on_window_days > 0),
  confidence_score numeric(4,3) check (confidence_score >= 0 and confidence_score <= 1),
  reason text,
  recommendation_data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  accepted_at timestamptz,
  dismissed_at timestamptz,
  check (accepted_at is null or dismissed_at is null)
);

create index if not exists idx_goal_recommendations_user_created
  on public.goal_recommendations (user_id, created_at desc);

create table if not exists public.consent_events (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  consent_type text not null,
  consent_version text not null,
  purpose text,
  lawful_basis text,
  granted boolean not null,
  event_at timestamptz not null default now(),
  revoked_at timestamptz,
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists idx_consent_events_user_time
  on public.consent_events (user_id, event_at desc);

create table if not exists public.sync_events (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  sync_type text not null check (sync_type in ('full','incremental','backfill')),
  status text not null check (status in ('started','success','error')),
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  records_in integer,
  records_out integer,
  error_message text,
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists idx_sync_events_user_started
  on public.sync_events (user_id, started_at desc);

-- Offline-first sync queue + replay.
create table if not exists public.sync_checkpoints (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete cascade,
  stream_name text not null,
  cursor_token text,
  cursor_time timestamptz,
  last_success_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, data_source_id, stream_name)
);

create table if not exists public.sync_queue (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  data_source_id uuid references public.data_sources(id) on delete set null,
  queue_type text not null check (queue_type in ('upsert','delete','checkpoint')),
  entity_type text not null,
  entity_id text,
  payload jsonb not null default '{}'::jsonb,
  dedupe_key text,
  status text not null default 'pending' check (status in ('pending','processing','done','failed','dead_letter')),
  attempt_count integer not null default 0,
  next_retry_at timestamptz,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  processed_at timestamptz,
  unique (user_id, dedupe_key)
);

create index if not exists idx_sync_queue_pending
  on public.sync_queue (user_id, status, created_at);

-- =========================================================
-- 6) Alerts + data quality + dashboard KPIs
-- =========================================================

create table if not exists public.alert_rules (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  rule_type text not null check (rule_type in ('inactivity','hr_drift','sleep_debt')),
  threshold_value numeric(12,2),
  threshold_unit text,
  lookback_hours integer not null default 24,
  enabled boolean not null default true,
  config jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_alert_rules_user_enabled
  on public.alert_rules (user_id, enabled);

create table if not exists public.alert_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  rule_id uuid references public.alert_rules(id) on delete set null,
  alert_type text not null,
  severity text not null default 'info' check (severity in ('info','warning','critical')),
  title text not null,
  message text,
  detected_at timestamptz not null default now(),
  acknowledged_at timestamptz,
  resolved_at timestamptz,
  context jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_alert_events_user_detected
  on public.alert_events (user_id, detected_at desc);

create table if not exists public.data_quality_flags (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  source_table text not null,
  source_record_id text not null,
  flag_type text not null check (flag_type in ('missing_data','outlier','conflict','duplicate','late_arrival')),
  severity text not null default 'warning' check (severity in ('info','warning','critical')),
  flagged_at timestamptz not null default now(),
  resolved_at timestamptz,
  detail jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_data_quality_flags_user_time
  on public.data_quality_flags (user_id, flagged_at desc);

-- Dashboard daily snapshot (materialized by jobs/edge functions).
create table if not exists public.dashboard_daily_kpis (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  kpi_date date not null,
  training_load numeric(12,2) not null default 0,
  resting_hr_bpm numeric(6,2),
  sleep_duration_minutes integer,
  active_energy_kcal numeric(10,2),
  distance_m numeric(12,2),
  steps integer,
  progression_score numeric(5,2),
  generated_at timestamptz not null default now(),
  unique (user_id, kpi_date)
);

create index if not exists idx_dashboard_daily_kpis_user_date
  on public.dashboard_daily_kpis (user_id, kpi_date desc);

-- Optional direct query view if you do not use snapshots.
create or replace view public.v_dashboard_daily_kpis_live as
select
  es.user_id,
  (es.started_at at time zone 'UTC')::date as kpi_date,
  sum(coalesce(es.total_energy_kcal, 0)) as active_energy_kcal,
  sum(coalesce(es.total_distance_m, 0)) as distance_m,
  sum(coalesce(es.total_steps, 0)) as steps,
  sum((coalesce(es.duration_seconds, 0)::numeric * coalesce(es.avg_heart_rate_bpm, 0)) / 3600.0) as training_load
from public.exercise_sessions es
group by es.user_id, (es.started_at at time zone 'UTC')::date;

-- =========================================================
-- 7) Updated_at trigger
-- =========================================================

create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

do $$
begin
  if not exists (
    select 1 from pg_trigger where tgname = 'trg_profiles_updated_at'
  ) then
    create trigger trg_profiles_updated_at
      before update on public.profiles
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_exercise_sessions_updated_at'
  ) then
    create trigger trg_exercise_sessions_updated_at
      before update on public.exercise_sessions
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_sleep_sessions_updated_at'
  ) then
    create trigger trg_sleep_sessions_updated_at
      before update on public.sleep_sessions
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_user_goals_updated_at'
  ) then
    create trigger trg_user_goals_updated_at
      before update on public.user_goals
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_goal_rules_updated_at'
  ) then
    create trigger trg_goal_rules_updated_at
      before update on public.goal_rules
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_sync_checkpoints_updated_at'
  ) then
    create trigger trg_sync_checkpoints_updated_at
      before update on public.sync_checkpoints
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_sync_queue_updated_at'
  ) then
    create trigger trg_sync_queue_updated_at
      before update on public.sync_queue
      for each row execute function public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'trg_alert_rules_updated_at'
  ) then
    create trigger trg_alert_rules_updated_at
      before update on public.alert_rules
      for each row execute function public.set_updated_at();
  end if;
end $$;

-- =========================================================
-- 8) RLS policies (per-user isolation)
-- =========================================================

alter table public.profiles enable row level security;
alter table public.data_sources enable row level security;
alter table public.exercise_sessions enable row level security;
alter table public.exercise_samples enable row level security;
alter table public.sleep_sessions enable row level security;
alter table public.sleep_stages enable row level security;
alter table public.weight_measurements enable row level security;
alter table public.step_hourly enable row level security;
alter table public.user_goals enable row level security;
alter table public.goal_recommendations enable row level security;
alter table public.consent_events enable row level security;
alter table public.sync_events enable row level security;
alter table public.sync_checkpoints enable row level security;
alter table public.sync_queue enable row level security;
alter table public.alert_rules enable row level security;
alter table public.alert_events enable row level security;
alter table public.data_quality_flags enable row level security;
alter table public.dashboard_daily_kpis enable row level security;

create policy "profiles_own" on public.profiles
  for all using (auth.uid() = id)
  with check (auth.uid() = id);

create policy "data_sources_own" on public.data_sources
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "exercise_sessions_own" on public.exercise_sessions
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "exercise_samples_own" on public.exercise_samples
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "sleep_sessions_own" on public.sleep_sessions
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "sleep_stages_own" on public.sleep_stages
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "weight_measurements_own" on public.weight_measurements
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "step_hourly_own" on public.step_hourly
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "user_goals_own" on public.user_goals
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "goal_recommendations_own" on public.goal_recommendations
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "consent_events_own" on public.consent_events
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "sync_events_own" on public.sync_events
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "sync_checkpoints_own" on public.sync_checkpoints
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "sync_queue_own" on public.sync_queue
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "alert_rules_own" on public.alert_rules
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "alert_events_own" on public.alert_events
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "data_quality_flags_own" on public.data_quality_flags
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "dashboard_daily_kpis_own" on public.dashboard_daily_kpis
  for all using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Optional: grant to authenticated role (adjust to your needs)
grant usage on schema public to authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant usage, select on all sequences in schema public to authenticated;
