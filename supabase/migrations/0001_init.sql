-- SeriesTime initial schema
-- Canonical IDs are TMDB ids. Catalog tables (shows, movies) are a shared cache
-- written by clients on first add/import; per-user state lives in user_* tables.

-- ============ profiles ============
create table public.profiles (
  id uuid primary key references auth.users on delete cascade,
  display_name text,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "profiles are viewable by owner"
  on public.profiles for select using (id = (select auth.uid()));
create policy "profiles are updatable by owner"
  on public.profiles for update using (id = (select auth.uid()));

-- auto-create profile on signup
create function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, new.raw_user_meta_data ->> 'display_name');
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============ shared catalog cache ============
create table public.shows (
  tmdb_id int primary key,
  name text not null,
  original_name text,
  poster_path text,
  first_air_date date,
  status text,
  number_of_seasons int,
  number_of_episodes int,
  tvdb_id int,
  imdb_id text,
  updated_at timestamptz not null default now()
);

create table public.movies (
  tmdb_id int primary key,
  title text not null,
  original_title text,
  poster_path text,
  release_date date,
  runtime int,
  imdb_id text,
  updated_at timestamptz not null default now()
);

alter table public.shows enable row level security;
alter table public.movies enable row level security;

-- trusted friends+family group: any authenticated user may read and upsert
-- the shared catalog. Move writes behind an Edge Function if ever opened up.
create policy "catalog shows readable" on public.shows
  for select to authenticated using (true);
create policy "catalog shows insertable" on public.shows
  for insert to authenticated with check (true);
create policy "catalog shows updatable" on public.shows
  for update to authenticated using (true);

create policy "catalog movies readable" on public.movies
  for select to authenticated using (true);
create policy "catalog movies insertable" on public.movies
  for insert to authenticated with check (true);
create policy "catalog movies updatable" on public.movies
  for update to authenticated using (true);

-- ============ per-user state ============
create type public.show_status as enum ('watchlist', 'watching', 'watched');

create table public.user_shows (
  user_id uuid not null references public.profiles on delete cascade,
  tmdb_id int not null references public.shows,
  status public.show_status not null default 'watchlist',
  is_favorite boolean not null default false,
  added_at timestamptz not null default now(),
  primary key (user_id, tmdb_id)
);

create table public.user_movies (
  user_id uuid not null references public.profiles on delete cascade,
  tmdb_id int not null references public.movies,
  is_watched boolean not null default false,
  is_favorite boolean not null default false,
  rewatch_count int not null default 0,
  watched_at timestamptz,
  added_at timestamptz not null default now(),
  primary key (user_id, tmdb_id)
);

-- one row per WATCHED episode; unwatched episodes have no row
create table public.user_episodes (
  user_id uuid not null references public.profiles on delete cascade,
  show_tmdb_id int not null references public.shows,
  season_number int not null,
  episode_number int not null,
  watched_at timestamptz,
  watch_count int not null default 1,
  primary key (user_id, show_tmdb_id, season_number, episode_number)
);

create index user_episodes_by_show on public.user_episodes (user_id, show_tmdb_id);

alter table public.user_shows enable row level security;
alter table public.user_movies enable row level security;
alter table public.user_episodes enable row level security;

create policy "own user_shows" on public.user_shows
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "own user_movies" on public.user_movies
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "own user_episodes" on public.user_episodes
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
